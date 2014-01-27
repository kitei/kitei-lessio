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

import static org.junit.Assert.assertFalse;

import java.io.File;
import java.util.UUID;

import org.junit.Test;
import org.kitei.testing.lessio.AllowLocalFileAccess;

@AllowLocalFileAccess(paths={"%TMP_DIR%/hello-*"})
public class TestAllowLocalFileAccessTmpSubDirDeprecated
{
    @Test
    public void testLocalFileSystem()
    {
        final File f = new File(System.getProperty("java.io.tmpdir"), "hello-bogus" + UUID.randomUUID().toString());
        assertFalse(f.getAbsolutePath() + " exists???", f.exists());
    }
}
