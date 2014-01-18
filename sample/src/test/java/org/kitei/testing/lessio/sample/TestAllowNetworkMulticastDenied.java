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

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import org.junit.Test;
import org.kitei.testing.lessio.AllowNetworkListen;
import org.kitei.testing.lessio.LessIOSecurityManager.CantDoItException;

@AllowNetworkListen(ports={0})
public class TestAllowNetworkMulticastDenied
{
    @Test(expected = CantDoItException.class)
    public void testMulticast() throws Exception
    {
            try (final DatagramSocket socket = new DatagramSocket(0)) {
                final InetAddress group = Inet4Address.getByName("230.254.254.1");
                final InetSocketAddress socketAddress = new InetSocketAddress(group, findUnusedPort());
                final String buffer = "foobar" + UUID.randomUUID();
                byte [] bytes = buffer.getBytes(StandardCharsets.UTF_8);
                final DatagramPacket packet = new DatagramPacket(bytes, bytes.length, socketAddress);
                socket.send(packet);
            }
    }

    public static int findUnusedPort() throws IOException
    {
        try (final ServerSocket socket = new ServerSocket()) {
            socket.bind(new InetSocketAddress(0));
            return socket.getLocalPort();
        }
    }
}
