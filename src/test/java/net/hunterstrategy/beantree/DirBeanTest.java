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


import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.stream.Collectors;
import net.hunterstrategy.beantree.DirBeans.BasicDir;
import net.hunterstrategy.beantree.DirBeans.BasicDirFileTemplate;
import net.hunterstrategy.beantree.DirBeans.BasicDirTemplate;
import net.hunterstrategy.beantree.DirBeans.BasicSubdirs;
import net.hunterstrategy.beantree.DirBeans.MemberIsImplicitName;
import net.hunterstrategy.beantree.FileBeans.ChildBean;
import net.hunterstrategy.beantree.InvalidDirBeans.BadType;
import net.hunterstrategy.beantree.InvalidDirBeans.CannotDeriveGenericType;
import net.hunterstrategy.beantree.InvalidDirBeans.ContainsGlob;
import net.hunterstrategy.beantree.InvalidDirBeans.CustomCollectionType;
import net.hunterstrategy.beantree.InvalidDirBeans.IsAbsolute;
import net.hunterstrategy.beantree.InvalidDirBeans.IsRelative;
import net.hunterstrategy.beantree.InvalidDirBeans.MapDoesNotDeclareString;
import net.hunterstrategy.beantree.InvalidDirBeans.TemplateTypeMismatch;
import net.hunterstrategy.beantree.InvalidDirBeans.ToList;
import net.hunterstrategy.beantree.InvalidDirBeans.ToQueue;
import net.hunterstrategy.beantree.InvalidDirBeans.ToSet;
import net.hunterstrategy.beantree.InvalidDirBeans.UnknownTypes;
import net.hunterstrategy.beantree.analysis.BeanTreeException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

@DisplayName("Integration test: BeanCollection (directories of files)")
public class DirBeanTest implements FunctionalTestSupport {
    ConfigurationTreeBuilder builder = new ConfigurationTreeBuilder();

    private void assert_basic_map(Map<String, ChildBean> map) {
        Assertions.assertNotNull(map);
        Assertions.assertEquals(2, map.size());
        Assertions.assertTrue(map.containsKey("foo"));
        Assertions.assertTrue(map.containsKey("bar"));

        ChildBean bean = map.get("foo");
        assert_childbean("foo_value", 12, bean);

        bean = map.get("bar");
        assert_childbean("bar_value", 35, bean);
    }

    private void assert_childbean(String foo, int bar, ChildBean actual) {
        Assertions.assertNotNull(actual);
        Assertions.assertEquals(foo, actual.childFoo);
        Assertions.assertEquals(bar, actual.childBar);
    }

    @Test
    @DisplayName("Basic happy-path for using subdir strategy (*/bean.json)")
    void basic_subdirs(TestInfo info) {
        Path p = resource(info, "entry.json");
        BasicSubdirs bs = builder.build(BasicSubdirs.class, p);
        Assertions.assertNotNull(bs);
        Assertions.assertEquals("entry", bs.name);
        assert_basic_map(bs.beans);
    }

    @Test
    @DisplayName("Basic happy-path for using single-dir strategy (dir/*.json)")
    void basic_dir(TestInfo info) {
        Path p = resource(info, "entry.json");
        BasicDir bd = builder.build(BasicDir.class, p);
        Assertions.assertNotNull(bd);
        Assertions.assertEquals("entry", bd.name);
        assert_basic_map(bd.beans);
    }

    @Test
    @DisplayName("Basic test with List")
    void basic_dir_list() {
        Path p = resource("basic_dir", "entry.json");
        ToList listbean = builder.build(ToList.class, p);
        Map<String, ChildBean> beans = listbean.beans.stream().collect(Collectors.toMap(cb -> cb.beanName, cb -> cb));
        assert_basic_map(beans);
    }

    @Test
    @DisplayName("Basic test with Set")
    void basic_dir_set() {
        Path p = resource("basic_dir", "entry.json");
        ToSet listbean = builder.build(ToSet.class, p);
        Map<String, ChildBean> beans = listbean.beans.stream().collect(Collectors.toMap(cb -> cb.beanName, cb -> cb));
        assert_basic_map(beans);
    }

    @Test
    @DisplayName("Custom collection types preserved if specified")
    void custom_collection_types() {
        Path p = resource("basic_dir", "entry.json");
        CustomCollectionType bean = builder.build(CustomCollectionType.class, p);
        Assertions.assertTrue(bean.beans instanceof LinkedHashSet);
    }

