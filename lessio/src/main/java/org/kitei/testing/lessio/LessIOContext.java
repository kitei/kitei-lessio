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

import static java.util.Collections.newSetFromMap;

import static org.kitei.testing.lessio.LessIOUtils.checkValidPort;

import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Set;

final class LessIOContext
{
    private final Set<Class<?>> testrunnerClasses;

    private final Set<Class<?>> whitelistedClasses;
    private final Set<String> whitelistedHosts;
    private final Set<Path> whitelistedPaths;

    private final Set<PathMatcher> whitelistedPathGlobs;

    private final int lowestEphemeralPort;
    private final int highestEphemeralPort;

    public static Builder builder()
    {
        return new Builder();
    }

    private LessIOContext(final Set<Class<?>> testrunnerClasses,
                          final Set<Class<?>> whitelistedClasses,
                          final Set<String> whitelistedHosts,
                          final Set<Path> whitelistedPaths,
                          final Set<PathMatcher> whitelistedPathGlobs,
                          final int lowestEphemeralPort,
                          final int highestEphemeralPort)
    {
        this.testrunnerClasses = testrunnerClasses;
        this.whitelistedClasses = whitelistedClasses;
        this.whitelistedHosts = whitelistedHosts;
        this.whitelistedPaths = whitelistedPaths;
        this.whitelistedPathGlobs = whitelistedPathGlobs;
        this.lowestEphemeralPort = lowestEphemeralPort;
        this.highestEphemeralPort = highestEphemeralPort;
    }

    public Set<Class<?>> getTestrunnerClasses()
    {
        return testrunnerClasses;
    }

    public Set<Class<?>> getWhitelistedClasses()
    {
        return whitelistedClasses;
    }

    public Set<String> getWhitelistedHosts()
    {
        return whitelistedHosts;
    }

    public Set<Path> getWhitelistedPaths()
    {
        return whitelistedPaths;
    }

    public Set<PathMatcher> getWhitelistedPathGlobs()
    {
        return whitelistedPathGlobs;
    }

    public int getLowestEphemeralPort()
    {
        return lowestEphemeralPort;
    }

    public int getHighestEphemeralPort()
    {
        return highestEphemeralPort;
    }

    public static final class Builder
    {
        private final Set<Class<?>> testrunnerClasses = newSetFromMap(new IdentityHashMap<Class<?>, Boolean>());
        private final Set<Class<?>> whitelistedClasses = newSetFromMap(new IdentityHashMap<Class<?>, Boolean>());

        private final Set<String> whitelistedHosts = new HashSet<>();
        private final Set<Path> whitelistedPaths = new HashSet<>();

        private final Set<PathMatcher> whitelistedPathGlobs = new HashSet<>();

        private int lowestEphemeralPort = 0;
        private int highestEphemeralPort = 0;

        public Builder addTestrunnerClasses(final Collection<Class<?>> classes)
        {
            testrunnerClasses.addAll(classes);
            return this;
        }

        public Builder addWhitelistedClasses(final Collection<Class<?>> classes)
        {
            whitelistedClasses.addAll(classes);
            return this;
        }

        public Builder addTestrunnerClasses(final Class<?> ... classes)
        {
            return addTestrunnerClasses(Arrays.asList(classes));
        }

        public Builder addWhitelistedClasses(final Class<?> ... classes)
        {
            return addWhitelistedClasses(Arrays.asList(classes));
        }

        public Builder addWhitelistedHosts(final String ... hosts)
        {
            whitelistedHosts.addAll(Arrays.asList(hosts));
            return this;
        }

        public Builder addWhitelistedPaths(final Path ... files)
        {
            whitelistedPaths.addAll(Arrays.asList(files));
            return this;
        }

        public Builder addWhitelistedPathGlobs(final PathMatcher ... globs)
        {
            whitelistedPathGlobs.addAll(Arrays.asList(globs));
            return this;
        }

        public Builder setLowestEphemeralPort(final int lowestEphemeralPort)
        {
            this.lowestEphemeralPort = checkValidPort(lowestEphemeralPort);
            return this;
        }

        public Builder setHighestEphemeralPort(final int highestEphemeralPort)
        {
            this.highestEphemeralPort = checkValidPort(highestEphemeralPort);
            return this;
        }

        public LessIOContext build()
        {
            return new LessIOContext(testrunnerClasses,
                                     whitelistedClasses,
                                     whitelistedHosts,
                                     whitelistedPaths,
                                     whitelistedPathGlobs,
                                     lowestEphemeralPort,
                                     highestEphemeralPort);
        }
    }
}
