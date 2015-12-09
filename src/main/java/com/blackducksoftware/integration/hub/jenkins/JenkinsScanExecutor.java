package com.blackducksoftware.integration.hub.jenkins;

import hudson.FilePath;
import hudson.Launcher;
import hudson.Launcher.ProcStarter;
import hudson.model.BuildListener;
import hudson.model.AbstractBuild;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
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
    protected boolean isConfiguredCorrectly(String scanExec, String oneJarPath, String javaExec) {
        if (getLogger() == null) {
            System.out.println("Could not find a logger");
            return false;
        }
        try {

            if (scanExec == null) {
                getLogger().error("Please provide the Hub scan CLI.");
                return false;
            }
            else {
                FilePath scanExecRemote = new FilePath(build.getBuiltOn().getChannel(), scanExec);
                if (!scanExecRemote.exists()) {
                    getLogger().error("The Hub scan CLI provided does not exist.");
                    return false;
                }
            }

            if (oneJarPath == null) {
                getLogger().error("Please provide the path for the CLI cache.");
                return false;
            }

            if (javaExec == null) {
                getLogger().error("Please provide the java home directory.");
                return false;
            }
            else {
                FilePath javaExecRemote = new FilePath(build.getBuiltOn().getChannel(), javaExec);
                if (!javaExecRemote.exists()) {
                    getLogger().error("The Java home provided does not exist.");
                    return false;
                }
            }

            if (getScanMemory() <= 0) {
                getLogger().error("No memory set for the HUB CLI. Will use the default memory, " + DEFAULT_MEMORY);
                setScanMemory(DEFAULT_MEMORY);
            }
        } catch (IOException e) {
            getLogger().error(e.toString(), e);
            return false;
        } catch (InterruptedException e) {
            getLogger().error(e.toString(), e);
            return false;
        }
        return true;
    }

    @Override
    protected String getLogDirectoryPath() throws IOException {
        FilePath logDirectory = new FilePath(build.getBuiltOn().getChannel(), getWorkingDirectory());
        logDirectory = new FilePath(logDirectory, "HubScanLogs");
        logDirectory = new FilePath(logDirectory, String.valueOf(getBuildNumber()));
        // This log directory should never exist as a new one is created for each Build
        try {
            logDirectory.mkdirs();
        } catch (InterruptedException e) {
            getLogger().error("Could not create the log directory : " + e.getMessage(), e);
        }

        return logDirectory.getRemote();
    }

    @Override
    protected Result executeScan(List<String> cmd, String logDirectoryPath) throws HubIntegrationException, InterruptedException {
        try {

            ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
            ProcStarter ps = launcher.launch();
            int exitCode = 0;
            if (ps != null) {
                // ////////////////////// Code to mask the password in the logs
                ArrayList<Integer> indexToMask = new ArrayList<Integer>();
                // The User's password will be at the next index
                indexToMask.add(cmd.indexOf("--password") + 1);

                for (int i = 0; i < cmd.size(); i++) {
                    if (cmd.get(i).contains("-Dhttp") && cmd.get(i).contains("proxyPassword")) {
                        indexToMask.add(i);
                    }
                }
                boolean[] masks = new boolean[cmd.size()];
                Arrays.fill(masks, false);

                for (Integer index : indexToMask) {
                    masks[index] = true;
                }

                ps.masks(masks);
                // ///////////////////////

                ps.envs(build.getEnvironment(listener));
                ps.cmds(cmd);
                ps.stdout(byteStream);
                exitCode = ps.join();

                ByteArrayOutputStream byteStreamOutput = (ByteArrayOutputStream) ps.stdout();

                // DO NOT close this PrintStream or Jenkins will not be able to log any more messages. Jenkins will
                // handle
                // closing it.
                String outputString = new String(byteStreamOutput.toByteArray(), "UTF-8");

                if (outputString.contains("Illegal character in path")
                        && (outputString.contains("Finished in") && outputString.contains("with status FAILURE"))) {
                    // This version of the CLI can not handle spaces in the log directory
                    // Not sure which version of the CLI this issue was fixed

                    int indexOfLogOption = cmd.indexOf("--logDir") + 1;

                    String logPath = cmd.get(indexOfLogOption);
                    logPath = logPath.replace(" ", "%20");
                    cmd.remove(indexOfLogOption);
                    cmd.add(indexOfLogOption, logPath);

                    byteStream = new ByteArrayOutputStream();

                    ps.cmds(cmd);
                    ps.stdout(byteStream);
                    exitCode = ps.join();

                    byteStreamOutput = (ByteArrayOutputStream) ps.stdout();
                    outputString = new String(byteStreamOutput.toByteArray(), "UTF-8");
                } else if (outputString.contains("Illegal character in opaque")
                        && (outputString.contains("Finished in") && outputString.contains("with status FAILURE"))) {
                    // This version of the CLI can not handle spaces in the log directory
                    // Not sure which version of the CLI this issue was fixed

                    int indexOfLogOption = cmd.indexOf("--logDir") + 1;

                    String logPath = cmd.get(indexOfLogOption);

                    File logFile = new File(logPath);

                    logPath = logFile.toURI().toString();
                    cmd.remove(indexOfLogOption);
                    cmd.add(indexOfLogOption, logPath);

                    byteStream = new ByteArrayOutputStream();

                    ps.cmds(cmd);
                    ps.stdout(byteStream);
                    exitCode = ps.join();

                    byteStreamOutput = (ByteArrayOutputStream) ps.stdout();
                    outputString = new String(byteStreamOutput.toByteArray(), "UTF-8");
                }

                getLogger().info(outputString);

                if (logDirectoryPath != null) {
                    FilePath logDirectory = new FilePath(build.getBuiltOn().getChannel(), logDirectoryPath);
                    if (logDirectory.exists() && doesHubSupportLogOption()) {

                        getLogger().info(
                                "You can view the BlackDuck Scan CLI logs at : '" + logDirectory.getRemote()
                                        + "'");
                        getLogger().info("");
                    }
                }

                if (shouldParseStatus()) {
                    if (outputString.contains("Finished in") && outputString.contains("with status SUCCESS")) {
                        return Result.SUCCESS;
                    } else {
                        return Result.FAILURE;
                    }
                } else {
                    if (exitCode == 0) {
                        return Result.SUCCESS;
                    } else {
                        return Result.FAILURE;
                    }
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
