/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.kitei.testing.lessio;

import static java.lang.String.format;

import static org.kitei.testing.lessio.LessIOUtils.checkNotNull;
import static org.kitei.testing.lessio.LessIOUtils.findAnnotation;
import static org.kitei.testing.lessio.LessIOUtils.hasAnnotations;

import java.net.InetAddress;

class LessIONetworkDelegate
{
    private final LessIOContext context;

    LessIONetworkDelegate(final LessIOContext context)
    {
        this.context = context;
    }

    LessIOPredicate getAcceptPredicate(final String host, final int port)
    {
        return new NetworkEndpointPredicate(host, port, "accept");
    }

    LessIOPredicate getConnectPredicate(final String host, final int port)
    {
        if (port == -1) {
            return new NetworkDNSResolutionPredicate(host);
        }
        else {
            return new NetworkEndpointPredicate(host, port, "connect");
        }
    }

    LessIOPredicate getListenPredicate(final int port)
    {
        return new NetworkListenPredicate(port);
    }

    LessIOPredicate getMulticastPredicate(final InetAddress maddr)
    {
        return new NetworkMulticastPredicate();
    }

    private boolean isValidEphemeralPort(final int port)
    {
        return port == 0 || port >= context.getLowestEphemeralPort() && port <= context.getHighestEphemeralPort();
    }

    private class NetworkDNSResolutionPredicate implements LessIOPredicate
    {
        private final boolean whitelisted;

        private NetworkDNSResolutionPredicate(final String host)
        {
            checkNotNull(host, "host is null");

            this.whitelisted = context.getWhitelistedHosts().contains(host);
        }

        @Override
        public boolean check(final Class<?> clazz)
        {
            return whitelisted || hasAnnotations(clazz, AllowDNSResolution.class, AllowNetworkMulticast.class, AllowNetworkListen.class, AllowNetworkAccess.class);
        }

        @Override
        public String toString()
        {
            return "Network DNS resolution.";
        }
    }

    private class NetworkEndpointPredicate implements LessIOPredicate
    {
        private final String host;
        private final int port;
        private final String portAsString;

        private final String description;

        private NetworkEndpointPredicate(final String host, final int port, final String description)
        {
            this.host = checkNotNull(host, "host is null");
            this.port = port;
            this.portAsString = Integer.toString(port);

            this.description = description;
        }

        @Override
        public boolean check(final Class<?> clazz)
        {
            final AllowNetworkAccess access = LessIOUtils.findAnnotation(clazz, AllowNetworkAccess.class);
            if (access == null) {
                return false;
            }

            final String[] endpoints = access.endpoints();
            if (endpoints == null) {
                return false;
            }

            for (final String endpoint : endpoints) {
                final String[] parts = endpoint.split(":");

                if (parts.length == 2) {
                    if (!(parts[0].equals("*")
                        || parts[0].equals(host))) {
                        // The host part is not a wildcard and does not match. No need to check the port.
                        return false;
                    }

                    if (parts[1].equals("*")
                        || parts[1].equals(portAsString)
                        || parts[1].equals("0") && isValidEphemeralPort(port))
                    {
                        return true;
                    }
                }
            }

            return false;
        }

        @Override
        public String toString()
        {
            return format("Network access to %s:%d (%s).", host, port, description);
        }
    }

    private class NetworkListenPredicate implements LessIOPredicate
    {
        private final int port;

        private NetworkListenPredicate(final int port)
        {
            this.port = port;
        }

        @Override
        public boolean check(final Class<?> clazz)
        {
            final AllowNetworkListen listen = findAnnotation(clazz, AllowNetworkListen.class);
            if (listen == null) {
                return false;
            }

            final int[] ports = listen.ports();
            if (ports == null) {
                return false;
            }

            for (final int p : ports) {
                if (p == 0 && isValidEphemeralPort(port)) {
                    return true;
                }
                else if (p == port) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public String toString()
        {
            return format("Network listen on port %d.", port);
        }
    }

    private static class NetworkMulticastPredicate implements LessIOPredicate
    {
        @Override
        public boolean check(final Class<?> clazz)
        {
            return hasAnnotations(clazz, AllowNetworkMulticast.class);
        }

        @Override
        public String toString()
        {
            return "Network Multicast access.";
        }
    }
}
