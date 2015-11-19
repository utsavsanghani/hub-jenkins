Hub plugin for Jenkins. This plugin provides ability to run a BD Hub CLi on build input or output. It offers to run multiple code locations.

## Release Notes:
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
