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
package org.kitei.testing.lessio.junit;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

import com.google.common.base.Optional;

import org.junit.Before;
import org.junit.Test;
import org.kitei.testing.lessio.AllowNetworkListen;
import org.kitei.testing.lessio.AllowNetworkMulticast;
import org.kitei.testing.lessio.LessIOSecurityManager;

public class AllowNetworkMulticastTest extends AbstractLessIOSecurityManagerTest
{
    protected class DisallowedOperation implements RunnableWithException
    {
        @Override
        public void run() throws IOException
        {
            try (final DatagramSocket socket = new DatagramSocket(0)) {
                final InetAddress group = InetAddress.getByName("230.0.0.1");
                final String buffer = "foo";
                final DatagramPacket packet = new DatagramPacket(buffer.getBytes(), buffer.length(), group, 4446);
                socket.send(packet);
            }
        }
    }

    @AllowNetworkMulticast
    @AllowNetworkListen(ports = { 0 })
    protected class AllowedOperation extends DisallowedOperation
    {
        @Override
        public void run() throws IOException
        {
            super.run();
        }
    }

    LessIOSecurityManager sm;

    @Before
    public void setupSecurityManager()
    {
        sm = new LessIOSecurityManager();
    }

    @Test
    public void testNonAnnotatedOperation()
    {
        assertDisallowed(sm, new DisallowedOperation());
    }

    @Test
    public void testAnnotatedOperation()
    {
        assertAllowed(sm, new AllowedOperation(),
            Optional.<Class<? extends Exception>>absent());
    }
}
