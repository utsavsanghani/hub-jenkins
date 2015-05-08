package com.blackducksoftware.integration.hub.jenkins;

import hudson.FilePath;
import hudson.Launcher;
import hudson.Launcher.ProcStarter;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.model.AbstractBuild;
import hudson.model.JDK;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.blackducksoftware.integration.hub.jenkins.exceptions.BDJenkinsHubPluginException;

public class ScanExecutor {

    private final AbstractBuild build;

    private final Launcher launcher;

    private final BuildListener listener;

    private FilePath scanExec;

    private FilePath oneJarPath;

    private List<FilePath> scanTargets;

    private JDK java;

    private Integer scanMemory;

    private HubServerInfo hubServerInfo;

    private String hubVersion;

    private FilePath workingDirectory;

    private String separator;

    private Boolean isTest;

    protected ScanExecutor(AbstractBuild build, Launcher launcher, BuildListener listener) {
        this.build = build;
        this.launcher = launcher;
        this.listener = listener;
    }

    public String getSeparator() {
        return separator;
    }

    public void setSeparator(String separator) {
        this.separator = separator;
    }

    public FilePath getOneJarPath() {
        return oneJarPath;
    }

    public void setOneJarPath(FilePath oneJarPath) {
        this.oneJarPath = oneJarPath;
    }

    public String getHubVersion() {
        return hubVersion;
    }

    public void setHubVersion(String hubVersion) {
        this.hubVersion = hubVersion;
    }

    public FilePath getWorkingDirectory() {
        return workingDirectory;
    }

    public void setWorkingDirectory(FilePath workingDirectory) {
        this.workingDirectory = workingDirectory;
    }

    public Integer getScanMemory() {
        return scanMemory;
    }

    public void setScanMemory(Integer scanMemory) {
        this.scanMemory = scanMemory;
    }

    public HubServerInfo getHubServerInfo() {
        return hubServerInfo;
    }

    public void setHubServerInfo(HubServerInfo hubServerInfo) {
        this.hubServerInfo = hubServerInfo;
    }

    public FilePath getScanExec() {
        return scanExec;
    }

    public void setScanExec(FilePath scanExec) {
        this.scanExec = scanExec;
    }

    public List<FilePath> getScanTargets() {
        return scanTargets;
    }

    public void setScanTargets(List<FilePath> scanTargets) {
        this.scanTargets = scanTargets;
    }

    public JDK getJava() {
        return java;
    }

    public void setJava(JDK java) {
        this.java = java;
    }

    public Boolean isTest() {
        return isTest;
    }

    public void setIsTest(Boolean isTest) {
        this.isTest = isTest;
    }

