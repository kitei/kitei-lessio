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
package org.kitei.testing.lessio.testng;

import java.io.IOException;
import java.net.InetAddress;

import com.google.common.base.Optional;

import org.kitei.testing.lessio.AllowDNSResolution;
import org.kitei.testing.lessio.LessIOSecurityManager;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class AllowDNSResolutionTest extends AbstractLessIOSecurityManagerTest
{
    protected class DisallowedOperation implements RunnableWithException
    {
        @Override
        public void run() throws IOException
        {
            InetAddress.getByName("example.com");
        }
    }

    protected class LocalhostOperation implements RunnableWithException
    {
        @Override
        public void run() throws IOException
        {
            InetAddress.getByName("127.0.0.1");
            InetAddress.getByName("localhost");
            InetAddress.getByName("::1");
        }
    }

    @AllowDNSResolution
    protected class AllowedOperation extends DisallowedOperation
    {
        @Override
        public void run() throws IOException
        {
            super.run();
        }
    }

    LessIOSecurityManager sm;

    @BeforeMethod
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
    public void testNonAnnotatedLocalhost()
    {
        assertAllowed(sm, new LocalhostOperation(), Optional.<Class<? extends Exception>>absent());
    }

    @Test
    public void testAnnotatedOperations()
    {
        assertAllowed(sm, new AllowedOperation(), Optional.<Class<? extends Exception>>absent());
    }
}
