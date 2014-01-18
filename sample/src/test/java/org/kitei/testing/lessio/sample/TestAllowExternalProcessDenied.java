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
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import com.google.common.base.Joiner;
import com.google.common.io.CharStreams;

import org.junit.Test;
import org.kitei.testing.lessio.LessIOSecurityManager.CantDoItException;

public class TestAllowExternalProcessDenied
{
    @Test(expected = CantDoItException.class)
    public void testExternalProcess() throws Exception
    {
        Process p = new ProcessBuilder("/usr/bin/whoami").start();
        assertEquals("process exited non-zero", 0, p.waitFor());
        try (InputStreamReader r = new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8)) {
                String data = Joiner.on("\n").join(CharStreams.readLines(r));
                assertNotNull("no user name read", data);
            }
    }

    @Test(expected = CantDoItException.class)
    public void testCanExecute() throws Exception
    {
        final File f = new File("/usr/bin/whoami");
        assertTrue("can not execute /usr/bin/whoami", f.canExecute());
    }
}
