/*-
 * #%L
 * Jackson Bean Tree
 * %%
 * Copyright (C) 2022 Hunter Strategy LLC
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

public final class FileBeans {
    private FileBeans() {}

    public static class ParentBean {
        String foo;
        int bar;

        @Bean("child.json")
        ChildBean baz;
    }

    public static class ParentBeanNoExtension {
        String foo;
        int bar;

        @Bean("child")
        ChildBean baz;
    }

    public static class ChildBean {
        @Name
        String beanName;

        String childFoo;
        int childBar;
    }

    public static class ParentWithGrandChild {
        String name;

        @Bean("child")
        ChildBeanWithGrandChild childInstance;
    }

    public static class ChildBeanWithGrandChild {
        @Name
        String beanName;

        String name;

        @Bean("grandchild")
        ChildBean grandchildInstance;
    }

    public static class EmptyFile {
        @Bean
        ChildBean child;
    }
}
