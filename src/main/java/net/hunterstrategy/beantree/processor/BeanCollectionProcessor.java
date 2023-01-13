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
import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.hunterstrategy.beantree.BeanCollection;
import net.hunterstrategy.beantree.BeanCollection.Mapping;
import net.hunterstrategy.beantree.analysis.BeanTreeException;
import net.hunterstrategy.beantree.analysis.DeserializationContext;
import net.hunterstrategy.beantree.analysis.Injector;

public class BeanCollectionProcessor implements AnnotationProcessor<BeanCollection> {
    private static final String GLOB_CHARS = "\\*?[{";
    private static final List<Class<?>> COLLECTION_INTERFACES =
            Collections.unmodifiableList(Arrays.asList(Map.class, List.class, Set.class, Queue.class));

    @Override
    public int indexOf(BeanCollection annotation) {
        return annotation.index();
    }

    @Override
    @SuppressFBWarnings(
            value = "PATH_TRAVERSAL_IN",
            justification = "Supplied only at compile time and validated at runtime.")
    public void validateAnnotation(BeanCollection annotation) {
        if (annotation.value().isBlank()) {
            return; // cannot do further validation
        }

        annotation.value().chars().forEach(c -> {
            if (GLOB_CHARS.indexOf(c) > -1) {
                throw new IllegalArgumentException("Dir/file cannot contain special character: " + String.valueOf(c));
            }
        });

        Path p = Paths.get(annotation.value());
        if (p.isAbsolute()) {
            throw new IllegalArgumentException("Dir/file cannot be an absolute path: " + p);
        }

        if (!pathIsConcrete(p)) {
            throw new IllegalArgumentException("Dir/file name cannot be relativized: " + p);
        }
    }

    Class<?> collectionType(Class<?> clazz) {
        for (Class<?> intfc : COLLECTION_INTERFACES) {
            if (intfc.isAssignableFrom(clazz)) {
                return intfc;
            }
        }
        return null;
    }

    boolean isSupportedCollectionType(Class<?> clazz) {
        return collectionType(clazz) != null;
    }

    @Override
    public void validateInContext(DeserializationContext context, Injector i, BeanCollection annotation) {
        if (!isSupportedCollectionType(i.targetType())) {
            throw new IllegalArgumentException("Target type must be Map, List, Queue, or Set.");
        }

        possiblyAssertStringKeyInMap(i);

        Class<?> deserializationType = getTargetDeserializationType(annotation, i);

        if (void.class.equals(deserializationType) || Object.class.equals(deserializationType)) {
            throw new IllegalArgumentException("Must specify deserialization target type.");
        }

        context.assertCanDeserialize(deserializationType, context);
    }

    void possiblyAssertStringKeyInMap(Injector i) {
        Class<?> collectionType = collectionType(i.targetType());
        if (!Map.class.isAssignableFrom(collectionType)) {
            return;
        }
        Type[] arguments = getDeserializationGenericTypes(i);
        if (arguments == null) {
            return;
        }
        if (!String.class.equals(arguments[0])) {
            throw new IllegalStateException("Maps must have String key.");
        }
    }

    Type[] getDeserializationGenericTypes(Injector i) {
        Type type = i.genericType();
        if (type instanceof ParameterizedType) {
            ParameterizedType pt = ParameterizedType.class.cast(type);
            try {
                return pt.getActualTypeArguments();
            } catch (Exception e) {
                // possibility to fail looking up generic type arguments,
                // but that's OK - fall back to requiring user to specify
            }
        }
        return null;
    }

    @Override
    public Class<?> getTargetDeserializationType(BeanCollection annotation, Injector i) {
        Class<?> targetType = annotation.type();

        /*
         * Attempt to read generic type.
         * Maps will have 2 arguments, others 1.
         */
        Type[] arguments = getDeserializationGenericTypes(i);
        if (arguments == null) {
            return targetType;
        }

        Type argument = (arguments.length > 1) ? arguments[1] : arguments[0];
        if (argument instanceof Class) {
            targetType = (Class<?>) (argument);
        }

        return targetType;
    }

    private String name(BeanCollection annotation, Injector i) {
        return annotation.value().isBlank() ? i.name() : annotation.value();
    }

    @Override
    public Object instantiate(DeserializationContext context, BeanCollection annotation, Injector i, Object target) {
        if (target == null) {
            target = context.instantiate(i.targetType());
        }

        Path start = context.peekFile();
        if (!Files.isDirectory(start)) {
            start = start.getParent();
        }
        if (start == null) {
            throw new BeanTreeException("Failed to get starting directory.", i);
        }

        String glob = String.format("glob:%s/%s", start, toGlob(context, annotation, i));
        Function<Path, String> namingStrategy = nameFunction(annotation);
        Class<?> deserializationType = getTargetDeserializationType(annotation, i);
        PathMatcher pm = start.getFileSystem().getPathMatcher(glob);

        String templateName = templateName(annotation.template(), i);
        assertTemplateIsDeserializable(context, templateName, deserializationType, i);

        try (Stream<Path> walker = Files.walk(start, 2)) { // 2 = subdir and file, is max recursion extent
            Map<String, Object> results = walker.filter(Files::isRegularFile)
                    .filter(pm::matches)
                    .collect(Collectors.toMap(namingStrategy, file -> {
                        Object template = context.getTemplateOrInstantiate(templateName, deserializationType);
                        return context.deserialize(template, file, namingStrategy);
                    }));
            applyResults(target, results);
        } catch (IOException ioe) {
            throw new BeanTreeException(ioe, i);
        }

        return target;
    }

    @SuppressWarnings("unchecked")
    private void applyResults(Object target, Map<String, Object> results) {
        if (target instanceof Map) {
            ((Map<String, Object>) target).putAll(results);
        } else if (target instanceof Collection) {
            ((Collection<Object>) target).addAll(results.values());
        } else {
            assert false : "Impossible path due to validation.";
        }
    }

    private Function<Path, String> nameFunction(BeanCollection annotation) {
        return annotation.mapping() == Mapping.MULTI_DIRS
                ? DeserializationContext.PARENT_DIR_NAME
                : DeserializationContext.FILENAME_NO_EXTENSION;
    }

    private String toGlob(DeserializationContext context, BeanCollection annotation, Injector i) {
        String glob = null;
        switch (annotation.mapping()) {
            case CONF_DIR:
                /*
                 * Use all files in a subdirectory. I.e.
                 *
                 * value/foo.json, value/bar.json, value/baz.json.
                 */
                glob = String.format("%s/*%s", name(annotation, i), context.getDefaultExtension());
                break;
            case MULTI_DIRS:
                /*
                 * Use all subdirectories containing value. I.e.
                 *
                 * foo/value.json, bar/value.json, baz/value.json
                 */
                String file = name(annotation, i);
                if (!file.endsWith(context.getDefaultExtension())) {
                    file = file + context.getDefaultExtension();
                }
                glob = String.format("*/%s", file);
                break;
            default:
                assert false : "Unknown mapping type: " + annotation.mapping();
        }
        return glob;
    }
}
