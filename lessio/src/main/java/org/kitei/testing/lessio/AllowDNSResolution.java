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

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Allow DNS resolution while running under the regime of the {@link LessIOSecurityManager}.
 *
 * Only classes annotated with this annotation may use the DNS system to resolve hostnames or IP
 * addresses.
 *
 * Annotating a class with {@link AllowNetworkMulticast},
 * {@link AllowNetworkListen}, or {@link AllowNetworkAccess} includes this permission.
 *
 * @see <a href="https://github.com/kitei/kitei-lessio/wiki/@AllowDNSResolution">LessIO Wiki, @AllowDNSResolution</a>.
 */
@Retention(RUNTIME)
@Target(TYPE)
public @interface AllowDNSResolution
{
}