    public Result executeScan() throws BDJenkinsHubPluginException {
        try {
            URL url = new URL(getHubServerInfo().getServerUrl());
            List<String> cmd = new ArrayList<String>();
            String javaPath = getJava().getHome();
            if (javaPath.endsWith(separator)) {
                javaPath = javaPath + "bin" + separator + "java";
            } else {
                javaPath = javaPath + separator + "bin" + separator + "java";
            }
            cmd.add(javaPath);
            cmd.add("-Done-jar.silent=true");
            cmd.add("-Done-jar.jar.path=" + getOneJarPath().getRemote());

            // TODO add proxy configuration for the CLI as soon as the CLI has proxy support
            // Jenkins jenkins = Jenkins.getInstance();
            // if (jenkins != null) {
            // ProxyConfiguration proxy = jenkins.proxy;
            // if (proxy != null && proxy.getNoProxyHostPatterns() != null) {
            // if (!JenkinsHubIntRestService.getMatchingNoProxyHostPatterns(url.getHost(),
            // proxy.getNoProxyHostPatterns()))
            // {
            // if (!StringUtils.isEmpty(proxy.name) && proxy.port != 0) {
            // // System.setProperty("http.proxyHost", proxy.name);
            // // System.setProperty("http.proxyPort", Integer.toString(proxy.port));
            // // cmd.add("-Dhttp.useProxy=true");
            // cmd.add("-Dblackduck.hub.proxy.host=" + proxy.name);
            // cmd.add("-Dblackduck.hub.proxy.port=" + proxy.port);
            // System.setProperty("blackduck.hub.proxy.host", proxy.name);
            // System.setProperty("blackduck.hub.proxy.port", Integer.toString(proxy.port));
            // }
            // }
            // }
            // }
            if (scanMemory != 256) {
                cmd.add("-Xmx" + scanMemory + "m");
            } else {
                cmd.add("-Xmx" + PostBuildHubScan.DEFAULT_MEMORY + "m");
            }
            cmd.add("-jar");
            cmd.add(scanExec.getRemote());
            cmd.add("--scheme");
            cmd.add(url.getProtocol());
            cmd.add("--host");
            cmd.add(url.getHost());
            listener.getLogger().println("[DEBUG] : Using this Hub Url : '" + url.getHost() + "'");
            cmd.add("--username");
            cmd.add(getHubServerInfo().getUsername());
            cmd.add("--password");
            cmd.add(getHubServerInfo().getPassword());
            if (url.getPort() != -1) {
                cmd.add("--port");
                cmd.add(Integer.toString(url.getPort()));
            } else {
                if (url.getDefaultPort() != -1) {
                    cmd.add("--port");
                    cmd.add(Integer.toString(url.getDefaultPort()));
                } else {
                    listener.getLogger().println("[WARN] : Could not find a port to use for the Server.");
                }

            }

            if (isTest()) {
                // The new dry run option
                cmd.add("--selfTest");
            }
            FilePath logDirectory = null;
            Boolean oldCLi = false;

            if (hubVersion != null && !hubVersion.equals("2.0.0")) {
                logDirectory = new FilePath(getWorkingDirectory(), "HubScanLogs" + separator + build.getNumber());
                // This log directory should never exist as a new one is created for each Build
                logDirectory.mkdirs();
                // Need to only add this option if version 2.0.1 or later,
                // this is the pro-active approach to the log problem
                cmd.add("--logDir");

                cmd.add(URLEncoder.encode(logDirectory.getRemote(), "UTF-8"));
            }

            for (FilePath target : scanTargets) {
                String targetPath = target.getRemote();
                // targetPath = PostBuildHubScan.correctSeparatorInPath(targetPath, separator);
                cmd.add(targetPath);
            }
            listener.getLogger().println("[DEBUG] : Using this java installation : " + getJava().getName() + " : " +
                    getJava().getHome());
            ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
            ProcStarter ps = launcher.launch();
            if (ps != null) {
                // ////////////////////// Code to mask the password in the logs
                int indexOfPassword = cmd.indexOf("--password");
                boolean[] masks = new boolean[cmd.size()];
                Arrays.fill(masks, false);

                // The Users password should appear after --password
                masks[indexOfPassword + 1] = true;

                ps.masks(masks);
                // ///////////////////////

                ps.envs(build.getEnvironment(listener));
                ps.cmds(cmd);
                ps.stdout(byteStream);
                ps.join();

                ByteArrayOutputStream byteStreamOutput = (ByteArrayOutputStream) ps.stdout();

                if (byteStreamOutput.toString().contains("Unrecognized option: --logDir")) {
                    // retry without the log option
                    // The reactive approach to the log problem
                    cmd.remove("--logDir");
                    cmd.remove(logDirectory.getRemote());
                    oldCLi = true;
                    byteStream = new ByteArrayOutputStream();
                    ps.envs(build.getEnvironment(listener));
                    ps.cmds(cmd);
                    ps.stdout(byteStream);
                    ps.join();
                }
                byteStreamOutput = (ByteArrayOutputStream) ps.stdout();
                // DO NOT close this PrintStream or Jenkins will not be able to log any more messages. Jenkins will
                // handle
                // closing it.
                String outputString = new String(byteStreamOutput.toByteArray(), "UTF-8");
                listener.getLogger().println(outputString);
                if (!outputString.contains("Finished in") || !outputString.contains("with status SUCCESS")) {
                    return Result.UNSTABLE;
                } else if (outputString.contains("ERROR")) {
                    return Result.UNSTABLE;
                } else {
                    if (logDirectory != null && logDirectory.exists() && !oldCLi) {
                        listener.getLogger().println(
                                "You can view the BlackDuck Scan CLI logs at : '" + logDirectory.getRemote()
                                        + "'");
                        listener.getLogger().println();
                    }
                }
            } else {
                listener.getLogger().println("[ERROR] : Could not find a ProcStarter to run the process!");
            }
        } catch (MalformedURLException e) {
            throw new BDJenkinsHubPluginException("The server URL provided was not a valid", e);
        } catch (IOException e) {
            throw new BDJenkinsHubPluginException(e.getMessage(), e);
        } catch (InterruptedException e) {
            throw new BDJenkinsHubPluginException(e.getMessage(), e);
        }
        return Result.SUCCESS;
    }
}
