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
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.Socket;

import com.google.common.base.Optional;

import org.kitei.testing.lessio.AllowNetworkAccess;
import org.kitei.testing.lessio.LessIOSecurityManager;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class AllowNetworkAccessTest extends AbstractLessIOSecurityManagerTest
{
    protected class DisallowedOperation implements RunnableWithException
    {
        @Override
        public void run() throws IOException
        {
            try (Socket s = new Socket()) {
                s.connect(new InetSocketAddress("localhost", 1));
            }
        }
    }

    @AllowNetworkAccess(endpoints = { "127.0.0.1:1" })
    protected class AllowedOperation extends DisallowedOperation
    {
        @Override
        public void run() throws IOException
        {
            super.run();
        }
    }

    @AllowNetworkAccess(endpoints = { "*:1" })
    protected class AllowedWildcardOperation extends DisallowedOperation
    {
        @Override
        public void run() throws IOException
        {
            super.run();
        }
    }

    @AllowNetworkAccess(endpoints = { "localhost:25" })
    protected class MisannotatedOperation extends DisallowedOperation
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
    public void testAnnotatedOperation()
    {
        assertAllowed(sm, new AllowedOperation(), Optional.<Class<? extends Exception>>of(ConnectException.class));
    }

    @Test
    public void testMisannotatedOperation()
    {
        assertDisallowed(sm, new MisannotatedOperation());
    }

    @Test
    public void testWildcardOperation()
    {
        assertAllowed(sm, new AllowedWildcardOperation(), Optional.<Class<? extends Exception>>of(ConnectException.class));
    }
}
