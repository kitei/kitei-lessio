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
import static java.util.Collections.newSetFromMap;
import static java.util.Collections.unmodifiableSet;

import java.io.File;
import java.io.FileDescriptor;
import java.net.InetAddress;
import java.security.Permission;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

/**
 * A {@link SecurityManager} for controlling IO access.
 *
 * See <a href="https://github.com/kitei/kitei-lessio/wiki"> for a description
 * of all features.
 *
 * @see {@link AllowDNSResolution}, {@link AllowExternalProcess},
 *      {@link AllowLocalFileAccess}, {@link AllowNetworkAccess},
 *      {@link AllowNetworkListen}, {@link AllowNetworkMulticast} and
 *      {@link AllowAll}.
 */
public class LessIOSecurityManager extends SecurityManager
{
    private static final String JAVA_HOME = System.getProperty("java.home");
    private static final String PATH_SEPARATOR = System.getProperty("path.separator");

    private static final Set<Class<?>> TESTRUNNER_CLASSES;
    private static final Set<Class<?>> WHITELISTED_CLASSES;
    private static final Set<String> LOCAL_HOSTS;
    private static final Set<String> WHITELISTED_FILES;

    private static final String TMP_DIR;
    private static final String JUNIT_TMP_PREFIX;

    private static final boolean SKIP_CHECKS = Boolean.getBoolean("kitei.testing.skip-lessio-checks");

    private static final int LOWEST_EPHEMERAL_PORT = Integer.getInteger("kitei.testing.low-ephemeral-port", 32768);
    private static final int HIGHEST_EPHEMERAL_PORT = Integer.getInteger("kitei.testing.high-ephemeral-port", 61000);

    static {
        final Set<Class<?>> testrunnerClasses = newSetFromMap(new IdentityHashMap<Class<?>, Boolean>());

        // junit
        testrunnerClasses.addAll(safeClassForNames("junit",
            "org.junit.internal.runners.statements.InvokeMethod",
            "org.junit.internal.runners.statements.RunAfters",
            "org.junit.internal.runners.statements.RunBefores",
            "org.junit.rules.TestRule",
            "org.junit.runners.ParentRunner",
            "org.junit.runners.model.FrameworkMethod",
            "org.junit.runners.model.Statement"));

        // testng
        testrunnerClasses.addAll(safeClassForNames("testng",
            "org.testng.TestRunner"));

        TESTRUNNER_CLASSES = unmodifiableSet(testrunnerClasses);

        final Set<Class<?>> whitelistedClasses = newSetFromMap(new IdentityHashMap<Class<?>, Boolean>());
        whitelistedClasses.add(java.lang.ClassLoader.class);
        whitelistedClasses.add(java.net.URLClassLoader.class);

        WHITELISTED_CLASSES = unmodifiableSet(whitelistedClasses);

        final Set<String> localHosts = new HashSet<>();
        localHosts.add("localhost");
        localHosts.add("127.0.0.1");
        localHosts.add("::1");

        LOCAL_HOSTS = unmodifiableSet(localHosts);

        final Set<String> whitelistedFiles = new HashSet<>();
        whitelistedFiles.add("/dev/random");
        whitelistedFiles.add("/dev/urandom");

        WHITELISTED_FILES = unmodifiableSet(whitelistedFiles);

        TMP_DIR = System.getProperty("java.io.tmpdir", "/tmp").replaceFirst("/$", "");
        JUNIT_TMP_PREFIX = new File(TMP_DIR, "/junit").getAbsolutePath();
    }

    private static Collection<Class<?>> safeClassForNames(final String group, String ... names)
    {
        final Set<Class<?>> classes = newSetFromMap(new IdentityHashMap<Class<?>, Boolean>());

        for (String name : names) {
            try {
                Class<?> clazz = Class.forName(name);
                classes.add(clazz);
            }
            catch (ClassNotFoundException cnfe) {
                return Collections.emptySet();
            }
        }
        System.err.printf("%s: Ready to instrument %s tests.%n", LessIOSecurityManager.class.getSimpleName(), group);
        return classes;
    }

