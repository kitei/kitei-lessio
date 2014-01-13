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
import java.net.InetAddress;

import com.google.common.base.Optional;

import org.junit.Before;
import org.junit.Test;
import org.kitei.testing.lessio.AllowDNSResolution;
import org.kitei.testing.lessio.LessIOSecurityManager;

public class TestInheritance extends AbstractLessIOSecurityManagerTest
{
    protected class DisallowedOperation implements RunnableWithException
    {
        @Override
        public void run() throws IOException
        {
            InetAddress.getByName("example.com");
        }
    }

    @AllowDNSResolution
    protected abstract class BaseOperation
    {
    }

    protected class AllowedOperation extends BaseOperation implements RunnableWithException
    {
        @Override
        public void run() throws IOException
        {
            InetAddress.getByName("example.com");
        }
    }

    private LessIOSecurityManager sm;

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
    public void testAnnotatedOperations()
    {
        assertAllowed(sm, new AllowedOperation(), Optional.<Class<? extends Exception>>absent());
    }
}
