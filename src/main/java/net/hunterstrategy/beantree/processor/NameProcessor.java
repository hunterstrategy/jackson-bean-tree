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
package net.hunterstrategy.beantree.processor;


import net.hunterstrategy.beantree.Name;
import net.hunterstrategy.beantree.analysis.DeserializationContext;
import net.hunterstrategy.beantree.analysis.Injector;

public class NameProcessor implements AnnotationProcessor<Name> {

    @Override
    public void validateAnnotation(Name annotation) {}

    @Override
    public void validateInContext(DeserializationContext context, Injector i, Name annotation) {
        if (!String.class.equals(i.targetType())) {
            throw new IllegalArgumentException("Name must be String");
        }
    }

    @Override
    public Object instantiate(DeserializationContext context, Name annotation, Injector i, Object target) {
        return context.peekName();
    }
}
