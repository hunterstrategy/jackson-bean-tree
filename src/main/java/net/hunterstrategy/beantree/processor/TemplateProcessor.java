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


import net.hunterstrategy.beantree.Template;
import net.hunterstrategy.beantree.analysis.DeserializationContext;
import net.hunterstrategy.beantree.analysis.Injector;

public class TemplateProcessor implements AnnotationProcessor<Template> {
    public static final String BEAN_IGNORE_VALUE = "../***IGNORE***";

    private final BeanProcessor beanProcessor = new BeanProcessor();

    private boolean hasBeanSettings(Template annotation) {
        return !BEAN_IGNORE_VALUE.equals(annotation.external().value());
    }

    @Override
    public void validateAnnotation(Template annotation) {
        if (annotation.value().isBlank()) {
            throw new IllegalArgumentException("Must specify name for template.");
        }

        if (hasBeanSettings(annotation)) {
            beanProcessor.validateAnnotation(annotation.external());
        }
    }

    @Override
    public void validateInContext(DeserializationContext context, Injector i, Template annotation) {
        if (hasBeanSettings(annotation)) {
            beanProcessor.validateInContext(context, i, annotation.external());
        }
    }

    @Override
    public Object instantiate(DeserializationContext context, Template annotation, Injector i, Object target) {
        Object result = target;
        if (hasBeanSettings(annotation)) {
            result = beanProcessor.instantiate(context, annotation.external(), i, result);
        }

        context.registerTemplate(annotation.value(), result, annotation, i);
        return result;
    }
}
