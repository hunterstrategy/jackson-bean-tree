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
import net.hunterstrategy.beantree.InvalidTemplateBeans.MustSpecifyName;
import net.hunterstrategy.beantree.InvalidTemplateBeans.RedeclaredTemplate;
import net.hunterstrategy.beantree.TemplateBeans.Dependency;
import net.hunterstrategy.beantree.TemplateBeans.External;
import net.hunterstrategy.beantree.TemplateBeans.Inline;
import net.hunterstrategy.beantree.analysis.BeanTreeException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

@DisplayName("Integration test: Template")
public class TemplateTest implements FunctionalTestSupport {
    ConfigurationTreeBuilder builder = new ConfigurationTreeBuilder();

    @Test
    @DisplayName("Inline tests with a file bean")
    void inline(TestInfo info) {
        Path p = resource(info, "entry.json");
        Inline bean = builder.build(Inline.class, p);
        Assertions.assertNotNull(bean.defaults);
        Assertions.assertNotNull(bean.bean);
        Assertions.assertEquals("bean_foo", bean.bean.childFoo);
        Assertions.assertEquals(bean.defaults.childBar, bean.bean.childBar);
    }

    @Test
    @DisplayName("External template tests with a file bean")
    void external(TestInfo info) {
        Path p = resource(info, "entry.json");
        External bean = builder.build(External.class, p);
        Assertions.assertNotNull(bean.defaults);
        Assertions.assertNotNull(bean.bean);
        Assertions.assertEquals("bean_foo", bean.bean.childFoo);
        Assertions.assertEquals(bean.defaults.childBar, bean.bean.childBar);
    }

    @Test
    @DisplayName("@Bean templating works, even with Templates")
    void template_dependency(TestInfo info) {
        Path p = resource(info, "entry.json");
        Dependency bean = builder.build(Dependency.class, p);
        Assertions.assertNotNull(bean.defaults);
        Assertions.assertNotNull(bean.bean);
        Assertions.assertNotNull(bean.dependency);
        Assertions.assertEquals("default_foo", bean.defaults.childFoo);
        Assertions.assertEquals(987, bean.defaults.childBar);
        Assertions.assertEquals(777, bean.bean.childBar);
    }

    @Test
    @DisplayName("Cannot re-declare the same template")
    void duplicate_names() {
        Path p = resource("inline", "entry.json");
        Throwable t =
                Assertions.assertThrows(BeanTreeException.class, () -> builder.build(RedeclaredTemplate.class, p));
        Assertions.assertTrue(t.getMessage().contains("Template has already been defined: "), t.getMessage());
    }

    @Test
    @DisplayName("Must specify a name for a Template")
    void template_requires_name() {
        Path p = resource("inline", "entry.json");
        Throwable t = Assertions.assertThrows(BeanTreeException.class, () -> builder.build(MustSpecifyName.class, p));
        Assertions.assertTrue(t.getMessage().contains("Must specify name for template."), t.getMessage());
    }
}
