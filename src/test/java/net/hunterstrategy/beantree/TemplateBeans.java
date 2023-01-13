/*-
 * #%L
 * Jackson Bean Tree
 * %%
 * Copyright (C) 2022 - 2023 Hunter Strategy LLC
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 *
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * #L%
 */
package net.hunterstrategy.beantree;


import net.hunterstrategy.beantree.FileBeans.ChildBean;

public final class TemplateBeans {
    private TemplateBeans() {}

    public static class Inline {
        @Template("bean")
        ChildBean defaults;

        @Bean
        ChildBean bean;
    }

    public static class External {
        @Template(value = "bean", external = @Bean("bean-defaults"))
        ChildBean defaults;

        @Bean
        ChildBean bean;
    }

    public static class Dependency {
        @Template("dependency")
        ChildBean dependency;

        // requires "dependency" above
        // dependency could also be external, doesn't need to be inline.
        // currently no desire to allow inline templates to express dependencies, as
        // I'm not sure the template dependency feature is worthwhile to explore in depth
        @Template(value = "bean", external = @Bean(value = "bean-defaults", template = "dependency"))
        ChildBean defaults;

        @Bean
        ChildBean bean;
    }
}
