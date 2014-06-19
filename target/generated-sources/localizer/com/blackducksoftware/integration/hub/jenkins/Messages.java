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
     * Add another iScan
     * 
     */
    public static String AddScan() {
        return holder.format("AddScan");
    }

    /**
     * Add another iScan
     * 
     */
    public static Localizable _AddScan() {
        return new Localizable(holder, "AddScan");
    }

    /**
     * Directory=Tool Location
     * 
     */
    public static String Home() {
        return holder.format("Home");
    }

    /**
     * Directory=Tool Location
     * 
     */
    public static Localizable _Home() {
        return new Localizable(holder, "Home");
    }

    /**
     * Credentials
     * 
     */
    public static String Credentials() {
        return holder.format("Credentials");
    }

    /**
     * Credentials
     * 
     */
    public static Localizable _Credentials() {
        return new Localizable(holder, "Credentials");
    }

    /**
     * Please fill in the Hub credentials in the global settings!
     * 
     */
    public static String HubBuildScan_getPleaseFillInGlobalCCCredentials() {
        return holder.format("HubBuildScan_getPleaseFillInGlobalCCCredentials");
    }

    /**
     * Please fill in the Hub credentials in the global settings!
     * 
     */
    public static Localizable _HubBuildScan_getPleaseFillInGlobalCCCredentials() {
        return new Localizable(holder, "HubBuildScan_getPleaseFillInGlobalCCCredentials");
    }

    /**
     * Directed to the Hub.
     * 
     */
    public static String HubBuildScan_getDirectedToHub() {
        return holder.format("HubBuildScan_getDirectedToHub");
    }

    /**
     * Directed to the Hub.
     * 
     */
    public static Localizable _HubBuildScan_getDirectedToHub() {
        return new Localizable(holder, "HubBuildScan_getDirectedToHub");
    }

    /**
     * IScan Installation
     * 
     */
    public static String Name() {
        return holder.format("Name");
    }

    /**
     * IScan Installation
     * 
     */
    public static Localizable _Name() {
        return new Localizable(holder, "Name");
    }

    /**
     * Please set an application name!
     * 
     */
    public static String HubBuildScan_getPleaseSetApplicationName() {
        return holder.format("HubBuildScan_getPleaseSetApplicationName");
    }

    /**
     * Please set an application name!
     * 
     */
    public static Localizable _HubBuildScan_getPleaseSetApplicationName() {
        return new Localizable(holder, "HubBuildScan_getPleaseSetApplicationName");
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
     * iScan Target
     * 
     */
    public static String ScanTarget() {
        return holder.format("ScanTarget");
    }

    /**
     * iScan Target
     * 
     */
    public static Localizable _ScanTarget() {
        return new Localizable(holder, "ScanTarget");
    }

    /**
     * This application has been created: {0}
     * 
     */
    public static String HubBuildScan_getApplicationCreated_0_(Object arg1) {
        return holder.format("HubBuildScan_getApplicationCreated_0_", arg1);
    }

    /**
     * This application has been created: {0}
     * 
     */
    public static Localizable _HubBuildScan_getApplicationCreated_0_(Object arg1) {
        return new Localizable(holder, "HubBuildScan_getApplicationCreated_0_", arg1);
    }

    /**
     * Test Connection
     * 
     */
    public static String TestConnection() {
        return holder.format("TestConnection");
    }

    /**
     * Test Connection
     * 
     */
    public static Localizable _TestConnection() {
        return new Localizable(holder, "TestConnection");
    }

    /**
     * Connection Timeout
     * 
     */
    public static String ConnectionTimeout() {
        return holder.format("ConnectionTimeout");
    }

    /**
     * Connection Timeout
     * 
     */
    public static Localizable _ConnectionTimeout() {
        return new Localizable(holder, "ConnectionTimeout");
    }

    /**
     * Please set an application version!
     * 
     */
    public static String HubBuildScan_getPleaseSetApplicationVersion() {
        return holder.format("HubBuildScan_getPleaseSetApplicationVersion");
    }

    /**
     * Please set an application version!
     * 
     */
    public static Localizable _HubBuildScan_getPleaseSetApplicationVersion() {
        return new Localizable(holder, "HubBuildScan_getPleaseSetApplicationVersion");
    }

    /**
     * Server URL
     * 
     */
    public static String ServerURL() {
        return holder.format("ServerURL");
    }

    /**
     * Server URL
     * 
     */
    public static Localizable _ServerURL() {
        return new Localizable(holder, "ServerURL");
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
     * Delete iScan
     * 
     */
    public static String DeleteScan() {
        return holder.format("DeleteScan");
    }

    /**
     * Delete iScan
     * 
     */
    public static Localizable _DeleteScan() {
        return new Localizable(holder, "DeleteScan");
    }

    /**
     * Black Duck Hub
     * 
     */
    public static String GlobalSectionTitle() {
        return holder.format("GlobalSectionTitle");
    }

    /**
     * Black Duck Hub
     * 
     */
    public static Localizable _GlobalSectionTitle() {
        return new Localizable(holder, "GlobalSectionTitle");
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
