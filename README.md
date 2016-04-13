# Jenkins Hub Plugin

Hub plugin for Jenkins. This plugin provides ability to run a BD Hub CLi on build input or output. It offers to run multiple code locations.

## Build

[![Build Status](https://travis-ci.org/blackducksoftware/jenkins-hub.svg?branch=master)](https://travis-ci.org/blackducksoftware/jenkins-hub)

## Overview


# Documentation
1.5.0 - TBD

1.4.1 - https://github.com/blackducksoftware/integration-all/blob/master/Hub_Jenkins_Plugin_v1.4.1.pdf

## Release Notes:
### 1.5.0
* Initial Open Source release
* Updating to use the hub-common changes for Public Api's

### 1.4.1
* Removed empty "BlackDuck Scan" configuration entirely from the global configuration
* Using new CLI option to better check when the BOM has been updated with the scan results (Hub 3.0.0+ only)
* Black Duck Failure Conditions are fixed; they are now added after the Black Duck Hub integration.
* Failure Conditions now work even if the project name and version contain variables.
* Failure Conditions now wait until the Bill of Materials is updated before checking the policy status.
* The field Maximum time to wait for report (in minutes) is changed to Maximum time to wait for BOM update (in minutes).

### 1.4.0
* Auto install of the BD Scanner (CLI)
* Ability to pull a BOM report from Hub into Jenkins
* Fail the Jenkins build, if any component does not pass Hub policy (Hub 3.0.0+ only)
* Network timeout for Hub connection configurable
* Update of plugin via Jenkins update site
** Independent of Hub releases
** Signaled in the Jenkins plugin management

### 1.3.7
* Fix issue where builds run on slaves log less messages to the console log, than when run on master

### 1.3.6
* Improves compatibility with non Oracle JREs
* Improves determination of the local hostname 

### 1.3.5
* Fix an issue with Code locations being mapped to multiple projects (IJH-83)
* Fix an issue with scanning >10 code locations in a single job (IJH-93)
* Fix an issue where the build.JDK is set to < 1.7 (IJH-97)
* Factored out common CI code for hub integrations.

### 1.3.4
* Invalid URL succeeds in "Test Connection", but fails in the Job run
* Make log directory option work with Hub 2.1.5, 2.2 and going forward.

### 1.3.3
* empty version never delivered

### 1.3.2
* Requires Java 1.7+
* Fixed an issue where the command line interface login failed on Windows operating systems.

### 1.3.1
* Fixed an issue where if, on a rescan, the generation of the log file failed, the plugin would show the previously generated log file.
* Fixed an issue where the incorrect port number was being passed to the Hub command line interface (CLI) when the Jenkins port value was null (no port number entered). 

### 1.3.0
* Initial release
