// CHECKSTYLE:OFF

package com.blackducksoftware.integration.hub.jenkins;

import org.jvnet.localizer.Localizable;
import org.jvnet.localizer.ResourceBundleHolder;

@SuppressWarnings({
    "",
    "PMD"
})
public class Messages {

    private final static ResourceBundleHolder holder = ResourceBundleHolder.get(Messages.class);

    /**
     * This Project does not exist on the Hub Server : {0}, BUT these matches were found : {1}
     * 
     */
    public static String HubBuildScan_getProjectNonExistingWithMatches_0_(Object arg1, Object arg2) {
        return holder.format("HubBuildScan_getProjectNonExistingWithMatches_0_", arg1, arg2);
    }

    /**
     * This Project does not exist on the Hub Server : {0}, BUT these matches were found : {1}
     * 
     */
    public static Localizable _HubBuildScan_getProjectNonExistingWithMatches_0_(Object arg1, Object arg2) {
        return new Localizable(holder, "HubBuildScan_getProjectNonExistingWithMatches_0_", arg1, arg2);
    }

    /**
     * Can not reach this server : {0}
     * 
     */
    public static String HubBuildScan_getCanNotReachThisServer_0_(Object arg1) {
        return holder.format("HubBuildScan_getCanNotReachThisServer_0_", arg1);
    }

    /**
     * Can not reach this server : {0}
     * 
     */
    public static Localizable _HubBuildScan_getCanNotReachThisServer_0_(Object arg1) {
        return new Localizable(holder, "HubBuildScan_getCanNotReachThisServer_0_", arg1);
    }

    /**
     * Could not find the Project with the Name : {0}
     * 
     */
    public static String HubBuildScan_getCouldNotFindProject_0_(Object arg1) {
        return holder.format("HubBuildScan_getCouldNotFindProject_0_", arg1);
    }

    /**
     * Could not find the Project with the Name : {0}
     * 
     */
    public static Localizable _HubBuildScan_getCouldNotFindProject_0_(Object arg1) {
        return new Localizable(holder, "HubBuildScan_getCouldNotFindProject_0_", arg1);
    }

    /**
     * This Project exists on the Hub Server : {0}
     * 
     */
    public static String HubBuildScan_getProjectExistsIn_0_(Object arg1) {
        return holder.format("HubBuildScan_getProjectExistsIn_0_", arg1);
    }

    /**
     * This Project exists on the Hub Server : {0}
     * 
     */
    public static Localizable _HubBuildScan_getProjectExistsIn_0_(Object arg1) {
        return new Localizable(holder, "HubBuildScan_getProjectExistsIn_0_", arg1);
    }

    /**
     * This Project does not exist on the Hub Server : {0}
     * 
     */
    public static String HubBuildScan_getProjectNonExistingIn_0_(Object arg1) {
        return holder.format("HubBuildScan_getProjectNonExistingIn_0_", arg1);
    }

    /**
     * This Project does not exist on the Hub Server : {0}
     * 
     */
    public static Localizable _HubBuildScan_getProjectNonExistingIn_0_(Object arg1) {
        return new Localizable(holder, "HubBuildScan_getProjectNonExistingIn_0_", arg1);
    }

    /**
     * The Project and Version specified already exist!
     * 
     */
    public static String HubBuildScan_getProjectAndVersionExist() {
        return holder.format("HubBuildScan_getProjectAndVersionExist");
    }

    /**
     * The Project and Version specified already exist!
     * 
     */
    public static Localizable _HubBuildScan_getProjectAndVersionExist() {
        return new Localizable(holder, "HubBuildScan_getProjectAndVersionExist");
    }

    /**
     * Credentials are not valid for: {0}
     * 
     */
    public static String HubBuildScan_getCredentialsInValidFor_0_(Object arg1) {
        return holder.format("HubBuildScan_getCredentialsInValidFor_0_", arg1);
    }

    /**
     * Credentials are not valid for: {0}
     * 
     */
    public static Localizable _HubBuildScan_getCredentialsInValidFor_0_(Object arg1) {
        return new Localizable(holder, "HubBuildScan_getCredentialsInValidFor_0_", arg1);
    }

    /**
     * User needs to specify which credentials to use.
     * 
     */
    public static String HubBuildScan_getCredentialsNotFound() {
        return holder.format("HubBuildScan_getCredentialsNotFound");
    }

    /**
     * User needs to specify which credentials to use.
     * 
     */
    public static Localizable _HubBuildScan_getCredentialsNotFound() {
        return new Localizable(holder, "HubBuildScan_getCredentialsNotFound");
    }

    /**
     * Not a valid URL
     * 
     */
    public static String HubBuildScan_getNotAValidUrl() {
        return holder.format("HubBuildScan_getNotAValidUrl");
    }

    /**
     * Not a valid URL
     * 
     */
    public static Localizable _HubBuildScan_getNotAValidUrl() {
        return new Localizable(holder, "HubBuildScan_getNotAValidUrl");
    }

    /**
     * Either this Project or Version already exist.
     * 
     */
    public static String HubBuildScan_getProjectVersionCreationProblem() {
        return holder.format("HubBuildScan_getProjectVersionCreationProblem");
    }

