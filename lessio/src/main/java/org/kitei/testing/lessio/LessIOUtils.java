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
import static java.util.Collections.unmodifiableList;

import java.lang.annotation.Annotation;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class LessIOUtils
{
    private static final String PATH_SEPARATOR = System.getProperty("path.separator");

    static final Path TMP_PATH = Paths.get(System.getProperty("java.io.tmpdir", "/tmp"));

    static int checkValidPort(final int port)
    {
        if (port < 1 || port > 65535) {
            throw new IllegalArgumentException(format("%d is not a valid port value", port));
        }

        return port;
    }

    static <T> T checkNotNull(final T value, final String msg)
    {
        if (value == null) {
            throw new NullPointerException(msg);
        }

        return value;
    }

    static Collection<Class<?>> safeClassForNames(final boolean allOrNothing, final String ... names)
    {
        final Set<Class<?>> classes = newSetFromMap(new IdentityHashMap<Class<?>, Boolean>());

        for (final String name : names) {
            try {
                final Class<?> clazz = Class.forName(name);
                classes.add(clazz);
            }
            catch (final ClassNotFoundException cnfe) {
                if (allOrNothing) {
                    return Collections.emptySet();
                }
            }
        }
        return classes;
    }

    static PathMatcher createGlobMatcher(final Path path)
    {
        checkNotNull(path, "path is null");
        return FileSystems.getDefault().getPathMatcher("glob:" + path.toString());
    }

    static List<String> getCurrentClassPath()
    {
        return unmodifiableList(Arrays.asList(System.getProperty("java.class.path", "").split(PATH_SEPARATOR)));
    }

    /**
     * Determines whether a given class, an interface implemented by that class or
     * any enclosing class is in the whitelist cache. If yes, that class is added to
     * the cache as well. The cache must be seeded from the outside, with an empty cache this
     * method will always return false;
     */
    static boolean matchesWhitelistCache(final Class<?> clazz, final Map<Class<?>, Boolean> clazzCache)
    {
        Boolean result = null;
        Class<?> currentClazz = clazz;

        lookForClass:
        while (result == null && currentClazz != null) {
            result = clazzCache.get(currentClazz);
            if (result != null) {
                if (currentClazz == clazz) {
                    // This is the result of a direct class lookup. So
                    // it is present in the cache and the method can exit
                    // directly
                    return result;
                }
                else {
                    break lookForClass;
                }
            }

            // Check enclosing classes for the current class first.
            Class<?> enclosingClass = currentClazz.getEnclosingClass();
            while (enclosingClass != null) {
                if (matchesWhitelistCache(enclosingClass, clazzCache)) {
                    result = Boolean.TRUE;
                    break lookForClass;
                }

                enclosingClass = enclosingClass.getEnclosingClass();
            }

            // Also look at the interfaces that a class implements.
            // Some of the test runner classes (e.g. TestRule) are actual interfaces.
            final Class<?>[] interfaces = currentClazz.getInterfaces();
            for (final Class<?> interfaceClass : interfaces) {
                if (matchesWhitelistCache(interfaceClass, clazzCache)) {
                    result = Boolean.TRUE;
                    break lookForClass;
                }
            }

            currentClazz = currentClazz.getSuperclass();
        }

        if (result == null) {
            result = Boolean.FALSE;
        }

        clazzCache.put(clazz, result);
        return result;
    }

    /**
     * Check whether a given class has one of the annotations listed. Annotations
     * must be either on the class itself, on a superclass or any enclosing class.
     */
    @SafeVarargs
    static boolean hasAnnotations(final Class<?> clazz, final Class<? extends Annotation> ... annotations)
    {
        checkNotNull(clazz, "clazz is null");

        if (annotations.length == 0) {
            throw new IllegalArgumentException("at least one annotation must be present");
        }

        // Check class and parent classes.
        Class<?> currentClazz = clazz;
        while (currentClazz != null) {
            Class<?> enclosingClass = currentClazz.getEnclosingClass();

            for (final Class<? extends Annotation> annotation : annotations) {
                if (currentClazz.getAnnotation(annotation) != null) {
                    return true;
                }

                while (enclosingClass != null) {
                    if (hasAnnotations(enclosingClass, annotations)) {
                        return true;
                    }
                    enclosingClass = enclosingClass.getEnclosingClass();
                }

                final Class<?>[] interfaces = currentClazz.getInterfaces();
                for (final Class<?> interfaceClass : interfaces) {
                    if (hasAnnotations(interfaceClass, annotations)) {
                        return true;
                    }
                }
            }

            currentClazz = currentClazz.getSuperclass();
        }

        return false;
    }

    static <T extends Annotation> T findAnnotation(final Class<?> clazz, final Class<T> annotation)
    {
        checkNotNull(clazz, "clazz is null");
        checkNotNull(annotation, "annotation is null");

        // Check class and parent classes.
        Class<?> currentClazz = clazz;
        while (currentClazz != null) {
            T a = currentClazz.getAnnotation(annotation);
            if (a != null) {
                return a;
            }

            Class<?> enclosingClass = currentClazz.getEnclosingClass();

            while (enclosingClass != null) {
                a = findAnnotation(enclosingClass, annotation);
                if (a != null) {
                    return a;
                }
                enclosingClass = enclosingClass.getEnclosingClass();
            }

            final Class<?>[] interfaces = currentClazz.getInterfaces();
            for (final Class<?> interfaceClass : interfaces) {
                a = findAnnotation(interfaceClass, annotation);
                if (a != null) {
                    return a;
                }
            }

            currentClazz = currentClazz.getSuperclass();
        }

        return null;
    }
}
