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


import com.fasterxml.jackson.annotation.JsonIgnore;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import net.hunterstrategy.beantree.analysis.BeanTreeException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

@DisplayName("Integration test: Method injection")
public class MethodInjectorTest implements FunctionalTestSupport {
    ConfigurationTreeBuilder builder = new ConfigurationTreeBuilder();

    @Test
    @DisplayName("Happy path: inject @Name and @SourceFile with setter")
    void set_bean_metadata(TestInfo info) {
        Path p = resource(info, "empty.json");
        BeanName bn = builder.build(BeanName.class, p);
        Assertions.assertEquals("empty", bn.getName());
        Assertions.assertEquals(p, bn.getSource());
    }

    @Test
    @DisplayName("Runtime exceptions from setter methods are handled via BeanTreeException")
    void runtime_exception_in_setter() {
        Path p = resource("set_bean_metadata", "empty.json");
        Throwable t = Assertions.assertThrows(
                BeanTreeException.class, () -> builder.build(RuntimeExceptionInSetter.class, p));
        Assertions.assertTrue(t.getMessage().contains("WITHIN"), t.getMessage());
        Assertions.assertTrue(t.getSuppressed().length == 2);
    }

    @Test
    @DisplayName("Checked exceptions from setter methods are handled via BeanTreeException")
    void checked_exception_in_setter() {
        Path p = resource("set_bean_metadata", "empty.json");
        Throwable t =
                Assertions.assertThrows(BeanTreeException.class, () -> builder.build(NormalExceptionInSetter.class, p));
        Assertions.assertTrue(t.getMessage().contains("java.io.FileNotFoundException"), t.getMessage());
        Assertions.assertTrue(t.getSuppressed().length == 2);
    }

    public static class BeanName {
        @JsonIgnore
        private String name;

        @JsonIgnore
        private Path source;

        @Name
        public void setName(String name) {
            this.name = name;
        }

        public String getName() {
            return this.name;
        }

        @SourceFile
        public void setSource(Path source) {
            this.source = source;
        }

        public Path getSource() {
            return this.source;
        }
    }

    public static class NormalExceptionInSetter {
        @JsonIgnore
        private String name;

        @Name
        public void setName(String name) throws IOException {
            throw new FileNotFoundException();
        }
    }

    public static class RuntimeExceptionInSetter {
        @JsonIgnore
        private String name;

        @Name
        public void setName(String name) {
            throw new IllegalStateException("WITHIN");
        }
    }
}
