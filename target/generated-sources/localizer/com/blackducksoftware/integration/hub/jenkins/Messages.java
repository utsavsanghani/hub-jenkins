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
     * This Release does not exist in the Project with Id : {0}, BUT these releases were found : {1}
     * 
     */
    public static String HubBuildScan_getReleaseNonExistingIn_0_(Object arg1, Object arg2) {
        return holder.format("HubBuildScan_getReleaseNonExistingIn_0_", arg1, arg2);
    }

    /**
     * This Release does not exist in the Project with Id : {0}, BUT these releases were found : {1}
     * 
     */
    public static Localizable _HubBuildScan_getReleaseNonExistingIn_0_(Object arg1, Object arg2) {
        return new Localizable(holder, "HubBuildScan_getReleaseNonExistingIn_0_", arg1, arg2);
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
     * The Project and Release specified already exist!
     * 
     */
    public static String HubBuildScan_getProjectAndReleaseExist() {
        return holder.format("HubBuildScan_getProjectAndReleaseExist");
    }

    /**
     * The Project and Release specified already exist!
     * 
     */
    public static Localizable _HubBuildScan_getProjectAndReleaseExist() {
        return new Localizable(holder, "HubBuildScan_getProjectAndReleaseExist");
    }

    /**
     * This Release exists in the Project with Id : {0}
     * 
     */
    public static String HubBuildScan_getReleaseExistsIn_0_(Object arg1) {
        return holder.format("HubBuildScan_getReleaseExistsIn_0_", arg1);
    }

    /**
     * This Release exists in the Project with Id : {0}
     * 
     */
    public static Localizable _HubBuildScan_getReleaseExistsIn_0_(Object arg1) {
        return new Localizable(holder, "HubBuildScan_getReleaseExistsIn_0_", arg1);
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
     * Need to provide a Project release.
     * 
     */
    public static String HubBuildScan_getProvideProjectRelease() {
        return holder.format("HubBuildScan_getProvideProjectRelease");
    }

    /**
     * Need to provide a Project release.
     * 
     */
    public static Localizable _HubBuildScan_getProvideProjectRelease() {
        return new Localizable(holder, "HubBuildScan_getProvideProjectRelease");
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
     * Can not reach this server
     * 
     */
    public static String HubBuildScan_getCanNotReachThisServer() {
        return holder.format("HubBuildScan_getCanNotReachThisServer");
    }

    /**
     * Can not reach this server
     * 
     */
    public static Localizable _HubBuildScan_getCanNotReachThisServer() {
        return new Localizable(holder, "HubBuildScan_getCanNotReachThisServer");
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
     * The Project and Release have been created!
     * 
     */
    public static String HubBuildScan_getProjectAndReleaseCreated() {
        return holder.format("HubBuildScan_getProjectAndReleaseCreated");
    }

    /**
     * The Project and Release have been created!
     * 
     */
    public static Localizable _HubBuildScan_getProjectAndReleaseCreated() {
        return new Localizable(holder, "HubBuildScan_getProjectAndReleaseCreated");
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

}
