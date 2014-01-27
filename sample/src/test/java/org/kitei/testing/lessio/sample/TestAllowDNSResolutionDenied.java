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
package org.kitei.testing.lessio.sample;

import static org.junit.Assert.assertNotNull;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;

import org.junit.Test;
import org.kitei.testing.lessio.LessIOException;

public class TestAllowDNSResolutionDenied
{
    @Test(expected = LessIOException.class)
    public void testDNSLookup() throws Exception
    {
        InetAddress googleIpv4 = Inet4Address.getByName("google.com");
        assertNotNull(googleIpv4);

        InetAddress googleIpv6 = Inet6Address.getByName("ipv6.google.com");
        assertNotNull(googleIpv6);

        String googleRevIpv4 = Inet4Address.getByName(googleIpv4.getHostAddress()).getCanonicalHostName();
        assertNotNull(googleRevIpv4);

        String googleRevIpv6 = Inet4Address.getByName(googleIpv6.getHostAddress()).getCanonicalHostName();
        assertNotNull(googleRevIpv6);
    }
}

