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

import static java.lang.String.format;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.google.common.base.Optional;

import org.kitei.testing.lessio.LessIOSecurityManager.CantDoItException;

public abstract class AbstractLessIOSecurityManagerTest
{
    protected interface RunnableWithException
    {
        public void run() throws Exception;
    }

    protected void withTemporarySM(final SecurityManager sm, final Runnable runnable)
    {
        final SecurityManager previous = System.getSecurityManager();
        System.setSecurityManager(sm);
        try {
            runnable.run();
        }
        finally {
            System.setSecurityManager(previous);
        }
    }

    protected void assertAllowed(final SecurityManager sm, final RunnableWithException runnable, final Optional<Class<? extends Exception>> expectedOption)
    {
        withTemporarySM(sm, new Runnable() {
            @Override
            public void run()
            {
                try {
                    runnable.run();
                }
                catch (final Exception e) {
                    assertFalse(format("Action must be allowed. I should not catch a %s (%s)", e.getClass().getCanonicalName(), e.getLocalizedMessage()), e instanceof CantDoItException);
                    if (expectedOption.isPresent()) {
                        final Class<?> expected = expectedOption.get();
                        assertTrue(format("Expecting exception %s but received %s (%s)", expected.getCanonicalName(), e.getClass().getCanonicalName(), e.getLocalizedMessage()), e.getClass().isAssignableFrom(expected));
                        return;
                    }
                    fail(format("Unexpected exception: %s (%s)", e.getClass().getCanonicalName(), e.getLocalizedMessage()));
                }
                if (expectedOption.isPresent()) {
                    fail(format("Expected exception %s, but no exception was thrown!", expectedOption.get().getCanonicalName()));
                }
            }
        });
    }

    protected void assertDisallowed(final SecurityManager sm, final RunnableWithException runnable)
    {
        withTemporarySM(sm, new Runnable() {
            @Override
            public void run()
            {
                try {
                    runnable.run();
                }
                catch (final Exception e) {
                    assertTrue(format("Action must be disallowed. However, no CantDoItException was thrown. Instead, a %s (%s) was caught.", e.getClass().getCanonicalName(), e.getLocalizedMessage()),
                        e instanceof CantDoItException);
                    return;
                }
                fail("Action must be disallowed. However, no CantDoItException was thrown.");
            }
        });
    }
}
