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

import static org.kitei.testing.lessio.LessIOUtils.checkNotNull;
import static org.kitei.testing.lessio.LessIOUtils.createGlobMatcher;
import static org.kitei.testing.lessio.LessIOUtils.findAnnotation;
import static org.kitei.testing.lessio.LessIOUtils.hasAnnotations;

import java.io.FileDescriptor;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

class LessIOFilesystemDelegate
{
    private final LessIOContext context;
    private final AtomicReference<List<String>> classpathHolder;

    private final Map<Path, Boolean> whitelistCache = new ConcurrentHashMap<>();

    private final AtomicBoolean tmpDirWarningFlag = new AtomicBoolean();
    private final AtomicBoolean fileDescriptorWarningFlag = new AtomicBoolean();

    LessIOFilesystemDelegate(final LessIOContext context, final AtomicReference<List<String>> classpathHolder)
    {
        this.context = context;
        this.classpathHolder = classpathHolder;

        for (final Path whitelistedPath : context.getWhitelistedPaths()) {
            whitelistCache.put(whitelistedPath, Boolean.TRUE);
        }
    }

    private void tmpDirWarning()
    {
        if (tmpDirWarningFlag.compareAndSet(false, true)) {
            System.err.println("*************************************************************");
            System.err.println("*                                                           *");
            System.err.println("* Usage of %TMP_DIR% is deprecated, use @AllowTmpDirAccess! *");
            System.err.println("*                                                           *");
            System.err.println("*************************************************************");
        }
    }

    private void fileDescriptorWarning()
    {
        if (fileDescriptorWarningFlag.compareAndSet(false, true)) {
            System.err.println("************************************************************");
            System.err.println("*                                                          *");
            System.err.println("* Usage of %FD% is deprecated, use @AllowFileDescriptorIO! *");
            System.err.println("*                                                          *");
            System.err.println("************************************************************");
        }
    }

    boolean checkFilesystemAccess(final Path path)
        throws IOException
    {
        final Boolean result = whitelistCache.get(path);
        if (result != null) {
            return result;
        }

        for (final Path whitelistedPath : context.getWhitelistedPaths()) {
            // Files.isSameFile accesses the file system so it can not be used here.
            if (whitelistedPath.equals(path)) {
                whitelistCache.put(path, Boolean.TRUE);
                return true;
            }
        }

        for (final PathMatcher whitelistedPathGlob : context.getWhitelistedPathGlobs()) {
            if (whitelistedPathGlob.matches(path)) {
                whitelistCache.put(path, Boolean.TRUE);
                return true;
            }
        }

        /*
         * Although this is an expensive operation, it needs to be here, in a
         * suboptimal location to avoid ClassCircularityErrors that can occur when
         * attempting to load an anonymous class.
         */
        for (final String classpathReference : classpathHolder.get()) {
            if (path.toString().startsWith(classpathReference)) {
                // Files on the CLASSPATH are always allowed.
                whitelistCache.put(path, Boolean.TRUE);
                return true;
            }
        }

        return false;
    }

    LessIOPredicate getFileAccessPredicate(final Path path, final String description)
    {
        return new FilesystemFileAccessPredicate(path, description);
    }

    LessIOPredicate getFileDescriptorPredicate(final FileDescriptor fd, final String description)
    {
        return new FilesystemFileDescriptorPredicate(fd, description);
    }

    private class FilesystemFileAccessPredicate implements LessIOPredicate
    {
        private final Path path;
        private final String description;

        private final boolean tmpFile;

        private FilesystemFileAccessPredicate(final Path path, final String description)
        {
            this.path = path;
            this.description = description;
            this.tmpFile = path.startsWith(LessIOUtils.TMP_PATH);
        }

        @Override
        public boolean check(final Class<?> clazz)
            throws Exception
        {
            if (tmpFile && hasAnnotations(clazz, AllowTmpDirAccess.class)) {
                return true;
            }

            final AllowLocalFileAccess annotation = LessIOUtils.findAnnotation(clazz, AllowLocalFileAccess.class);
            if (annotation == null) {
                return false;
            }

            final String[] paths = annotation.paths();
            if (paths == null) {
                return false;
            }

            for (final String p : paths) {
                if (p.equals("*")) {
                    return true;
                }

                PathMatcher tmpMatcher = null;

                // not here checked.
                if (p.equals("%FD%")) {
                    fileDescriptorWarning();
                    continue;
                }

                if (p.equals("%TMP_DIR%")) {
                    tmpDirWarning();
                    if (tmpFile) {
                        return true;
                    }
                }
                else if (p.startsWith("%TMP_DIR%/")) {
                    tmpDirWarning();
                    tmpMatcher = createGlobMatcher(LessIOUtils.TMP_PATH.resolve(p.substring(10)));
                }

                if (tmpMatcher != null) {
                    if (tmpMatcher.matches(path)) {
                        return true;
                    }
                    else {
                        // next check
                        continue;
                    }
                }

                final Path annotationPath = Paths.get(p);

                // Files.isSameFile accesses the file system so it can not be used here.
                if (annotationPath.equals(path)) {
                    return true;
                }

                final PathMatcher matcher = createGlobMatcher(annotationPath);
                if (matcher.matches(path)) {
                    return true;
                }
            }

            return false;
        }

        @Override
        public String toString()
        {
            return format("@AllowLocalFileAccess for %s (%s)", path, description);
        }
    }

    private class FilesystemFileDescriptorPredicate implements LessIOPredicate
    {
        private final String description;
        private final FileDescriptor fd;

        private FilesystemFileDescriptorPredicate(final FileDescriptor fd, final String description)
        {
            this.fd = checkNotNull(fd, "fd is null");
            this.description = description;
        }

        @Override
        public boolean check(final Class<?> clazz)
            throws Exception
        {

            // AllowExternalProcess and AllowNetworkAccess imply @AllowLocalFileDescriptorIO
            // since it's required.
            if (hasAnnotations(clazz, AllowFileDescriptorIO.class, AllowExternalProcess.class, AllowNetworkAccess.class)) {
                return true;
            }

            final AllowLocalFileAccess annotation = findAnnotation(clazz, AllowLocalFileAccess.class);
            if (annotation == null) {
                return false;
            }

            final String[] paths = annotation.paths();
            if (paths == null) {
                return false;
            }

            for (final String p : paths) {
                if (p.equals("%FD%")) {
                    fileDescriptorWarning();
                    return true;
                }
            }
            return false;
        }

        @Override
        public String toString()
        {
            return format("@AllowLocalFileAccess for FileDescriptor(%s) (%s)", fd, description);
        }
    }
}
