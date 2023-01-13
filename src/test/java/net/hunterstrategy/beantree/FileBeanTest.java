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


import com.fasterxml.jackson.dataformat.toml.TomlMapper;
import java.nio.file.Path;
import net.hunterstrategy.beantree.FileBeans.EmptyFile;
import net.hunterstrategy.beantree.FileBeans.ParentBean;
import net.hunterstrategy.beantree.FileBeans.ParentBeanNoExtension;
import net.hunterstrategy.beantree.FileBeans.ParentWithGrandChild;
import net.hunterstrategy.beantree.InvalidFileBeans.AbsoluteFile;
import net.hunterstrategy.beantree.InvalidFileBeans.BadOverride;
import net.hunterstrategy.beantree.InvalidFileBeans.CannotDeserialize;
import net.hunterstrategy.beantree.InvalidFileBeans.CycleBean;
import net.hunterstrategy.beantree.InvalidFileBeans.IgnoreDirs;
import net.hunterstrategy.beantree.InvalidFileBeans.InterfaceOkayWithSpecification;
import net.hunterstrategy.beantree.InvalidFileBeans.MissingFile;
import net.hunterstrategy.beantree.InvalidFileBeans.RelativeFile;
import net.hunterstrategy.beantree.analysis.BeanTreeException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

@DisplayName("Integration test: Bean (sibling files)")
public class FileBeanTest implements FunctionalTestSupport {
    ConfigurationTreeBuilder builder = new ConfigurationTreeBuilder();

    @Test
    @DisplayName("Basic test of FileBean")
    void sibling_file(TestInfo test) {
        ParentBean bean = builder.build(ParentBean.class, resource(test, "parent.json"));
        Assertions.assertEquals("value", bean.foo);
        Assertions.assertEquals(42, bean.bar);
        Assertions.assertNotNull(bean.baz);
        Assertions.assertEquals(24, bean.baz.childBar);
        Assertions.assertEquals("childValue", bean.baz.childFoo);
    }

    @Test
    @DisplayName("TOML instead of JSON, similar to testing setting defaults")
    void basic_toml(TestInfo test) {
        ConfigurationTreeBuilder builder = new ConfigurationTreeBuilder()
                .mapper(new TomlMapper())
                .defaultMapperVisibilitySettings()
                .defaultExtension(".toml");

        ParentBeanNoExtension bean = builder.build(ParentBeanNoExtension.class, resource(test, "parent.toml"));
        Assertions.assertEquals("value", bean.foo);
        Assertions.assertEquals(42, bean.bar);
        Assertions.assertNotNull(bean.baz);
        Assertions.assertEquals(24, bean.baz.childBar);
        Assertions.assertEquals("childValue", bean.baz.childFoo);
    }

    @Test
    @DisplayName("Same as basic test, but don't specify .json file extensions")
    void sibling_file_no_extension() {
        ParentBeanNoExtension bean =
                builder.build(ParentBeanNoExtension.class, resource("sibling_file", "parent.json"));
        Assertions.assertEquals("value", bean.foo);
        Assertions.assertEquals(42, bean.bar);
        Assertions.assertNotNull(bean.baz);
        Assertions.assertEquals(24, bean.baz.childBar);
        Assertions.assertEquals("childValue", bean.baz.childFoo);
    }

    @Test
    @DisplayName("Ensure that defaults can be set in the parent bean")
    void set_defaults_in_parent(TestInfo test) {
        ParentBean bean = builder.build(ParentBean.class, resource(test, "parent.json"));
        Assertions.assertEquals("value", bean.foo);
        Assertions.assertEquals(42, bean.bar);
        Assertions.assertNotNull(bean.baz);
        Assertions.assertEquals(1234, bean.baz.childBar);
        Assertions.assertEquals("childValue", bean.baz.childFoo);
    }

