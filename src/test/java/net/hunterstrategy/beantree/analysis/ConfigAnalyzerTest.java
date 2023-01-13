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
package net.hunterstrategy.beantree.analysis;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import net.hunterstrategy.beantree.Bean;
import net.hunterstrategy.beantree.BeanCollection;
import net.hunterstrategy.beantree.FileBeans;
import net.hunterstrategy.beantree.Template;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Unit test: configuration analyzer")
public class ConfigAnalyzerTest {
    List<String> listFields(List<Injector> injectors) {
        return injectors.stream().map(Injector::name).collect(Collectors.toList());
    }

    @Test
    @DisplayName("Simple case based on FileBeans")
    void analyze_simple_case() {
        DeserializationContext ctxt = DeserializationContext.defaultContext(".json");
        List<Injector> injectors = ConfigAnalyzer.analyze(FileBeans.ParentBean.class, ctxt);
        Assertions.assertEquals(1, injectors.size());
        Injector i = injectors.get(0);
        Assertions.assertEquals("baz", i.name());
    }

    @Test
    @DisplayName("Ensure that templates are ordered by dependency")
    public void analyze_template_dependencies() {
        // ensure that the main analyze method employs all necessary sorting,
        // including template dependency ordering.
        DeserializationContext ctxt = DeserializationContext.defaultContext(".json");
        List<Injector> injectors = ConfigAnalyzer.analyze(TemplateDependencies.class, ctxt);

        List<String> expectedFieldOrder = Arrays.asList("fooTemplate", "barTemplate", "value");
        Assertions.assertEquals(expectedFieldOrder, listFields(injectors));

        // ensure the validity of the test - that, prior to sorting, the fields
        // would be processed in the wrong order.
        List<Injector> unsortedInjectors = new ArrayList<>();
        ConfigAnalyzer._analyze(TemplateDependencies.class, unsortedInjectors, ctxt);
        List<String> unsortedFieldOrder = Arrays.asList("value", "barTemplate", "fooTemplate");
        Assertions.assertEquals(unsortedFieldOrder, listFields(unsortedInjectors));

        // ensure that sorting WITHOUT the template dependency, and just based on index
        // and phase sorting alone, produces the wrong ordering for the templates
        Collections.sort(
                unsortedInjectors,
                Comparator.comparing(PhaseComparison::ordinal).thenComparing(Injector::index));
        List<String> partiallySortedFieldOrder = Arrays.asList("barTemplate", "fooTemplate", "value");
        Assertions.assertEquals(partiallySortedFieldOrder, listFields(unsortedInjectors));
    }

    @Test
    @DisplayName("Ensure that @Bean and @BeanCollection order by index")
    public void bean_order_by_index() {
        DeserializationContext ctxt = DeserializationContext.defaultContext(".json");
        List<Injector> injectors = ConfigAnalyzer.analyze(BeanOrder.class, ctxt);
        List<Injector> unsortedInjectors = new ArrayList<>();
        ConfigAnalyzer._analyze(BeanOrder.class, unsortedInjectors, ctxt);
        Assertions.assertEquals(Arrays.asList("bar", "names", "foo"), listFields(injectors));
        Assertions.assertEquals(Arrays.asList("names", "foo", "bar"), listFields(unsortedInjectors));
    }

    // generally field order will be in the order written in the code, so
    // we can rely on that to purposefully create badly-ordered test input
    public static class TemplateDependencies {
        @Bean(value = "valueFile", template = "bar")
        Object value;

        // the bean injected into bar template depends on foo template;
        // foo *must* be processed first
        @Template(value = "bar", external = @Bean(value = "barTemplateFile", template = "foo"))
        Object barTemplate;

        // this is a basic template
        @Template("foo")
        Object fooTemplate;
    }

    public static class BeanOrder {
        @BeanCollection(value = "coll", index = 3)
        List<String> names;

        @Bean(value = "foo", index = 5)
        Object foo;

        @Bean(value = "bar", index = 1)
        Object bar;
    }
}