    /**
     * Either this Project or Version already exist.
     * 
     */
    public static Localizable _HubBuildScan_getProjectVersionCreationProblem() {
        return new Localizable(holder, "HubBuildScan_getProjectVersionCreationProblem");
    }

    /**
     * Please set a Server URL
     * 
     */
    public static String HubBuildScan_getPleaseSetServerUrl() {
        return holder.format("HubBuildScan_getPleaseSetServerUrl");
    }

    /**
     * Please set a Server URL
     * 
     */
    public static Localizable _HubBuildScan_getPleaseSetServerUrl() {
        return new Localizable(holder, "HubBuildScan_getPleaseSetServerUrl");
    }

    /**
     * Need to provide a Project name.
     * 
     */
    public static String HubBuildScan_getProvideProjectName() {
        return holder.format("HubBuildScan_getProvideProjectName");
    }

    /**
     * Need to provide a Project name.
     * 
     */
    public static Localizable _HubBuildScan_getProvideProjectName() {
        return new Localizable(holder, "HubBuildScan_getProvideProjectName");
    }

    /**
     * This Project does not exist or there is a problem connecting to the Hub server
     * 
     */
    public static String HubBuildScan_getProjectNonExistingOrTroubleConnecting_() {
        return holder.format("HubBuildScan_getProjectNonExistingOrTroubleConnecting_");
    }

    /**
     * This Project does not exist or there is a problem connecting to the Hub server
     * 
     */
    public static Localizable _HubBuildScan_getProjectNonExistingOrTroubleConnecting_() {
        return new Localizable(holder, "HubBuildScan_getProjectNonExistingOrTroubleConnecting_");
    }

    /**
     * The Project Name contains a variable. The Name will be resolved and handled during the build.
     * 
     */
    public static String HubBuildScan_getProjectNameContainsVariable() {
        return holder.format("HubBuildScan_getProjectNameContainsVariable");
    }

    /**
     * The Project Name contains a variable. The Name will be resolved and handled during the build.
     * 
     */
    public static Localizable _HubBuildScan_getProjectNameContainsVariable() {
        return new Localizable(holder, "HubBuildScan_getProjectNameContainsVariable");
    }

    /**
     * The Project Version contains a variable. The Version will be resolved and handled during the build.
     * 
     */
    public static String HubBuildScan_getProjectVersionContainsVariable() {
        return holder.format("HubBuildScan_getProjectVersionContainsVariable");
    }

    /**
     * The Project Version contains a variable. The Version will be resolved and handled during the build.
     * 
     */
    public static Localizable _HubBuildScan_getProjectVersionContainsVariable() {
        return new Localizable(holder, "HubBuildScan_getProjectVersionContainsVariable");
    }

    /**
     * The Version was created!
     * 
     */
    public static String HubBuildScan_getVersionCreated() {
        return holder.format("HubBuildScan_getVersionCreated");
    }

    /**
     * The Version was created!
     * 
     */
    public static Localizable _HubBuildScan_getVersionCreated() {
        return new Localizable(holder, "HubBuildScan_getVersionCreated");
    }

    /**
     * Could not connect to the Hub server with the Given Url and credentials. Error Code:  {0}
     * 
     */
    public static String HubBuildScan_getErrorConnectingTo_0_(Object arg1) {
        return holder.format("HubBuildScan_getErrorConnectingTo_0_", arg1);
    }

    /**
     * Could not connect to the Hub server with the Given Url and credentials. Error Code:  {0}
     * 
     */
    public static Localizable _HubBuildScan_getErrorConnectingTo_0_(Object arg1) {
        return new Localizable(holder, "HubBuildScan_getErrorConnectingTo_0_", arg1);
    }

    /**
     * This Version exists in the Project with Id : {0}
     * 
     */
    public static String HubBuildScan_getVersionExistsIn_0_(Object arg1) {
        return holder.format("HubBuildScan_getVersionExistsIn_0_", arg1);
    }

    /**
     * This Version exists in the Project with Id : {0}
     * 
     */
    public static Localizable _HubBuildScan_getVersionExistsIn_0_(Object arg1) {
        return new Localizable(holder, "HubBuildScan_getVersionExistsIn_0_", arg1);
    }

    /**
     * The Project Name or Version contains a variable. They will be resolved and handled during the build.
     * 
     */
    public static String HubBuildScan_getProjectNameOrVersionContainsVariable() {
        return holder.format("HubBuildScan_getProjectNameOrVersionContainsVariable");
    }

    /**
     * The Project Name or Version contains a variable. They will be resolved and handled during the build.
     * 
     */
    public static Localizable _HubBuildScan_getProjectNameOrVersionContainsVariable() {
        return new Localizable(holder, "HubBuildScan_getProjectNameOrVersionContainsVariable");
    }

    /**
     * The memory provided is not a valid amount
     * 
     */
    public static String HubBuildScan_getInvalidMemoryString() {
        return holder.format("HubBuildScan_getInvalidMemoryString");
    }

