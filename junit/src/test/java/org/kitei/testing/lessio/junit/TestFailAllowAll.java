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

import java.net.InetAddress;

import org.junit.Before;
import org.junit.Test;
import org.kitei.testing.lessio.AllowAll;
import org.kitei.testing.lessio.LessIOSecurityManager;

@AllowAll
public class TestFailAllowAll extends AbstractLessIOSecurityManagerTest
{
    private LessIOSecurityManager sm;

    @Before
    public void setupSecurityManager()
    {
        sm = new LessIOSecurityManager();
    }

    @Test
    public void testSimple()
    {
        assertDisallowed(sm, new RunnableWithException() {
            @Override
            public void run() throws Exception
            {
                InetAddress.getByName("example.com");
            }
        });
    }
}
