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


import java.util.Optional;
import net.hunterstrategy.beantree.Template;

public final class TemplateDependencyComparison {
    private TemplateDependencyComparison() {}

    public static int templateDependencyOrder(Injector o1, Injector o2) {
        if (dependsOn(o1, o2)) {
            return -1;
        }
        if (dependsOn(o2, o1)) {
            return 1;
        }
        return 0;
    }

    private static boolean dependsOn(Injector o1, Injector o2) {
        Optional<Template> t1 = getTemplate(o1);
        Optional<Template> t2 = getTemplate(o2);

        if (t1.isEmpty() || t2.isEmpty()) {
            return false;
        }

        Optional<String> dependency = nameOfDependency(t2.get());
        if (dependency.isEmpty()) {
            return false;
        }

        String dependencyName = dependency.get();
        String nameOfTemplate = nameOfTemplate(t1.get());
        return dependencyName.equals(nameOfTemplate);
    }

    private static Optional<Template> getTemplate(Injector i) {
        if (i.annotation() instanceof Template) {
            return Optional.of((Template) i.annotation());
        }
        return Optional.empty();
    }

    private static String nameOfTemplate(Template t) {
        return t.value();
    }

    private static Optional<String> nameOfDependency(Template t) {
        return Optional.ofNullable(
                t.external().template().isBlank() ? null : t.external().template());
    }
}
