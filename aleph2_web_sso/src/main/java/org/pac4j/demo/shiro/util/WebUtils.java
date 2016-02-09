/*******************************************************************************
 * Copyright 2016, The IKANOW Open Source Project.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package org.pac4j.demo.shiro.util;

import javax.servlet.jsp.PageContext;

import org.apache.shiro.web.env.EnvironmentLoader;
import org.apache.shiro.web.env.IniWebEnvironment;

public class WebUtils {
    
    public static <T extends Object> T getObject(final PageContext pageContext, final Class<T> clazz, final String name) {
        final IniWebEnvironment env = (IniWebEnvironment) pageContext.getServletContext()
            .getAttribute(EnvironmentLoader.ENVIRONMENT_ATTRIBUTE_KEY);
        if (env != null) {
            return env.getObject(name, clazz);
        }
        return null;
    }
}
