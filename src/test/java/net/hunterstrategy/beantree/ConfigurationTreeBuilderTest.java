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
import java.util.Set;
import net.hunterstrategy.beantree.ConfigurationBeans.SimpleSet;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

@DisplayName("Integration test: ConfigurationTreeBuilder features")
public class ConfigurationTreeBuilderTest implements FunctionalTestSupport {
    ConfigurationTreeBuilder builder = new ConfigurationTreeBuilder();

    @Test
    @DisplayName("Can register custom factories for types")
    void custom_factories(TestInfo info) {
        Path p = resource(info, "entry.json");
        SimpleSet bean = builder.build(SimpleSet.class, p);
        Assertions.assertFalse(bean.beans instanceof LinkedHashSet);

        builder.factory(Set.class, LinkedHashSet::new);
        bean = builder.build(SimpleSet.class, p);
        Assertions.assertTrue(bean.beans instanceof LinkedHashSet);
    }

    @Test
    @DisplayName("Can reuse custom factory configuration between builders")
    void shared_factories() {
        Path p = resource("custom_factories", "entry.json");

        builder.factory(Set.class, LinkedHashSet::new);
        ConfigurationTreeBuilder builder2 = new ConfigurationTreeBuilder();
        builder2.reuseFactories(builder);

        SimpleSet bean = builder2.build(SimpleSet.class, p);
        Assertions.assertTrue(bean.beans instanceof LinkedHashSet);
    }

    @Test
    @DisplayName("Can re-run build more than once with same builder")
    void multiple_runs() {
        // tested implicitly with some other tests, but good to have
        // an explicit test for it. Could potentially expand this
        // in the future to make a more exhaustive featureset test
        // to ensure no state is tracked between builds.
        Path p = resource("custom_factories", "entry.json");
        SimpleSet bean1 = builder.build(SimpleSet.class, p);
        SimpleSet bean2 = builder.build(SimpleSet.class, p);
        Assertions.assertEquals(1, bean1.beans.size());
        Assertions.assertEquals(1, bean2.beans.size());
    }

    @Test
    @DisplayName("Extension doesn't need to use the dot")
    void extension_formatting() {
        builder.defaultExtension("json");
        Path p = resource("custom_factories", "entry.json");
        SimpleSet bean = builder.build(SimpleSet.class, p);
        Assertions.assertEquals(1, bean.beans.size());

        // can't find *.foobar
        builder.defaultExtension(".foobar");
        bean = builder.build(SimpleSet.class, p);
        Assertions.assertEquals(0, bean.beans.size());
    }

    @Test
    @DisplayName("Can share the analysis cache between builders")
    void shared_analysis_cache() {
        // this one is tough to test directly.
        Path p = resource("custom_factories", "entry.json");
        builder.build(SimpleSet.class, p);

        ConfigurationTreeBuilder builder2 = new ConfigurationTreeBuilder();
        builder2.reuseCache(builder);
        SimpleSet bean = builder2.build(SimpleSet.class, p);

        Assertions.assertEquals(1, bean.beans.size());
    }
}
