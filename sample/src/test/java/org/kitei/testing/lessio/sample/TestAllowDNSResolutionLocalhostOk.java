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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;

import org.junit.Test;

public class TestAllowDNSResolutionLocalhostOk
{
    @Test
    public void testDNSLookup() throws Exception
    {
        InetAddress localhost = Inet4Address.getByName("localhost");
        assertNotNull(localhost);

        String localIpv4 = Inet4Address.getByName("127.0.0.1").getCanonicalHostName();
        assertNotNull(localIpv4);
        assertEquals("not localhost for ipv4", "localhost", localIpv4);

        String localIpv6 = Inet6Address.getByName("::1").getCanonicalHostName();
        assertNotNull(localIpv6);
        assertEquals("not localhost for ipv6", "localhost", localIpv6);
    }
}
