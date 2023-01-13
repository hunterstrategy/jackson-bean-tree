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


import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import net.hunterstrategy.beantree.Bean;
import net.hunterstrategy.beantree.analysis.DeserializationContext;
import net.hunterstrategy.beantree.analysis.Injector;

public class BeanProcessor implements AnnotationProcessor<Bean> {
    @Override
    @SuppressFBWarnings(
            value = "PATH_TRAVERSAL_IN",
            justification = "Supplied only at compile time and validated at runtime.")
    public void validateAnnotation(Bean annotation) {
        if (annotation.value().isBlank()) {
            return; // can't do more validations
        }

        Path p = Paths.get(annotation.value());
        if (p.isAbsolute()) {
            throw new IllegalArgumentException("File name cannot be an absolute path: " + p);
        }
        if (!pathIsConcrete(p)) {
            throw new IllegalArgumentException("File name cannot be relativized: " + p);
        }
    }

    @Override
    public void validateInContext(DeserializationContext context, Injector i, Bean annotation) {
        Class<?> targetType = getTargetDeserializationType(annotation, i);

        if (!i.targetType().isAssignableFrom(targetType)) {
            throw new IllegalArgumentException(
                    String.format("Type %s not assignable to target type: %s", targetType, i.targetType()));
        }

        context.assertCanDeserialize(targetType, context);
    }

    @Override
    public Class<?> getTargetDeserializationType(Bean annotation, Injector i) {
        if (annotation.type().equals(void.class)) {
            return i.targetType();
        }
        return annotation.type();
    }

    @Override
    public int indexOf(Bean annotation) {
        return annotation.index();
    }

    String targetFile(DeserializationContext context, Bean annotation, Injector i) {
        String name = annotation.value();
        if (name.isBlank()) {
            name = i.name();
        }

        if (name.contains(".")) {
            return name;
        }
        return name + context.getDefaultExtension();
    }

    Path resolve(Path location, String nextLocation) {
        Path resolver = location;
        if (!Files.isDirectory(location)) {
            resolver = location.getParent();
        }
        if (resolver == null) {
            throw new IllegalStateException("Failed to get parent directory.");
        }
        return resolver.resolve(nextLocation);
    }

    @Override
    public Object instantiate(DeserializationContext context, Bean annotation, Injector i, Object target) {
        Path theFile = resolve(context.peekFile(), targetFile(context, annotation, i));
        if (!Files.exists(theFile) && !Files.isRegularFile(theFile)) {
            return null;
        }

        Class<?> deserializationType = getTargetDeserializationType(annotation, i);
        String templateName = templateName(annotation.template(), i);
        assertTemplateIsDeserializable(context, templateName, deserializationType, i);

        // if no value exists in the declaring file, *or* if a template is declared.
        // a warning is probably due if both an inline default is present *and* a template
        // is being declared.
        if (target == null || !annotation.template().isBlank()) {
            target = context.getTemplateOrInstantiate(templateName, deserializationType);
        }

        return context.deserialize(target, theFile, DeserializationContext.FILENAME_NO_EXTENSION);
    }
}
