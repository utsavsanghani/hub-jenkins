package com.blackducksoftware.integration.hub.jenkins.site;

import hudson.BulkChange;
import hudson.Extension;
import hudson.Plugin;
import hudson.PluginWrapper;
import hudson.init.InitMilestone;
import hudson.init.Initializer;
import hudson.lifecycle.RestartNotSupportedException;
import hudson.model.Hudson;
import hudson.model.UpdateCenter;
import hudson.model.UpdateSite;
import hudson.security.ACL;
import hudson.triggers.SafeTimerTask;
import hudson.triggers.Trigger;
import hudson.util.PersistedList;
import hudson.util.TimeUnit2;
import hudson.util.VersionNumber;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import jenkins.model.Jenkins;

import org.acegisecurity.context.SecurityContext;
import org.acegisecurity.context.SecurityContextHolder;
import org.jvnet.localizer.Localizable;

import com.blackducksoftware.integration.hub.jenkins.Messages;

/**
 * Majority of the code was copied from http://github.com/jenkinsci/cloudbees-plugin-gateway
 *
 */
public class HubPluginImpl extends Plugin {

    /**
     * Our logger.
     */
    private static final Logger LOGGER = Logger.getLogger(HubPluginImpl.class.getName());

    // FIXME must change this URL to the correct update center json file
    /**
     * The current update center URL.
     */
    private static final String HUB_UPDATE_CENTER_URL =
            "http://eng-jenkins-update.blackducksoftware.com:8080/userContent/ReleaseUpdateCenter/update-center.json";

    /**
     * The current update center URL and any previous URLs that were used for the same content and should be migrated
     * to the current one.
     */
    private static final Set<String> hubUpdateCenterUrls = new HashSet<String>(Arrays.asList(
            HUB_UPDATE_CENTER_URL
            ));

    /**
     * The current update center ID.
     */
    private static final String HUB_UPDATE_CENTER_ID = "blackDuck-hub-proprietary";

    /**
     * The Jenkins default update site ID.
     */
    private static final String JENKINS_UPDATE_CENTER_ID = "default";

    /**
     * The current update center ID and any previous IDs that were used for the same content and should be migrated
     * to the current one.
     */
    private static final Set<String> hubUpdateCenterIds = new HashSet<String>(Arrays.asList(
            HUB_UPDATE_CENTER_ID
            ));

    /**
     * The plugins that can and/or should be installed/upgraded.
     */
    private static final Dependency[] PLUGIN_DEPENDENCIES = {
            require("credentials", "1.9.4"),
    };

    /**
     * The list of plugin installations that remains to be completed.
     * <p/>
     * Guarded by {@link #pendingPluginInstalls}.
     */
    private static final List<Dependency> pendingPluginInstalls = new ArrayList<Dependency>();

    /**
     * Guarded by {@link #pendingPluginInstalls}.
     */
    private static DelayedInstaller worker = null;

    /**
     * The current status.
     */
    private static volatile Localizable status = null;

    /**
     * Whether the current status is important.
     */
    private static volatile boolean statusImportant = false;

    /**
     * The most recently installed version of this plugin, used to trigger whether to re-evaluate installing/upgrading
     * the {@link #PLUGIN_DEPENDENCIES}.
     */
    private String installedVersion = null;

    public HubPluginImpl() {
    }

    @Override
    public void start() throws Exception {
        LOGGER.log(Level.INFO, "Started...");
        try {
            load();
        } catch (Throwable e) {
            LOGGER.log(Level.WARNING, "Could not deserialize state, assuming the plugins need re-installation", e);
            installedVersion = null;
        }
    }

    public boolean isInstalled() {
        if (installedVersion == null) {
            return false;
        }
        try {
            PluginWrapper pluginWrapper = Hudson.getInstance().getPluginManager().getPlugin(getClass());
            String targetVersion = getVersionString(pluginWrapper);
            LOGGER.log(Level.INFO, "Installed version = {0}. Target version = {1}",
                    new Object[] { installedVersion, targetVersion });
            return !new VersionNumber(installedVersion).isOlderThan(new VersionNumber(targetVersion));
        } catch (Throwable t) {
            // if in doubt, it's not installed
            return false;
        }
    }

    public void setInstalled(boolean installed) {
        boolean changed = false;
        if (installed) {
            PluginWrapper pluginWrapper = Hudson.getInstance().getPluginManager().getPlugin(getClass());
            String version = getVersionString(pluginWrapper);
            if (!version.equals(installedVersion)) {
                installedVersion = version;
                changed = true;
            }
        } else {
            if (installedVersion != null) {
                installedVersion = null;
                changed = true;
            }
        }
        if (changed) {
            try {
                save();
            } catch (IOException e) {
                LOGGER.log(Level.WARNING,
                        "Could not serialize state. If any of the free plugins are uninstalled, "
                                + "they may be reinstalled on next restart.",
                        e);
            }
        }
    }

