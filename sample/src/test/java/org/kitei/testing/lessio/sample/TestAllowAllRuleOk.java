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

import java.io.File;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.kitei.testing.lessio.AllowAll;

public class TestAllowAllRuleOk
{
    @Rule
    public final AllowAllRule testRule = new AllowAllRule();

    @Test
    public void testSimple()
    {
        String value = testRule.getDefaultValue();

        String result = "can read";

        assertEquals("Result did not match", value, result);
    }

    @AllowAll
    public static class AllowAllRule implements TestRule
    {
        private String value = null;

        @Override
        public Statement apply(Statement base, Description description)
        {
            // need to do some file I/O
            final File services = new File("/etc/services");
            if (services.canRead()) {
                value = "can read";
            }

            return base;
        }

        public String getDefaultValue()
        {
            return value;
        }
    }
}