    /**
     * The memory provided is not a valid amount
     * 
     */
    public static Localizable _HubBuildScan_getInvalidMemoryString() {
        return new Localizable(holder, "HubBuildScan_getInvalidMemoryString");
    }

    /**
     * Expected a different JSON response from the server, the Hub API's may have changed, Or the response was mapped incorrectly.
     * 
     */
    public static String HubBuildScan_getIncorrectMappingOfServerResponse() {
        return holder.format("HubBuildScan_getIncorrectMappingOfServerResponse");
    }

    /**
     * Expected a different JSON response from the server, the Hub API's may have changed, Or the response was mapped incorrectly.
     * 
     */
    public static Localizable _HubBuildScan_getIncorrectMappingOfServerResponse() {
        return new Localizable(holder, "HubBuildScan_getIncorrectMappingOfServerResponse");
    }

    /**
     * Credentials valid for: {0}
     * 
     */
    public static String HubBuildScan_getCredentialsValidFor_0_(Object arg1) {
        return holder.format("HubBuildScan_getCredentialsValidFor_0_", arg1);
    }

    /**
     * Credentials valid for: {0}
     * 
     */
    public static Localizable _HubBuildScan_getCredentialsValidFor_0_(Object arg1) {
        return new Localizable(holder, "HubBuildScan_getCredentialsValidFor_0_", arg1);
    }

    /**
     * The Project and Version have been created!
     * 
     */
    public static String HubBuildScan_getProjectAndVersionCreated() {
        return holder.format("HubBuildScan_getProjectAndVersionCreated");
    }

    /**
     * The Project and Version have been created!
     * 
     */
    public static Localizable _HubBuildScan_getProjectAndVersionCreated() {
        return new Localizable(holder, "HubBuildScan_getProjectAndVersionCreated");
    }

    /**
     * This Project exists on the Hub Server : {0}, BUT there are multiple projects with the same name.
     * 
     */
    public static String HubBuildScan_getProjectExistsWithDuplicateMatches_0_(Object arg1) {
        return holder.format("HubBuildScan_getProjectExistsWithDuplicateMatches_0_", arg1);
    }

    /**
     * This Project exists on the Hub Server : {0}, BUT there are multiple projects with the same name.
     * 
     */
    public static Localizable _HubBuildScan_getProjectExistsWithDuplicateMatches_0_(Object arg1) {
        return new Localizable(holder, "HubBuildScan_getProjectExistsWithDuplicateMatches_0_", arg1);
    }

    /**
     * The Project was created!
     * 
     */
    public static String HubBuildScan_getProjectCreated() {
        return holder.format("HubBuildScan_getProjectCreated");
    }

    /**
     * The Project was created!
     * 
     */
    public static Localizable _HubBuildScan_getProjectCreated() {
        return new Localizable(holder, "HubBuildScan_getProjectCreated");
    }

    /**
     * The scan target is empty, the entire workspace will be scanned
     * 
     */
    public static String HubBuildScan_getWorkspaceWillBeScanned() {
        return holder.format("HubBuildScan_getWorkspaceWillBeScanned");
    }

    /**
     * The scan target is empty, the entire workspace will be scanned
     * 
     */
    public static Localizable _HubBuildScan_getWorkspaceWillBeScanned() {
        return new Localizable(holder, "HubBuildScan_getWorkspaceWillBeScanned");
    }

    /**
     * This Version does not exist in the Project with Id : {0}, BUT these versions were found : {1}
     * 
     */
    public static String HubBuildScan_getVersionNonExistingIn_0_(Object arg1, Object arg2) {
        return holder.format("HubBuildScan_getVersionNonExistingIn_0_", arg1, arg2);
    }

    /**
     * This Version does not exist in the Project with Id : {0}, BUT these versions were found : {1}
     * 
     */
    public static Localizable _HubBuildScan_getVersionNonExistingIn_0_(Object arg1, Object arg2) {
        return new Localizable(holder, "HubBuildScan_getVersionNonExistingIn_0_", arg1, arg2);
    }

    /**
     * Black Duck Hub Integration
     * 
     */
    public static String HubBuildScan_getDisplayName() {
        return holder.format("HubBuildScan_getDisplayName");
    }

    /**
     * Black Duck Hub Integration
     * 
     */
    public static Localizable _HubBuildScan_getDisplayName() {
        return new Localizable(holder, "HubBuildScan_getDisplayName");
    }

    /**
     * Need to provide a Project version.
     * 
     */
    public static String HubBuildScan_getProvideProjectVersion() {
        return holder.format("HubBuildScan_getProvideProjectVersion");
    }

    /**
     * Need to provide a Project version.
     * 
     */
    public static Localizable _HubBuildScan_getProvideProjectVersion() {
        return new Localizable(holder, "HubBuildScan_getProvideProjectVersion");
    }

    /**
     * Need to provide some memory for the scan
     * 
     */
    public static String HubBuildScan_getNeedMemory() {
        return holder.format("HubBuildScan_getNeedMemory");
    }

    /**
     * Need to provide some memory for the scan
     * 
     */
    public static Localizable _HubBuildScan_getNeedMemory() {
        return new Localizable(holder, "HubBuildScan_getNeedMemory");
    }

}
