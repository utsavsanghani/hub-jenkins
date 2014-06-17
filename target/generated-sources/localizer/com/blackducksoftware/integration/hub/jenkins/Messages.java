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
     * Can not reach this server
     * 
     */
    public static String CodeCenterBuildWrapper_getCanNotReachThisServer() {
        return holder.format("CodeCenterBuildWrapper_getCanNotReachThisServer");
    }

    /**
     * Can not reach this server
     * 
     */
    public static Localizable _CodeCenterBuildWrapper_getCanNotReachThisServer() {
        return new Localizable(holder, "CodeCenterBuildWrapper_getCanNotReachThisServer");
    }

    /**
     * This application has been created: {0}
     * 
     */
    public static String CodeCenterBuildWrapper_getApplicationCreated_0_(Object arg1) {
        return holder.format("CodeCenterBuildWrapper_getApplicationCreated_0_", arg1);
    }

    /**
     * This application has been created: {0}
     * 
     */
    public static Localizable _CodeCenterBuildWrapper_getApplicationCreated_0_(Object arg1) {
        return new Localizable(holder, "CodeCenterBuildWrapper_getApplicationCreated_0_", arg1);
    }

    /**
     * Black Duck Code Center integration
     * 
     */
    public static String CodeCenterBuildWrapper_getDisplayName() {
        return holder.format("CodeCenterBuildWrapper_getDisplayName");
    }

    /**
     * Black Duck Code Center integration
     * 
     */
    public static Localizable _CodeCenterBuildWrapper_getDisplayName() {
        return new Localizable(holder, "CodeCenterBuildWrapper_getDisplayName");
    }

    /**
     * Not a valid URL
     * 
     */
    public static String CodeCenterBuildWrapper_getNotAValidUrl() {
        return holder.format("CodeCenterBuildWrapper_getNotAValidUrl");
    }

    /**
     * Not a valid URL
     * 
     */
    public static Localizable _CodeCenterBuildWrapper_getNotAValidUrl() {
        return new Localizable(holder, "CodeCenterBuildWrapper_getNotAValidUrl");
    }

    /**
     * Governance Report
     * 
     */
    public static String CodeCenterGovernanceReportAction_getDisplayName() {
        return holder.format("CodeCenterGovernanceReportAction_getDisplayName");
    }

    /**
     * Governance Report
     * 
     */
    public static Localizable _CodeCenterGovernanceReportAction_getDisplayName() {
        return new Localizable(holder, "CodeCenterGovernanceReportAction_getDisplayName");
    }

    /**
     * Please fill in the Code Center credentials in the global settings!
     * 
     */
    public static String CodeCenterBuildWrapper_getPleaseFillInGlobalCCCredentials() {
        return holder.format("CodeCenterBuildWrapper_getPleaseFillInGlobalCCCredentials");
    }

    /**
     * Please fill in the Code Center credentials in the global settings!
     * 
     */
    public static Localizable _CodeCenterBuildWrapper_getPleaseFillInGlobalCCCredentials() {
        return new Localizable(holder, "CodeCenterBuildWrapper_getPleaseFillInGlobalCCCredentials");
    }

    /**
     * Credentials valid for: {0}
     * 
     */
    public static String CodeCenterBuildWrapper_getCredentialsValidFor_0_(Object arg1) {
        return holder.format("CodeCenterBuildWrapper_getCredentialsValidFor_0_", arg1);
    }

    /**
     * Credentials valid for: {0}
     * 
     */
    public static Localizable _CodeCenterBuildWrapper_getCredentialsValidFor_0_(Object arg1) {
        return new Localizable(holder, "CodeCenterBuildWrapper_getCredentialsValidFor_0_", arg1);
    }

    /**
     * Please set an application version!
     * 
     */
    public static String CodeCenterBuildWrapper_getPleaseSetApplicationVersion() {
        return holder.format("CodeCenterBuildWrapper_getPleaseSetApplicationVersion");
    }

    /**
     * Please set an application version!
     * 
     */
    public static Localizable _CodeCenterBuildWrapper_getPleaseSetApplicationVersion() {
        return new Localizable(holder, "CodeCenterBuildWrapper_getPleaseSetApplicationVersion");
    }

    /**
     * Please set a Server URL
     * 
     */
    public static String CodeCenterBuildWrapper_getPleaseSetServerUrl() {
        return holder.format("CodeCenterBuildWrapper_getPleaseSetServerUrl");
    }

    /**
     * Please set a Server URL
     * 
     */
    public static Localizable _CodeCenterBuildWrapper_getPleaseSetServerUrl() {
        return new Localizable(holder, "CodeCenterBuildWrapper_getPleaseSetServerUrl");
    }

    /**
     * Directed to Code Center.
     * 
     */
    public static String CodeCenterBuildWrapper_getDirectedToCodeCenter() {
        return holder.format("CodeCenterBuildWrapper_getDirectedToCodeCenter");
    }

    /**
     * Directed to Code Center.
     * 
     */
    public static Localizable _CodeCenterBuildWrapper_getDirectedToCodeCenter() {
        return new Localizable(holder, "CodeCenterBuildWrapper_getDirectedToCodeCenter");
    }

    /**
     * Please set an application name!
     * 
     */
    public static String CodeCenterBuildWrapper_getPleaseSetApplicationName() {
        return holder.format("CodeCenterBuildWrapper_getPleaseSetApplicationName");
    }

    /**
     * Please set an application name!
     * 
     */
    public static Localizable _CodeCenterBuildWrapper_getPleaseSetApplicationName() {
        return new Localizable(holder, "CodeCenterBuildWrapper_getPleaseSetApplicationName");
    }

    /**
     * Black Duck Code Center Governance Report
     * 
     */
    public static String CodeCenterGovernanceReportAction_H1_Governance_Report() {
        return holder.format("CodeCenterGovernanceReportAction_H1_Governance_Report");
    }

    /**
     * Black Duck Code Center Governance Report
     * 
     */
    public static Localizable _CodeCenterGovernanceReportAction_H1_Governance_Report() {
        return new Localizable(holder, "CodeCenterGovernanceReportAction_H1_Governance_Report");
    }

}
