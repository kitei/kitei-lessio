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

import static org.kitei.testing.lessio.LessIOUtils.checkNotNull;
import static org.kitei.testing.lessio.LessIOUtils.createGlobMatcher;
import static org.kitei.testing.lessio.LessIOUtils.getCurrentClassPath;
import static org.kitei.testing.lessio.LessIOUtils.hasAnnotations;
import static org.kitei.testing.lessio.LessIOUtils.matchesWhitelistCache;
import static org.kitei.testing.lessio.LessIOUtils.safeClassForNames;

import java.io.FileDescriptor;
import java.net.InetAddress;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.Permission;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.kitei.testing.lessio.LessIOContext.Builder;

public class LessIOSecurityManager
    extends SecurityManager
{
    private static final boolean SKIP_CHECKS = Boolean.getBoolean("kitei.testing.skip-lessio-checks");

    private final LessIOContext context;

    // Updated at SecurityManager init and again at every ClassLoader init.
    private final AtomicReference<List<String>> classpathHolder = new AtomicReference<List<String>>();

    private final LessIONetworkDelegate networkDelegate;
    private final LessIOFilesystemDelegate filesystemDelegate;
    private final LessIOProcessDelegate processDelegate;

    private final Map<Class<?>, Boolean> whitelistCache = Collections.synchronizedMap(new IdentityHashMap<Class<?>, Boolean>(2048));
    private final Map<Class<?>, Boolean> testrunnerCache = Collections.synchronizedMap(new IdentityHashMap<Class<?>, Boolean>(128));

    public static LessIOContext.Builder defaultContextBuilder()
    {
        final Builder builder = LessIOContext.builder();

        final Collection<Class<?>> junitClasses = safeClassForNames(true,
            "org.junit.internal.runners.statements.InvokeMethod",
            "org.junit.internal.runners.statements.RunAfters",
            "org.junit.internal.runners.statements.RunBefores",
            "org.junit.rules.TestRule",
            "org.junit.runners.ParentRunner",
            "org.junit.runners.model.FrameworkMethod",
            "org.junit.runners.model.Statement");

        if (!junitClasses.isEmpty()) {
            builder.addTestrunnerClasses(junitClasses);

            // JUnit accesses <tmpdir>/junitXXXX files
            builder.addWhitelistedPathGlobs(createGlobMatcher(LessIOUtils.TMP_PATH.resolve("junit*")));

            System.err.println("Ready to instrument junit tests.");
        }

        final Collection<Class<?>> testngClasses = safeClassForNames(true,
            "org.testng.TestRunner");
        if (!testngClasses.isEmpty()) {
            builder.addTestrunnerClasses(testngClasses);
            System.err.println("Ready to instrument TestNG tests.");
        }

        builder.addWhitelistedClasses(
            java.lang.ClassLoader.class,
            java.net.URLClassLoader.class);

        builder.addWhitelistedHosts(
            "localhost",
            "127.0.0.1",
            "::1");

        builder.addWhitelistedPaths(Paths.get("/dev/random"),
            Paths.get("/dev/urandom"));

        // Everything on the java.home path is always accessible.
        builder.addWhitelistedPathGlobs(createGlobMatcher(Paths.get(System.getProperty("java.home")).resolve("**")));

        builder.setLowestEphemeralPort(Integer.getInteger("kitei.testing.low-ephemeral-port", 32768));
        builder.setHighestEphemeralPort(Integer.getInteger("kitei.testing.high-ephemeral-port", 61000));

        return builder;
    }

    public LessIOSecurityManager()
    {
        this(defaultContextBuilder().build());
    }

    protected LessIOSecurityManager(final LessIOContext context)
    {
        this.context = checkNotNull(context, "context is null");
        this.classpathHolder.set(getCurrentClassPath());

        for (final Class<?> whitelistedClass : context.getWhitelistedClasses()) {
            whitelistCache.put(whitelistedClass, Boolean.TRUE);
        }

        for (final Class<?> testrunnerClass : context.getTestrunnerClasses()) {
            testrunnerCache.put(testrunnerClass, Boolean.TRUE);
        }

        this.networkDelegate = new LessIONetworkDelegate(context);
        this.filesystemDelegate = new LessIOFilesystemDelegate(context, classpathHolder);
        this.processDelegate = new LessIOProcessDelegate(context);
    }

    //
    // Network
    //

    @Override
    public void checkAccept(final String host, final int port) throws LessIOException
    {
        final Class<?>[] classContext = getClassContext();
        if (checkImplicitPermissions(classContext)) {
            return;
        }

        checkPredicate(classContext, networkDelegate.getAcceptPredicate(host, port));
    }

    @Override
    public void checkConnect(final String host, final int port, final Object context) throws LessIOException
    {
        checkConnect(host, port);
    }

    @Override
    public void checkConnect(final String host, final int port) throws LessIOException
    {
        final Class<?>[] classContext = getClassContext();
        if (checkImplicitPermissions(classContext)) {
            return;
        }
        checkPredicate(classContext, networkDelegate.getConnectPredicate(host, port));
    }

    @Override
    public void checkListen(final int port) throws LessIOException
    {
        final Class<?>[] classContext = getClassContext();
        if (checkImplicitPermissions(classContext)) {
            return;
        }
        checkPredicate(classContext, networkDelegate.getListenPredicate(port));
    }

    @Override
    public void checkMulticast(final InetAddress maddr, final byte ttl) throws LessIOException
    {
        checkMulticast(maddr);
    }

    @Override
    public void checkMulticast(final InetAddress maddr) throws LessIOException
    {
        final Class<?>[] classContext = getClassContext();
        if (checkImplicitPermissions(classContext)) {
            return;
        }
        checkPredicate(classContext, networkDelegate.getMulticastPredicate(maddr));
    }

    //
    // File system
    //

    @Override
    public void checkRead(final String file, final Object context)
    {
        checkRead(file);
    }

    @Override
    public void checkRead(final String fileName)
    {
        final Class<?>[] classContext = getClassContext();
        if (checkImplicitPermissions(classContext)) {
            return;
        }

        checkNotNull(fileName, "fileName is null");
        final Path path = Paths.get(fileName);

        try {
            if (filesystemDelegate.checkFilesystemAccess(path)) {
                return;
            }
        }
        catch (final Exception e) {
            throw new LessIOException(e, "Exception while accessing %s for read.", fileName);
        }

        checkPredicate(classContext, filesystemDelegate.getFileAccessPredicate(path, "read"));
    }

    @Override
    public void checkRead(final FileDescriptor fd)
    {
        final Class<?>[] classContext = getClassContext();
        if (checkImplicitPermissions(classContext)) {
            return;
        }
        checkPredicate(classContext, filesystemDelegate.getFileDescriptorPredicate(fd, "read"));
    }

    @Override
    public void checkWrite(final FileDescriptor fd)
    {
        final Class<?>[] classContext = getClassContext();
        if (checkImplicitPermissions(classContext)) {
            return;
        }
        checkPredicate(classContext, filesystemDelegate.getFileDescriptorPredicate(fd, "write"));
    }

    @Override
    public void checkWrite(final String fileName)
    {
        final Class<?>[] classContext = getClassContext();
        if (checkImplicitPermissions(classContext)) {
            return;
        }

        checkNotNull(fileName, "fileName is null");
        final Path path = Paths.get(fileName);

        try {
            if (filesystemDelegate.checkFilesystemAccess(path)) {
                return;
            }
        }
        catch (final Exception e) {
            throw new LessIOException(e, "Exception while accessing %s for write.", fileName);
        }

        checkPredicate(classContext, filesystemDelegate.getFileAccessPredicate(path, "write"));
    }

    @Override
    public void checkDelete(final String fileName)
    {
        final Class<?>[] classContext = getClassContext();
        if (checkImplicitPermissions(classContext)) {
            return;
        }

        checkNotNull(fileName, "fileName is null");
        final Path path = Paths.get(fileName);

        try {
            if (filesystemDelegate.checkFilesystemAccess(path)) {
                return;
            }
        }
        catch (final Exception e) {
            throw new LessIOException(e, "Exception while accessing %s for delete.", fileName);
        }

        checkPredicate(classContext, filesystemDelegate.getFileAccessPredicate(path, "delete"));
    }

    //
    // Command execution
    //
    @Override
    public void checkExec(final String cmd) throws LessIOException
    {
        final Class<?>[] classContext = getClassContext();
        if (checkImplicitPermissions(classContext)) {
            return;
        }
        checkPredicate(classContext, processDelegate.getExecuteProcessPredicate(cmd));
    }

    //
    // Class Loader management
    //
    @Override
    public void checkCreateClassLoader()
    {
        // Reset classpath refernce on classloader creation in case the classpath has changed.
        // In particular, Maven's Surefire booter changes the classpath after the security
        // manager has been initialized.
        classpathHolder.set(getCurrentClassPath());
    }

    //
    // Overrides from SecurityManager
    //

    @Override
    public void checkAccess(final Thread t)
    {
    }

    @Override
    public void checkAccess(final ThreadGroup g)
    {
    }

    @Override
    public void checkMemberAccess(final Class<?> clazz, final int which)
    {
    }

    @Override
    public void checkPackageAccess(final String pkg)
    {
    }

    @Override
    public void checkPackageDefinition(final String pkg)
    {
    }

    @Override
    public void checkSetFactory()
    {
    }

    @Override
    public void checkPropertiesAccess()
    {
    }

    @Override
    public void checkPropertyAccess(final String key)
    {
    }

    @Override
    public void checkSecurityAccess(final String target)
    {
    }

    @Override
    public void checkPermission(final Permission perm, final Object context)
    {
    }

    @Override
    public void checkPermission(final Permission perm)
    {
    }

    private boolean checkImplicitPermissions(final Class<?>[] classContext)
    {
        // all tests are skipped.
        if (SKIP_CHECKS) {
            return true;
        }

        // Check all classes in the class context.
        for (final Class<?> clazz : classContext) {
            // Any whitelisted class is accepted.
            if (isWhitelistedClass(clazz)) {
                return true;
            }

            // Any testrunner class that contains the @AllowAll annotation is
            // also accepted
            if (isTestrunnerClass(clazz) && hasAnnotations(clazz, AllowAll.class)) {
                return true;
            }
        }

        return false;
    }

    private boolean isWhitelistedClass(final Class<?> clazz)
    {
        return matchesWhitelistCache(clazz, whitelistCache);
    }

    private boolean isTestrunnerClass(final Class<?> clazz)
    {
        return matchesWhitelistCache(clazz, testrunnerCache);
    }

    private void checkPredicate(final Class<?>[] classContext,
                                final LessIOPredicate predicate) throws LessIOException
    {
        // Only check permissions when we're running in the context of a JUnit test.
        boolean encounteredTestMethodRunner = false;

        for (final Class<?> clazz : classContext) {
            // Check whether any of the classes on the stack is one of the
            // test runner classes.
            if (isTestrunnerClass(clazz)) {
                encounteredTestMethodRunner = true;
            }
            else if (hasAnnotations(clazz, AllowAll.class)) {
                throw new LessIOException("Found @AllowAll on a non-testrunner class (%s), refusing to run test!", clazz.getName());
            }

            // Look whether any class in the stack is properly authorized to run the
            // operation.
            try {
                if (predicate.check(clazz)) {
                    return;
                }
            }
            catch (final Exception e) {
                throw new LessIOException(e, "Exception while processing %s", predicate);
            }
        }

        if (!encounteredTestMethodRunner) {
            return;
        }

        // No class on the stack trace is properly authorized, throw an exception.
        throw new LessIOException("No class in the class context satisfies %s", predicate);
    }
}
