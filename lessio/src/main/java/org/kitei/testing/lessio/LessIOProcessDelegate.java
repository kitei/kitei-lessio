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
package org.kitei.testing.lessio;

import static java.lang.String.format;

import static org.kitei.testing.lessio.LessIOUtils.hasAnnotations;

class LessIOProcessDelegate
{
    private final LessIOContext context;

    LessIOProcessDelegate(final LessIOContext context)
    {
        this.context = context;
    }

    LessIOPredicate getExecuteProcessPredicate(final String cmd)
    {
        return new ProcessExecuteProcessPredicate(cmd);
    }

    private static class ProcessExecuteProcessPredicate implements LessIOPredicate
    {
        private final String cmd;

        private ProcessExecuteProcessPredicate(final String cmd)
        {
            this.cmd = cmd;
        }

        @Override
        public boolean check(final Class<?> clazz)
            throws Exception
        {
            return hasAnnotations(clazz, AllowExternalProcess.class);
        }

        @Override
        public String toString()
        {
            return format("@AllowExternalProcess for %s (exec)", cmd);
        }
    }
}