    private String getVersionString(PluginWrapper pluginWrapper) {
        String version = pluginWrapper.getVersionNumber().toString();
        int i = version.indexOf(' ');
        version = i == -1 ? version : version.substring(0, i);
        return version;
    }

    public static Localizable getStatus() {
        return status;
    }

    public static boolean isStatusImportant() {
        return statusImportant;
    }

    @Initializer(after = InitMilestone.EXTENSIONS_AUGMENTED, attains = "blackduck-hub-update-center-configured")
    public static void addUpdateCenter() throws Exception {
        LOGGER.log(Level.INFO, "Checking that the BlackDuck update center has been configured.");
        UpdateCenter updateCenter = Hudson.getInstance().getUpdateCenter();
        synchronized (updateCenter) {
            PersistedList<UpdateSite> sites = updateCenter.getSites();
            if (sites.isEmpty()) {
                // likely the list has not been loaded yet
                updateCenter.load();
                sites = updateCenter.getSites();
            }

            boolean found = false;
            List<UpdateSite> forRemoval = new ArrayList<UpdateSite>();
            for (UpdateSite site : sites) {
                LOGGER.log(Level.FINE, "Update site {0} class {1} url {2}",
                        new Object[] { site.getId(), site.getClass(), site.getUrl() });
                if (hubUpdateCenterUrls.contains(site.getUrl()) || hubUpdateCenterIds.contains(site.getId())
                        || site instanceof BlackDuckHubUpdateSite) {
                    LOGGER.log(Level.FINE, "Found possible match:\n  class = {0}\n  url = {1}\n  id = {2}",
                            new Object[] { site.getClass().getName(), site.getUrl(), site.getId() });
                    boolean valid = site instanceof BlackDuckHubUpdateSite
                            && HUB_UPDATE_CENTER_URL.equals(site.getUrl())
                            && HUB_UPDATE_CENTER_ID.equals(site.getId());
                    if (found || !valid) {
                        // remove old and duplicate entries
                        forRemoval.add(site);
                    }
                    found = found || valid;
                }
            }

            // now make the changes if we have any to make
            LOGGER.log(Level.FINE, "Found={0}\nRemoving={1}", new Object[] { found, forRemoval });
            if (!found || !forRemoval.isEmpty()) {
                BulkChange bc = new BulkChange(updateCenter);
                try {
                    for (UpdateSite site : forRemoval) {
                        LOGGER.info("Removing legacy BlackDuck Update Center from list of update centers");
                        sites.remove(site);
                    }
                    if (sites.isEmpty()) {
                        LOGGER.info("Adding Default Update Center to list of update centers as it was missing");
                        sites.add(new UpdateSite("default",
                                System.getProperty(UpdateCenter.class.getName() + ".updateCenterUrl",
                                        "http://updates.jenkins-ci.org/")
                                        + "update-center.json"));
                    }
                    if (!found) {
                        LOGGER.info("Adding BlackDuck Update Center to list of update centers");
                        sites.add(new BlackDuckHubUpdateSite(HUB_UPDATE_CENTER_ID, HUB_UPDATE_CENTER_URL));
                    }
                } finally {
                    bc.commit();
                }
            }
        }
    }

