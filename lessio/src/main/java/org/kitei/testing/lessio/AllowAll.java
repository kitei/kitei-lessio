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
 * Skip all security checks.
 *
 * This is now what you are looking for. This annotation allows pruning of the security rules
 * applied to any piece of code if you are inside one of the whitelisted classes in the security
 * manager. If you add this annotation to any other class (especially a test class), it will
 * fail the test immediately.
 *
 * This is for internal code such as test rules and runners. Do not use otherwise. Please do
 * not send in pull-requests or bug reports to change this behaviour. It is intentional.
 *
 * @see <a href="https://github.com/kitei/kitei-lessio/wiki/@AllowAll">LessIO Wiki, @AllowAll</a>.
 *
 */
@Retention(RUNTIME)
@Target(TYPE)
public @interface AllowAll
{
}
