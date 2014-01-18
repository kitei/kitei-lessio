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

import static org.junit.Assert.assertNotNull;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import com.google.common.base.Joiner;
import com.google.common.io.CharStreams;

import org.junit.Test;
import org.kitei.testing.lessio.LessIOSecurityManager.CantDoItException;

public class TestAllowNetworkAccessDenied
{
    @Test(expected = CantDoItException.class)
    public void testNetworkAccess() throws Exception
    {
        final URL wellKnown = new URL("http://google.com/");

        try (BufferedReader r = new BufferedReader(new InputStreamReader(wellKnown.openStream(), StandardCharsets.UTF_8))) {
            String data = Joiner.on("\n").join(CharStreams.readLines(r));
            assertNotNull("no data received?", data);
        }
    }
}
