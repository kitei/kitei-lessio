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

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

import com.google.common.base.Optional;

import org.junit.Before;
import org.junit.Test;
import org.kitei.testing.lessio.LessIOSecurityManager;
import org.kitei.testing.lessio.LessIOSecurityManager.CantDoItException;

public class LessIOSecurityManagerTest extends AbstractLessIOSecurityManagerTest
{
    LessIOSecurityManager sm;

    @Before
    public void setupSecurityManager()
    {
        sm = new LessIOSecurityManager();
    }

    @Test
    public void testNoSecurityManager()
    {
        withTemporarySM(null, new Runnable() {
            @Override
            public void run()
            {
                try {
                    openSocket();
                }
                catch (final Exception e) {
                    assertTrue(String.format("Received %s (%s) instead of IOException",
                        System.getSecurityManager(), e.getClass().getCanonicalName(),
                        e.getLocalizedMessage()), e instanceof IOException);
                }
            }
        });
    }

    @Test
    public void testSecurityManager()
    {
        withTemporarySM(sm, new Runnable() {
            @Override
            public void run()
            {
                try {
                    openSocket();
                }
                catch (final Exception e) {
                    assertTrue(String.format(
                        "Received %s (%s) instead of CantDoItException", e.getClass()
                            .getCanonicalName(), e.getLocalizedMessage()),
                        e instanceof CantDoItException);
                }
            }
        });
    }

    @Test
    public void testAssertAllowed()
    {
        assertAllowed(
            sm,
            new RunnableWithException() {
                @Override
                public void run() throws Exception
                {
                    throw new UnsupportedOperationException();
                }
            },
            Optional
                .<Class<? extends Exception>>of(UnsupportedOperationException.class));

        try {
            assertAllowed(sm, new RunnableWithException() {
                @Override
                public void run() throws Exception
                {
                    throw new CantDoItException("");
                }
            }, Optional.<Class<? extends Exception>>absent());
        }
        catch (final AssertionError e) {
            // Success
        }
    }

    @Test
    public void testAssertDisallowed()
    {
        assertDisallowed(sm, new RunnableWithException() {
            @Override
            public void run() throws Exception
            {
                throw new CantDoItException("");
            }
        });

        try {
            assertDisallowed(sm, new RunnableWithException() {
                @Override
                public void run() throws Exception
                {
                    // Intentionally left empty.
                }
            });
        }
        catch (final AssertionError e) {
            // Success
        }
    }

    private void openSocket() throws IOException
    {
        try (Socket s = new Socket()) {
            s.connect(new InetSocketAddress("localhost", 1));
        }
    }

}
