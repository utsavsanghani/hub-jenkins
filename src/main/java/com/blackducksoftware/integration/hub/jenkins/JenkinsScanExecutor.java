package com.blackducksoftware.integration.hub.jenkins;

import hudson.Launcher;
import hudson.Launcher.ProcStarter;
import hudson.model.BuildListener;
import hudson.model.AbstractBuild;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Arrays;
import java.util.List;

import com.blackducksoftware.integration.hub.ScanExecutor;
import com.blackducksoftware.integration.hub.exception.HubIntegrationException;

public class JenkinsScanExecutor extends ScanExecutor {
    private final AbstractBuild build;

    private final Launcher launcher;

    private final BuildListener listener;

    protected JenkinsScanExecutor(String hubUrl, String hubUsername, String hubPassword, List<String> scanTargets, Integer buildNumber, AbstractBuild build,
            Launcher launcher, BuildListener listener) {
        super(hubUrl, hubUsername, hubPassword, scanTargets, buildNumber);
        this.build = build;
        this.launcher = launcher;
        this.listener = listener;
    }

    protected void setTestScan(Boolean isTest) {
        setIsTest(isTest);
    }

    @Override
    protected Result executeScan(List<String> cmd, File logDirectory) throws HubIntegrationException, InterruptedException {
        try {
            ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
            ProcStarter ps = launcher.launch();
            if (ps != null) {
                // ////////////////////// Code to mask the password in the logs
                int indexOfPassword = cmd.indexOf("--password");
                int indexOfProxyPassword = -1;
                for (int i = 0; i < cmd.size(); i++) {
                    if (cmd.get(i).contains("-Dhttp.proxyPassword")) {
                        indexOfProxyPassword = i;
                        break;
                    }
                }
                boolean[] masks = new boolean[cmd.size()];
                Arrays.fill(masks, false);

                // The Users password should appear after --password
                masks[indexOfPassword + 1] = true;

                if (indexOfProxyPassword != -1) {
                    masks[indexOfProxyPassword] = true;
                }

                ps.masks(masks);
                // ///////////////////////

                ps.envs(build.getEnvironment(listener));
                ps.cmds(cmd);
                ps.stdout(byteStream);
                ps.join();

                ByteArrayOutputStream byteStreamOutput = (ByteArrayOutputStream) ps.stdout();

                byteStreamOutput = (ByteArrayOutputStream) ps.stdout();
                // DO NOT close this PrintStream or Jenkins will not be able to log any more messages. Jenkins will
                // handle
                // closing it.
                String outputString = new String(byteStreamOutput.toByteArray(), "UTF-8");
                getLogger().info(outputString);
                if (logDirectory != null && logDirectory.exists() && doesHubSupportLogOption()) {
                    getLogger().info(
                            "You can view the BlackDuck Scan CLI logs at : '" + logDirectory.getCanonicalPath()
                                    + "'");
                    getLogger().info("");
                }

                if (!outputString.contains("Finished in") || !outputString.contains("with status SUCCESS")) {
                    return Result.FAILURE;
                } else if (outputString.contains("ERROR")) {
                    return Result.FAILURE;
                }

            } else {
                getLogger().error("Could not find a ProcStarter to run the process!");
            }
        } catch (MalformedURLException e) {
            throw new HubIntegrationException("The server URL provided was not a valid", e);
        } catch (IOException e) {
            throw new HubIntegrationException(e.getMessage(), e);
        } catch (InterruptedException e) {
            throw new HubIntegrationException(e.getMessage(), e);
        }
        return Result.SUCCESS;
    }

}