    @Test
    @DisplayName("Basic test with Queue")
    void basic_dir_queue() {
        Path p = resource("basic_dir", "entry.json");
        ToQueue listbean = builder.build(ToQueue.class, p);
        Map<String, ChildBean> beans = listbean.beans.stream().collect(Collectors.toMap(cb -> cb.beanName, cb -> cb));
        assert_basic_map(beans);
    }

    @Test
    @DisplayName("Single-dir strategy with @Template in source file")
    void basic_dir_with_template(TestInfo info) {
        Path p = resource(info, "entry.json");
        BasicDirTemplate bdt = builder.build(BasicDirTemplate.class, p);
        Assertions.assertNotNull(bdt);
        assert_childbean("foo_value", 12, bdt.beans.get("foo"));
        assert_childbean("bar_value", 123, bdt.beans.get("bar"));
    }

    @Test
    @DisplayName("Single-dir strategy with @Template in external file")
    void basic_dir_file_template(TestInfo info) {
        Path p = resource(info, "entry.json");
        BasicDirFileTemplate bdft = builder.build(BasicDirFileTemplate.class, p);
        Assertions.assertNotNull(bdft);
        assert_childbean("foo_value", 12, bdft.beans.get("foo"));
        assert_childbean("bar_value", 456, bdft.beans.get("bar"));
    }

    <T extends Throwable, B> T assert_throws(Class<B> beanType, Class<T> errorType) {
        return assert_throws("basic_dir", "entry.json", beanType, errorType);
    }

    <T extends Throwable, B> T assert_throws(String dir, String file, Class<B> beanType, Class<T> errorType) {
        Path p = resource(dir, file);
        T t = Assertions.assertThrows(errorType, () -> {
            builder.build(beanType, p);
        });
        return t;
    }

    @Test
    @DisplayName("Must use String keys in Maps")
    void maps_use_strings() {
        Throwable t = assert_throws(MapDoesNotDeclareString.class, BeanTreeException.class);
        Assertions.assertTrue(t.getMessage().contains("Maps must have String key."));
    }

    @Test
    @DisplayName("Use member name when value is unspecified")
    void missing_value() {
        Path p = resource("basic_subdirs", "entry.json");
        MemberIsImplicitName bean = builder.build(MemberIsImplicitName.class, p);
        assert_basic_map(bean.child);
    }

    @Test
    @DisplayName("Cannot use glob characters in value")
    void illegal_glob_expression() {
        Throwable t = assert_throws(ContainsGlob.class, BeanTreeException.class);
        Assertions.assertTrue(t.getMessage().contains("Dir/file cannot contain special character: "), t.getMessage());
    }

    @Test
    @DisplayName("Cannot use absolute paths")
    void absolute_path_expression() {
        Throwable t = assert_throws(IsAbsolute.class, BeanTreeException.class);
        Assertions.assertTrue(t.getMessage().contains("Dir/file cannot be an absolute path: "), t.getMessage());
    }

    @Test
    @DisplayName("Cannot use relative paths")
    void relative_path_expression() {
        Throwable t = assert_throws(IsRelative.class, BeanTreeException.class);
        Assertions.assertTrue(t.getMessage().contains("Dir/file name cannot be relativized: "), t.getMessage());
    }

    @Test
    @DisplayName("Must use specific types")
    void bad_type() {
        Throwable t = assert_throws(BadType.class, BeanTreeException.class);
        Assertions.assertTrue(t.getMessage().contains("Target type must be Map, List, Queue, or Set."), t.getMessage());
    }

    @Test
    @DisplayName("Must specify types when generics are not present")
    void cannot_infer_types() {
        Throwable t = assert_throws(UnknownTypes.class, BeanTreeException.class);
        Assertions.assertTrue(t.getMessage().contains("Must specify deserialization target type."), t.getMessage());
    }

    @Test
    @DisplayName("Must specify deserialization type when it cannot be determined (type erasure)")
    void cannot_infer_type_due_to_erasure() {
        Throwable t = assert_throws(CannotDeriveGenericType.class, BeanTreeException.class);
        Assertions.assertTrue(t.getMessage().contains("Must specify deserialization target type."));
    }

    @Test
    @DisplayName("Template types must match the BeanCollection target type")
    void templates_must_be_compatible_with_collection() {
        Path p = resource("basic_dir_with_template", "entry.json");
        Throwable t = Assertions.assertThrows(BeanTreeException.class, () -> {
            builder.build(TemplateTypeMismatch.class, p);
        });
        Assertions.assertTrue(
                t.getMessage().contains("Template of type class java.util.LinkedHashMap is not compatible with class"));
        Assertions.assertTrue(t.getMessage().contains("Template source:"));
        Assertions.assertTrue(t.getMessage().contains("Template annotation:"));
    }
}
