package com.blackducksoftware.integration.hub.jenkins.remote;

import hudson.remoting.Callable;

import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;

import org.jenkinsci.remoting.Role;
import org.jenkinsci.remoting.RoleChecker;

public class GetHostNameFromNetworkInterfaces implements Callable<String, IOException> {
    private static final long serialVersionUID = 3459269768733083577L;

    @Override
    public String call() throws IOException {
        String hostName = null;

        // Get the network interfaces for this machine
        Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();

        while (interfaces.hasMoreElements()) {
            NetworkInterface nic = interfaces.nextElement();
            // Get the addresses for this network interface
            Enumeration<InetAddress> addresses = nic.getInetAddresses();
            // will loop through the addresses until it finds a non loop back address that has a host name
            while (hostName == null && addresses.hasMoreElements()) {
                InetAddress address = addresses.nextElement();
                // if the address is not a loopback address then get the host name
                if (!address.isLoopbackAddress()) {
                    hostName = address.getHostName();
                    break;
                }
            }
            if (hostName != null) {
                break;
            }
        }

        return hostName;
    }

    @Override
    public void checkRoles(RoleChecker checker) throws SecurityException {
        checker.check(this, new Role(GetHostNameFromNetworkInterfaces.class));
    }
}