    @Initializer(requires = "blackduck-hub-update-center-configured")
    public static void installCorePlugins() {
        LOGGER.log(Level.INFO, "Checking that the Black Duck Hub dependencies have been installed.");
        HubPluginImpl hubPluginImpl = Hudson.getInstance().getPlugin(HubPluginImpl.class);
        if (hubPluginImpl != null && hubPluginImpl.isInstalled()) {
            for (Dependency pluginArtifactId : PLUGIN_DEPENDENCIES) {
                if (pluginArtifactId.mandatory) {
                    LOGGER.log(Level.INFO, "Checking {0}.", pluginArtifactId.name);
                    PluginWrapper plugin = Hudson.getInstance().getPluginManager().getPlugin(pluginArtifactId.name);
                    // scheduleInstall(pluginArtifactId);
                    if (plugin == null) {
                        scheduleInstall(pluginArtifactId);
                        LOGGER.log(Level.INFO, "Dependency {0} will be installed.", pluginArtifactId.name);
                    } else {
                        if (!plugin.isEnabled()) {
                            LOGGER.log(Level.INFO, "Enabling {0}", pluginArtifactId.name);
                            try {
                                plugin.enable();
                            } catch (IOException e) {
                                LOGGER.log(Level.WARNING, "Could not enable " + pluginArtifactId.name, e);
                            }
                        } else {
                            LOGGER.log(Level.INFO, "Dependency {0} already installed and enabled.", pluginArtifactId.name);
                        }
                    }
                }
            }
            installMissingDependencies(hubPluginImpl);
            LOGGER.info("Black Duck Hub installation previously completed, will not check or reinstall");
            return;
        }
        for (Dependency pluginArtifactId : PLUGIN_DEPENDENCIES) {
            LOGGER.log(Level.INFO, "Checking {0}.", pluginArtifactId.name);
            PluginWrapper plugin = Hudson.getInstance().getPluginManager().getPlugin(pluginArtifactId.name);
            if (plugin == null && !pluginArtifactId.optional) {
                // not installed and mandatory
                scheduleInstall(pluginArtifactId);
            } else if (plugin != null && (pluginArtifactId.version != null || plugin.getVersion() == null)) {
                // already installed
                if (plugin.getVersionNumber().compareTo(pluginArtifactId.version) < 0) {
                    // but older version
                    scheduleInstall(pluginArtifactId);
                }
            }
            if (pluginArtifactId.mandatory) {
                if (plugin != null && !plugin.isEnabled()) {
                    LOGGER.log(Level.INFO, "Enabling {0}", pluginArtifactId.name);
                    try {
                        plugin.enable();
                    } catch (IOException e) {
                        LOGGER.log(Level.WARNING, "Could not enable " + pluginArtifactId.name, e);
                    }
                }
            }
        }
        installMissingDependencies(hubPluginImpl);
    }

    private static void installMissingDependencies(HubPluginImpl hubPluginImpl) {
        boolean finished;
        synchronized (pendingPluginInstalls) {
            finished = pendingPluginInstalls.isEmpty();
            if (!finished && (worker == null || !worker.isAlive())) {
                status = Messages._HubPluginImpl_downloadUCMetadata();
                LOGGER.info("Starting background thread for core plugin installation");
                worker = new DelayedInstaller();
                worker.setDaemon(true);
                worker.start();
            } else {
                LOGGER.log(Level.INFO, "Nothing to do");
            }
        }
        if (finished && hubPluginImpl != null) {
            hubPluginImpl.setInstalled(true);
        }
    }

    private static void scheduleInstall(Dependency pluginArtifactId) {
        synchronized (pendingPluginInstalls) {
            LOGGER.log(Level.INFO, "Scheduling installation of {0}", pluginArtifactId.name);
            pendingPluginInstalls.add(pluginArtifactId);
        }
    }

    @Extension
    public static class DelayedInstaller extends Thread {

        private long nextWarning;

        @Override
        public void run() {
            nextWarning = 0;
            try {
                boolean loop = true;
                while (loop) {
                    LOGGER.fine("Background thread for core plugin installation awake");
                    try {
                        UpdateSite blackDuckHubSite =
                                Hudson.getInstance().getUpdateCenter().getSite(JENKINS_UPDATE_CENTER_ID);
                        if (blackDuckHubSite.getDataTimestamp() > -1) {
                            loop = progressPluginInstalls(blackDuckHubSite);
                        } else {
                            status = Messages._HubPluginImpl_downloadUCMetadata();
                        }

                        Thread.sleep(5000);
                    } catch (InterruptedException e) {
                        // ignore
                    } catch (Throwable t) {
                        try {
                            Thread.sleep(5000);
                        } catch (InterruptedException e) {
                            // ignore
                        }
                    }
                }
                if (!loop) {
                    statusImportant = true;
                    try {
                        status = Messages._HubPluginImpl_scheduledRestart();
                        try {
                            Thread.sleep(5000);
                        } catch (InterruptedException e) {
                            // ignore
                        }
                        Hudson.getInstance().safeRestart();
                        // if the user manually cancelled the quiet down, reflect that in the status message
                        Trigger.timer.scheduleAtFixedRate(new SafeTimerTask() {
                            @Override
                            protected void doRun() throws Exception {
                                if (!Jenkins.getInstance().isQuietingDown()) {
                                    status = null;
                                }
                            }
                        }, 1000, 1000);
                    } catch (RestartNotSupportedException exception) {
                        // ignore if restart is not allowed
                        status = Messages._HubPluginImpl_restartRequired();
                    }
                }
            } finally {
                LOGGER.info("Background thread for core plugin installation finished.");
                boolean finished;
                synchronized (pendingPluginInstalls) {
                    if (worker == this) {
                        worker = null;
                    }
                    finished = pendingPluginInstalls.isEmpty();
                }
                HubPluginImpl instance = Hudson.getInstance().getPlugin(HubPluginImpl.class);
                if (finished && instance != null) {
                    instance.setInstalled(true);
                }
            }
        }

