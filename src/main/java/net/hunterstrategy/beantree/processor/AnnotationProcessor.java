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


import java.lang.annotation.Annotation;
import java.nio.file.Path;
import net.hunterstrategy.beantree.analysis.BeanTreeException;
import net.hunterstrategy.beantree.analysis.DeserializationContext;
import net.hunterstrategy.beantree.analysis.Injector;

public interface AnnotationProcessor<T extends Annotation> {
    /**
     * Validate the annotation generally, when it's first loaded,
     * for things such as values out of range or illegal values.
     */
    void validateAnnotation(T annotation);

    /**
     * Validate the annotation in the context of its targeted field
     * and deserialization configuration.
     */
    void validateInContext(DeserializationContext context, Injector i, T annotation);

    /**
     * Validate the annotation in the context of its targeted field
     * and deserialization configuration.
     */
    @SuppressWarnings("unchecked")
    default void validateInContext(DeserializationContext context, Injector i) {
        validateInContext(context, i, (T) i.annotation());
    }

    /**
     * Throw an exception if the template cannot be deserialized. This must be pushed to
     * deserialization phase vs. a validation phase, as it depends on the context of what
     * templates have been created/exist.
     */
    default void assertTemplateIsDeserializable(
            DeserializationContext context, String templateName, Class<?> deserializationType, Injector i) {
        Object template = context.getTemplateOrInstantiate(templateName, deserializationType);
        if (!deserializationType.isAssignableFrom(template.getClass())) {
            String msg = String.format(
                    "Template of type %s is not compatible with %s", template.getClass(), deserializationType);
            BeanTreeException e = new BeanTreeException(msg, i);
            throw context.annotateTemplateError(e, templateName);
        }
    }

    /**
     * Ensure there are no "." or ".." path elements in the given
     * path.
     *
     * @return true if there are no directory symbols in the expression
     */
    default boolean pathIsConcrete(Path p) {
        Path fileNamePath;
        if (p == null || (fileNamePath = p.getFileName()) == null) {
            return true;
        }
        String fileName = fileNamePath.toString();
        if (".".equals(fileName) || "..".equals(fileName)) {
            return false;
        }
        Path parent = p.getParent();
        return pathIsConcrete(parent);
    }

    /**
     * Get the computed target type of this action, allowing the annotation to
     * potentially override or modify the target of the Injector.
     */
    default Class<?> getTargetDeserializationType(T annotation, Injector i) {
        return i.targetType();
    }

    /**
     * Compute the index of this annotation.
     */
    default int indexOf(T annotation) {
        return 0;
    }

    default String templateName(String annotatedValue, Injector i) {
        return annotatedValue.isBlank() ? i.name() : annotatedValue;
    }

    /**
     * Create the object to be injected by the Injector.
     *
     * @param context context-specific configuration that can influence deserialization
     * @param annotation the annotation
     * @param i the injector
     * @param target the target object for merging, or null if it needs to be created
     * @return the resultant object
     */
    Object instantiate(DeserializationContext context, T annotation, Injector i, Object target);
}
