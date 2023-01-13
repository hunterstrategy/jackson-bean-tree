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
import net.hunterstrategy.beantree.FileBeans.ParentWithGrandChild;

public final class InvalidFileBeans {
    private InvalidFileBeans() {}

    public static class CycleBean {
        @Bean("empty")
        CycleBean cycle;
    }

    public static class AbsoluteFile {
        @Bean("/tmp/foo.json")
        ChildBean bean;
    }

    public static class RelativeFile {
        @Bean("../../../etc/nsswitch.conf")
        ChildBean bean;
    }

    public static class BadOverride {
        @Bean(value = "child.json", type = ParentWithGrandChild.class)
        ChildBean bean;
    }

    public static interface InterfaceType {}

    public static class InterfaceTypeImpl implements InterfaceType {
        // whatever
    }

    public static class CannotDeserialize {
        @Bean("child")
        InterfaceType bean;
    }

    public static class InterfaceOkayWithSpecification {
        @Bean(value = "child", type = InterfaceTypeImpl.class)
        InterfaceType bean;
    }

    public static class MissingFile {
        @Bean("missing")
        ChildBean bean;
    }

    public static class IgnoreDirs {
        @Bean("subdir")
        ChildBean bean;
    }
}