    @Test
    @DisplayName("Ensure that defaults from parent can be overridden in child")
    void override_defaults_from_parent(TestInfo test) {
        ParentBean bean = builder.build(ParentBean.class, resource(test, "parent.json"));
        Assertions.assertEquals("value", bean.foo);
        Assertions.assertEquals(42, bean.bar);
        Assertions.assertNotNull(bean.baz);
        Assertions.assertEquals(4321, bean.baz.childBar);
        Assertions.assertEquals("notParent", bean.baz.childFoo);
    }

    @Test
    @DisplayName("Resolve grandchildren through tree")
    void grandchild(TestInfo test) {
        ParentWithGrandChild bean = builder.build(ParentWithGrandChild.class, resource(test, "parent.json"));
        Assertions.assertEquals("parentBean", bean.name);
        Assertions.assertNotNull(bean.childInstance);
        Assertions.assertEquals("childBean", bean.childInstance.name);
        Assertions.assertNotNull(bean.childInstance.grandchildInstance);
        Assertions.assertEquals("grandChildValue", bean.childInstance.grandchildInstance.childFoo);
        Assertions.assertEquals(99, bean.childInstance.grandchildInstance.childBar);
    }

    @Test
    @DisplayName("Missing file => null")
    void missing_file() {
        Path p = resource("basic", "empty.json");
        MissingFile b = builder.build(MissingFile.class, p);
        Assertions.assertNull(b.bean);
    }

    @Test
    @DisplayName("File is directory => null")
    void file_is_directory() {
        Path p = resource("basic", "empty.json");
        IgnoreDirs b = builder.build(IgnoreDirs.class, p);
        Assertions.assertNull(b.bean);
    }

    @Test
    @DisplayName("Must not use absolute paths")
    void absolute_path() {
        Path p = resource("basic", "empty.json");
        Throwable t = Assertions.assertThrows(BeanTreeException.class, () -> {
            builder.build(AbsoluteFile.class, p);
        });
        Assertions.assertTrue(t.getMessage().contains("File name cannot be an absolute path: "), t.getMessage());
    }

    @Test
    @DisplayName("Must not use relative paths")
    void relative_path() {
        Path p = resource("basic", "empty.json");
        Throwable t = Assertions.assertThrows(BeanTreeException.class, () -> {
            builder.build(RelativeFile.class, p);
        });
        Assertions.assertTrue(t.getMessage().contains("File name cannot be relativized: "), t.getMessage());
    }

    @Test
    @DisplayName("Empty name uses bean name")
    void empty_name() {
        Path p = resource("basic", "empty.json");
        EmptyFile ef = builder.build(EmptyFile.class, p);
        Assertions.assertNotNull(ef.child);
    }

    @Test
    @DisplayName("Override type must be compatible with field.")
    void bad_override() {
        Path p = resource("basic", "empty.json");
        Throwable t = Assertions.assertThrows(BeanTreeException.class, () -> {
            builder.build(BadOverride.class, p);
        });
        Assertions.assertTrue(t.getMessage().contains("not assignable to target type"), t.getMessage());
    }

    @Test
    @DisplayName("Report when beans cannot be deserialized")
    void cannot_deserialize() {
        Path p = resource("basic", "empty.json");
        Throwable t = Assertions.assertThrows(BeanTreeException.class, () -> {
            builder.build(CannotDeserialize.class, p);
        });
        Assertions.assertTrue(t.getMessage().contains("Cannot instantiate type: "), t.getMessage());

        // no exception when a compatible type is specified
        builder.build(InterfaceOkayWithSpecification.class, p);
    }

    @Test
    @DisplayName("Infinite recursion not allowed / cycle detection works")
    void cycle_detection() {
        Path p = resource("basic", "empty.json");
        Throwable t = Assertions.assertThrows(BeanTreeException.class, () -> builder.build(CycleBean.class, p));
        Assertions.assertTrue(t.getMessage().contains("cycle detected!"), t.getMessage());
        Assertions.assertEquals(2, t.getSuppressed().length);
    }
}
