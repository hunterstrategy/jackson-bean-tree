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


import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import net.hunterstrategy.beantree.FileBeans.ChildBean;

public final class InvalidDirBeans {
    private InvalidDirBeans() {}

    public static class MapDoesNotDeclareString {
        String name;

        @BeanCollection(value = "child")
        Map<Object, ChildBean> map;
    }

    public static class CannotDeriveGenericType {
        String name;

        @BeanCollection(value = "child")
        Map<String, ? extends ChildBean> map;
    }

    public static class TemplateTypeMismatch {
        String name;

        @Template("map")
        Object defaultChildBean;

        @BeanCollection("child")
        Map<String, ChildBean> map;
    }

    public static class ContainsGlob {
        String name;

        @BeanCollection("*?")
        Map<String, ChildBean> map;
    }

    public static class IsAbsolute {
        String name;

        @BeanCollection("/tmp/foo")
        Map<String, ChildBean> map;
    }

    public static class IsRelative {
        String name;

        @BeanCollection("../foo")
        Map<String, ChildBean> map;
    }

    public static class BadType {
        String name;

        @BeanCollection("conf.d")
        String foo;
    }

    public static class UnknownTypes {
        String name;

        @SuppressWarnings("rawtypes")
        @BeanCollection("conf.d")
        Map map;
    }

    public static class ToList {
        String name;

        @BeanCollection("conf.d")
        List<ChildBean> beans;
    }

    public static class ToSet {
        String name;

        @BeanCollection("conf.d")
        Set<ChildBean> beans;
    }

    public static class ToQueue {
        String name;

        @BeanCollection("conf.d")
        Queue<ChildBean> beans;
    }

    public static class CustomCollectionType {
        String name;

        @BeanCollection("conf.d")
        Set<ChildBean> beans = new LinkedHashSet<>();
    }
}
