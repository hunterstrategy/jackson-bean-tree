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


import java.util.Map;
import net.hunterstrategy.beantree.BeanCollection.Mapping;
import net.hunterstrategy.beantree.FileBeans.ChildBean;

public final class DirBeans {
    private DirBeans() {}

    public static class BasicSubdirs {
        String name;

        @BeanCollection(value = "child", mapping = Mapping.MULTI_DIRS, type = ChildBean.class)
        Map<String, ChildBean> beans;
    }

    public static class BasicDir {
        String name;

        @BeanCollection(value = "conf.d", mapping = Mapping.CONF_DIR, type = ChildBean.class)
        Map<String, ChildBean> beans;
    }

    public static class BasicDirTemplate {
        String name;

        @Template("beans")
        ChildBean defaultChildBean;

        @BeanCollection("conf.d")
        Map<String, ChildBean> beans;
    }

    public static class BasicDirFileTemplate {
        String name;

        @Template(value = "beans", external = @Bean("bean-defaults"))
        ChildBean defaultChildBean;

        @BeanCollection("conf.d")
        Map<String, ChildBean> beans;
    }

    public static class MemberIsImplicitName {
        String name;

        @BeanCollection(mapping = Mapping.MULTI_DIRS)
        Map<String, ChildBean> child;
    }
}