        private boolean progressPluginInstalls(UpdateSite blackDuckHubSite) {
            synchronized (pendingPluginInstalls) {
                while (!pendingPluginInstalls.isEmpty()) {
                    Dependency pluginArtifactId = pendingPluginInstalls.get(0);
                    UpdateSite.Plugin p = Hudson.getInstance()
                            .getUpdateCenter()
                            .getSite(JENKINS_UPDATE_CENTER_ID)
                            .getPlugin(pluginArtifactId.name);
                    if (p == null) {
                        if (System.currentTimeMillis() > nextWarning) {
                            LOGGER.log(Level.WARNING,
                                    "Cannot find core plugin {0}, the BlackDuck Hub plugin cannot be "
                                            + "installed without this core plugin. Will try again later.",
                                    pluginArtifactId.name);
                            nextWarning = System.currentTimeMillis() + TimeUnit2.HOURS.toMillis(1);
                        }
                        break;
                    } else if (p.getInstalled() != null && p.getInstalled().isEnabled()) {
                        PluginWrapper plugin = Hudson.getInstance().getPluginManager().getPlugin(pluginArtifactId.name);
                        if (plugin != null && plugin.getVersionNumber().compareTo(pluginArtifactId.version) < 0) {
                            LOGGER.info("Upgrading BlackDuck plugin: " + pluginArtifactId.name);
                            status = Messages._HubPluginImpl_upgradingPlugin(p.getDisplayName(), p.version);
                            SecurityContext old = ACL.impersonate(ACL.SYSTEM);
                            try {
                                p.deploy().get();
                                LOGGER.info("Upgraded BlackDuck plugin: " + pluginArtifactId.name + " to " + p.version);
                                pendingPluginInstalls.remove(0);
                                nextWarning = 0;
                                status = Messages._HubPluginImpl_upgradedPlugin(p.getDisplayName(), p.version);
                            } catch (Throwable e) {
                                if (System.currentTimeMillis() > nextWarning) {
                                    LOGGER.log(Level.WARNING,
                                            "Cannot upgrade BlackDuck plugin: " + pluginArtifactId.name + " to "
                                                    + p.version, e);
                                    nextWarning = System.currentTimeMillis() + TimeUnit2.MINUTES.toMillis(1);
                                }
                                break;
                            } finally {
                                SecurityContextHolder.setContext(old);
                            }
                        } else {
                            LOGGER.info("Detected previous installation of BlackDuck plugin: " + pluginArtifactId.name);
                            pendingPluginInstalls.remove(0);
                            nextWarning = 0;
                        }
                    } else {
                        LOGGER.info("Installing BlackDuck plugin: " + pluginArtifactId.name + " version " + p.version);
                        status = Messages._HubPluginImpl_installingPlugin(p.getDisplayName());
                        SecurityContext old = ACL.impersonate(ACL.SYSTEM);
                        try {
                            p.deploy().get();
                            LOGGER.info(
                                    "Installed BlackDuck plugin: " + pluginArtifactId.name + " version " + p.version);
                            pendingPluginInstalls.remove(0);
                            nextWarning = 0;
                            status = Messages._HubPluginImpl_installedPlugin(p.getDisplayName());
                        } catch (Throwable e) {
                            if (System.currentTimeMillis() > nextWarning) {
                                LOGGER.log(Level.WARNING,
                                        "Cannot install BlackDuck plugin: " + pluginArtifactId.name + " version "
                                                + p.version, e);
                                nextWarning = System.currentTimeMillis() + TimeUnit2.MINUTES.toMillis(1);
                            }
                            break;
                        } finally {
                            SecurityContextHolder.setContext(old);
                        }
                    }
                }
                return !pendingPluginInstalls.isEmpty();
            }
        }
    }

    static {
        UpdateCenter.XSTREAM.alias("blackDuck-hub-proprietary", BlackDuckHubUpdateSite.class);
    }

    private static Dependency require(String name) {
        return require(name, null);
    }

    private static Dependency require(String name, String version) {
        return new Dependency(name, version, false, true);
    }

    private static Dependency optional(String name) {
        return optional(name, null);
    }

    private static Dependency optional(String name, String version) {
        return new Dependency(name, version, true, false);
    }

    private static class Dependency {
        public final String name;

        public final VersionNumber version;

        public final boolean optional;

        public final boolean mandatory;

        private Dependency(String name, String version, boolean optional, boolean mandatory) {
            this.name = name;
            this.version = version == null ? null : new VersionNumber(version);
            this.optional = optional;
            this.mandatory = mandatory;
        }

    }

}