    private static boolean isValidEphemeralPort(int port)
    {
        return (port == 0  || (port >= LOWEST_EPHEMERAL_PORT && port <= HIGHEST_EPHEMERAL_PORT));
    }

    // Updated at SecurityManager init and again at every ClassLoader init.
    private final AtomicReference<List<String>> classpathParts = new AtomicReference<List<String>>(getClassPath());

    /**
     * Any subclasses that override this method <b>must</b> include any Class<?>
     * elements returned by {@link LessIOSecurityManager#getWhitelistedClasses()}.
     * The recommended pattern is:
     * <blockquote><pre>
     * {@code
    private final Set<Class<?>> whitelistedClasses = ImmutableSet.<Class<?>>builder()
                                                      .addAll(parentWhitelistedClasses)
                                                      .add(javax.crypto.Cipher.class)
                                                      .add(javax.xml.xpath.XPathFactory.class)
                                                      .build();
    protected Set<Class<?>> getWhitelistedClasses() { return whitelistedClasses; }
    }
    </pre></blockquote>
     */
    protected Set<Class<?>> getWhitelistedClasses()
    {
        return WHITELISTED_CLASSES;
    }

    private List<String> getClassPath()
    {
        return Collections.unmodifiableList(Arrays.asList(System.getProperty("java.class.path", "").split(PATH_SEPARATOR)));
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private static boolean hasAnnotations(final Class<?> clazz, final Class<?> ... annotations)
    {
        if (clazz == null) {
            throw new IllegalArgumentException("clazz argument can not be null!");
        }

        if (annotations == null || annotations.length == 0) {
            throw new IllegalArgumentException("at least one annotation must be present");
        }

        // Check class and parent classes.
        Class<?> currentClazz = clazz;
        while (currentClazz != null) {
            Class<?> enclosingClass = currentClazz.getEnclosingClass();

            for (final Class annotation : annotations) {
                if (currentClazz.getAnnotation(annotation) != null) {
                    return true;
                }
                while (enclosingClass != null) {
                    if (enclosingClass.getAnnotation(annotation) != null) {
                        return true;
                    }
                    enclosingClass = enclosingClass.getEnclosingClass();
                }
            }
            currentClazz = currentClazz.getSuperclass();
        }

        return false;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private static <T> T findAnnotation(final Class<?> clazz, final Class<T> annotation)
    {
        if (clazz == null) {
            throw new IllegalArgumentException("clazz argument can not be null!");
        }
        if (annotation == null) {
            throw new IllegalArgumentException("at least one annotation must be present");
        }

        // Check class and parent classes.
        Class<?> currentClazz = clazz;
        while (currentClazz != null) {
            T a = (T) currentClazz.getAnnotation((Class) annotation);
            if (a != null) {
                return a;
            }

            Class<?> enclosingClass = currentClazz.getEnclosingClass();

            while (enclosingClass != null) {
                a = (T) enclosingClass.getAnnotation((Class) annotation);
                if (a != null) {
                    return a;
                }
                enclosingClass = enclosingClass.getEnclosingClass();
            }

            currentClazz = currentClazz.getSuperclass();
        }

        return null;

    }

    private static boolean isTestrunnerClass(final Class<?> clazz)
    {
        Class<?> currentClazz = clazz;

        while (currentClazz != null) {
            if (TESTRUNNER_CLASSES.contains(currentClazz)) {
                return true;
            }

            Class<?> enclosingClass = currentClazz.getEnclosingClass();
            while (enclosingClass != null) {
                if (isTestrunnerClass(enclosingClass)) {
                    return true;
                }

                final Class<?>[] interfaces = enclosingClass.getInterfaces();
                for (final Class<?> interfaceClass : interfaces) {
                    if (isTestrunnerClass(interfaceClass)) {
                        return true;
                    }
                }

                enclosingClass = enclosingClass.getEnclosingClass();
            }

            // Some of the test runner classes (e.g. TestRule) are actual interfaces.
            // so also check the implemented interfaces.
            final Class<?>[] interfaces = currentClazz.getInterfaces();
            for (final Class<?> interfaceClass : interfaces) {
                if (isTestrunnerClass(interfaceClass)) {
                    return true;
                }
            }

            currentClazz = currentClazz.getSuperclass();
        }
        return false;
    }

    // Allowed only via {@link @AllowNetworkAccess}, {@link @AllowNetworkListen},
    // {@link @AllowDNSResolution}, or {@link @AllowNetworkMulticast})
    private void checkDNSResolution(final String host) throws CantDoItException
    {
        if (LOCAL_HOSTS.contains(host.toLowerCase(Locale.ENGLISH))) {
            return; // Always allow localhost
        }

        final Class<?>[] classContext = getClassContext();

        if (traceWithoutExplicitlyAllowedClass(classContext)) {
            checkClassContextPermissions(classContext, new Predicate<Class<?>>() {
                @Override
                public boolean check(final Class<?> input)
                {
                    return hasAnnotations(input, AllowDNSResolution.class, AllowNetworkMulticast.class, AllowNetworkListen.class, AllowNetworkAccess.class);
                }

                @Override
                public String toString()
                {
                    return "@AllowDNSResolution permission";
                }
            });
        }
    }

    private void checkNetworkEndpoint(final String host, final int port, final String description) throws CantDoItException
    {
        final Class<?>[] classContext = getClassContext();

        if (traceWithoutExplicitlyAllowedClass(classContext)) {
            checkClassContextPermissions(classContext, new Predicate<Class<?>>() {
                @Override
                public boolean check(final Class<?> input)
                {
                    String[] endpoints = null;
                    final AllowNetworkAccess access = findAnnotation(input, AllowNetworkAccess.class);
                    if (access != null) {
                        endpoints = access.endpoints();
                    }
                    if (endpoints == null) {
                        return false;
                    }

                    for (final String endpoint : endpoints) {
                        final String[] parts = endpoint.split(":");
                        final String portAsString = Integer.toString(port);

                        boolean hostMatches = parts[0].equals("*") || parts[0].equals(host);
                        boolean portMatches = parts[1].equals("*") || parts[1].equals(portAsString)
                                        || (parts[1].equals("0") && isValidEphemeralPort(port));

                        if (hostMatches && portMatches) {
                            return true;
                        }
                    }
                    return false;
                }

                @Override
                public String toString()
                {
                    return format("@AllowNetworkAccess permission for %s:%d (%s)", host, port, description);
                }
            });
        }
    }

    @Override
    public void checkAccept(final String host, final int port) throws CantDoItException
    {
        if (!SKIP_CHECKS) {
            checkNetworkEndpoint(host, port, "accept");
        }
    }

    @Override
    public void checkConnect(final String host, final int port, final Object context) throws CantDoItException
    {
        checkConnect(host, port);
    }

    @Override
    public void checkConnect(final String host, final int port) throws CantDoItException
    {
        if (host == null) {
            throw new NullPointerException("host is null");
        }

        if (SKIP_CHECKS) {
            return;
        }

        if (port == -1) {
            checkDNSResolution(host);
        }
        else {
            checkNetworkEndpoint(host, port, "connect");
        }
    }

    @Override
    public void checkListen(final int port) throws CantDoItException
    {
        if (SKIP_CHECKS) {
            return;
        }

        final Class<?>[] classContext = getClassContext();
        if (traceWithoutExplicitlyAllowedClass(classContext)) {
            checkClassContextPermissions(classContext, new Predicate<Class<?>>() {
                @Override
                public boolean check(final Class<?> input)
                {
                    int[] ports = null;
                    final AllowNetworkListen a = findAnnotation(input, AllowNetworkListen.class);
                    if (a != null) {
                        ports = a.ports();
                    }
                    if (ports == null) {
                        return false;
                    }

                    for (int p : ports) {
                        if (p == 0 && isValidEphemeralPort(port)) {
                            return true;
                        }
                        else if (p == port) {
                            return true;
                        }
                    }
                    return false;
                }

                @Override
                public String toString()
                {
                    return format("@AllowNetworkListen permission for port %d", port);
                }
            });
        }
    }

    @Override
    public void checkMulticast(final InetAddress maddr) throws CantDoItException
    {
        if (SKIP_CHECKS) {
            return;
        }

        final Class<?>[] classContext = getClassContext();
        if (traceWithoutExplicitlyAllowedClass(classContext)) {
            checkClassContextPermissions(classContext, new Predicate<Class<?>>() {
                @Override
                public boolean check(final Class<?> input)
                {
                    return hasAnnotations(input, AllowNetworkMulticast.class);
                }

                @Override
                public String toString()
                {
                    return "@AllowNetworkMulticast permission";
                }
            });
        }
    }

    @Override
    public void checkMulticast(final InetAddress maddr, final byte ttl) throws CantDoItException
    {
        checkMulticast(maddr);
    }

    // }}

    // {{ Allowed only via {@link @AllowLocalFileAccess}
    private void checkFileAccess(final String file, final String description) throws CantDoItException
    {
        final Class<?>[] classContext = getClassContext();
        if (traceWithoutExplicitlyAllowedClass(classContext)) {
            if (file.startsWith(JAVA_HOME)) {
                // Files in JAVA_HOME are always allowed
                return;
            }

            if (WHITELISTED_FILES.contains(file)) {
                return;
            }

            // Ant's JUnit task writes to /tmp/junitXXX
            if (file.startsWith(JUNIT_TMP_PREFIX)) {
                return;
            }

            /*
             * Although this is an expensive operation, it needs to be here, in a
             * suboptimal location to avoid ClassCircularityErrors that can occur when
             * attempting to load an anonymous class.
             */
            for (final String part : classpathParts.get()) {
                if (file.startsWith(part)) {
                    // Files in the CLASSPATH are always allowed
                    return;
                }
            }

            checkClassContextPermissions(classContext, new Predicate<Class<?>>() {
                @Override
                public boolean check(final Class<?> input)
                {
                    String[] paths = null;
                    final AllowLocalFileAccess a = findAnnotation(input, AllowLocalFileAccess.class);

                    if (a != null) {
                        paths = a.paths();
                    }
                    if (paths == null) {
                        return false;
                    }

                    for (final String p : paths) {
                        if (p.equals("*")
                            || p.equals(file)
                            || p.contains("%TMP_DIR%") && file.startsWith(p.replaceAll("%TMP_DIR%", TMP_DIR))
                            || p.startsWith("*") && p.endsWith("*") && file.contains(p.split("\\*")[1])
                            || p.startsWith("*") && file.endsWith(p.replaceFirst("^\\*", ""))
                            || p.endsWith("*") && file.startsWith(p.replaceFirst("\\*$", ""))) {
                            return true;
                        }
                    }
                    return false;
                }

                @Override
                public String toString()
                {
                    return format("@AllowLocalFileAccess for %s (%s)", file, description);
                }
            });
        }
    }

    private void checkFileDescriptorAccess(final FileDescriptor fd,
                                           final String description) throws CantDoItException
    {
        final Class<?>[] classContext = getClassContext();
        if (traceWithoutExplicitlyAllowedClass(classContext)) {
            checkClassContextPermissions(classContext, new Predicate<Class<?>>() {
                @Override
                public boolean check(final Class<?> input)
                {
                    // AllowExternalProcess and AllowNetworkAccess imply @AllowLocalFileAccess({"%FD%"}),
                    // since it's required.
                    if (hasAnnotations(input, AllowExternalProcess.class, AllowNetworkAccess.class)) {
                        return true;
                    }

                    String[] paths = null;
                    final AllowLocalFileAccess a = findAnnotation(input, AllowLocalFileAccess.class);

                    if (a != null) {
                        paths = a.paths();
                    }

                    if (paths == null) {
                        return false;
                    }

                    for (final String p : paths) {
                        if (p.equals("%FD%")) {
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
            });
        }
    }

    @Override
    public void checkRead(final String file, final Object context)
    {
        checkRead(file);
    }

    @Override
    public void checkRead(final String file)
    {
        if (!SKIP_CHECKS) {
            checkFileAccess(file, "read");
        }
    }

    @Override
    public void checkRead(final FileDescriptor fd)
    {
        if (!SKIP_CHECKS) {
            checkFileDescriptorAccess(fd, "read");
        }
    }

    @Override
    public void checkDelete(final String file)
    {
        if (!SKIP_CHECKS) {
            checkFileAccess(file, "delete");
        }
    }

    @Override
    public void checkWrite(final FileDescriptor fd)
    {
        if (!SKIP_CHECKS) {
            checkFileDescriptorAccess(fd, "write");
        }
    }

    @Override
    public void checkWrite(final String file)
    {
        if (!SKIP_CHECKS) {
            checkFileAccess(file, "write");
        }
    }

    @Override
    public void checkExec(final String cmd) throws CantDoItException
    {
        if (SKIP_CHECKS) {
            return;
        }

        final Class<?>[] classContext = getClassContext();
        if (traceWithoutExplicitlyAllowedClass(classContext)) {
            checkClassContextPermissions(classContext, new Predicate<Class<?>>() {
                @Override
                public boolean check(final Class<?> input)
                {
                    return hasAnnotations(input, AllowExternalProcess.class);
                }

                @Override
                public String toString()
                {
                    return format("@AllowExternalProcess for %s (exec)", cmd);
                }
            });
        }
    }

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

    @Override
    public void checkCreateClassLoader()
    {
        // This is reset on classloader creation in case the classpath has changed.
        // In particular, Maven's Surefire booter changes the classpath after the security
        // manager has been initialized.
        classpathParts.set(getClassPath());
    }

    private boolean isClassWhitelisted(final Class<?> clazz)
    {
        if (getWhitelistedClasses().contains(clazz)) {
            return true;
        }

        // Inner/Nested classes are whitelisted by their enclosing classes.
        Class<?> enclosingClass = clazz.getEnclosingClass();
        while (enclosingClass != null) {
            if (isClassWhitelisted(enclosingClass)) {
                return true;
            }
            enclosingClass = enclosingClass.getEnclosingClass();
        }

        return false;
    }

    /**
     * check whether a class is whitelisted or explicitly allowed to
     * execute an operation.
     */
    private boolean traceWithoutExplicitlyAllowedClass(final Class<?>[] classContext)
    {
        // whitelisted classes.
        for (final Class<?> clazz : classContext) {
            if (isClassWhitelisted(clazz)) {
                return false;
            }

            // Any class marked as a test runner can be annotated to allow everything.
            if (isTestrunnerClass(clazz) && hasAnnotations(clazz, AllowAll.class)) {
                return false;
            }
        }
        return true;
    }

    private void checkClassContextPermissions(final Class<?>[] classContext,
                                              final Predicate<Class<?>> classAuthorized) throws CantDoItException
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
                throw new CantDoItException("Found @AllowAll on a non-testrunner class (%s), refusing to run test!", clazz.getName());
            }

            // Look whether any class in the stack is properly authorized to run the
            // operation.
            if (classAuthorized.check(clazz)) {
                return;
            }
        }

        if (!encounteredTestMethodRunner) {
            return;
        }

        // No class on the stack trace is properly authorized, throw an exception.
        throw new CantDoItException("No class in the class context satisfies %s", classAuthorized);
    }

    public static class CantDoItException extends RuntimeException
    {
        private static final long serialVersionUID = 1L;

        public CantDoItException(final String fmt, final Object ... args)
        {
            super(format(fmt, args));
        }
    }

    public interface Predicate<T>
    {
        boolean check(T input);
    }
}
