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


import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Supplier;
import net.hunterstrategy.beantree.Template;
import net.hunterstrategy.beantree.processor.AnnotationProcessor;

public class DeserializationContext {
    public static final Function<Path, String> FILENAME_NO_EXTENSION = DeserializationContext::fileName;
    public static final Function<Path, String> PARENT_DIR_NAME = DeserializationContext::parentDirName;

    // settings & collaborators
    private final ObjectMapper mapper;
    private final String defaultExtension;
    private final Map<Class<?>, Supplier<?>> factories;

    // deserialization state
    private final ConfigAnalyzerCache cache;
    private final Deque<Path> stack = new ArrayDeque<>();
    private final Deque<String> names = new ArrayDeque<>();
    private final Map<String, TemplateInfo> templates = new ConcurrentHashMap<>();

    static DeserializationContext defaultContext(String extension) {
        ObjectMapper mapper = new ObjectMapper();
        mapper.setVisibility(PropertyAccessor.FIELD, Visibility.ANY);
        mapper.setVisibility(PropertyAccessor.SETTER, Visibility.PUBLIC_ONLY);
        ConfigAnalyzerCache cache = new ConfigAnalyzerCache();
        return new DeserializationContext(mapper, cache, extension, new ConcurrentHashMap<>());
    }

    private static String fileName(Path path) {
        String fileName = path.getFileName().toString();
        return fileName.substring(0, fileName.lastIndexOf('.'));
    }

    private static String parentDirName(Path path) {
        Path parentDir = path.getParent();
        return parentDir.getFileName().toString();
    }

    @SuppressFBWarnings(
            value = "EI_EXPOSE_REP2",
            justification = "Intended behavior to allow client to pass in Mapper.")
    public DeserializationContext(
            ObjectMapper mapper,
            ConfigAnalyzerCache cache,
            String defaultExtension,
            Map<Class<?>, Supplier<?>> factories) {
        this.mapper = mapper;
        this.cache = cache;
        this.defaultExtension = defaultExtension;
        this.factories = factories;
    }

    public String getDefaultExtension() {
        return defaultExtension;
    }

    @SuppressWarnings("unchecked")
    public AnnotationProcessor<Annotation> processorOf(ConfigTreeAnnotation cta) {
        return (AnnotationProcessor<Annotation>) this.cache.processor(cta);
    }

    /**
     * Check whether the target type can be deserialized or not.
     * @param clazz the target type
     * @throws IllegalArgumentException if the target type cannot be deserialized
     */
    public void assertCanDeserialize(Class<?> clazz, DeserializationContext context) {
        JavaType jt = mapper.getTypeFactory().constructType(clazz);
        AtomicReference<Throwable> cause = new AtomicReference<>();
        if (!mapper.canDeserialize(jt, cause)) {
            throw new IllegalArgumentException("Cannot deserialize " + clazz, cause.get());
        }

        try {
            context.instantiate(clazz);
        } catch (Exception e) {
            throw new IllegalArgumentException("Cannot instantiate type: " + clazz, e);
        }
    }

    /**
     * Create an object. First check to see if a factory has been specified
     * to use for its instantiation. Otherwise, attempt to find a no-argument
     * public constructor.
     */
    public <T> T instantiate(Class<T> type) {
        // Try to use factory
        if (factories.containsKey(type)) {
            return type.cast(factories.get(type).get());
        }

        // Find a no-arg constructor.
        try {
            return type.getConstructor().newInstance();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Deserialize the given type with the specified configuration file.
     * Use the {@link #instantiate(Class)} method to construct an instance.
     * @return an instance deserialized from the configuration file
     */
    public <T> T deserialize(Class<T> type, Path configurationFile, Function<Path, String> nameFunction) {
        T instance = instantiate(type);
        return deserialize(instance, configurationFile, nameFunction);
    }

    /**
     * Deserialize the given configuration file into the given object instance,
     * using the filename as the contextual "name" for the bean.
     */
    public <T> T deserialize(Class<T> type, Path configurationFile) {
        return deserialize(type, configurationFile, FILENAME_NO_EXTENSION);
    }

    /**
     * Deserialize the given configuration file into the given object instance,
     * using the provided name function to provide the contextual "name" for the bean.
     * @param instance the instance to deserialize into
     * @param configurationFile the source configuration file
     * @param nameFunction the naming function
     * @return the deserialized instance
     */
    public <T> T deserialize(T instance, Path configurationFile, Function<Path, String> nameFunction) {
        push(configurationFile, nameFunction);
        try {
            // deserialize file
            try (InputStream is = Files.newInputStream(configurationFile, StandardOpenOption.READ)) {
                mapper.readerForUpdating(instance).readValue(is);
            }
            // run injectors on type, possibly recursing
            for (Injector i : cache.injectors(instance.getClass(), this)) {
                try {
                    i.inject(this, instance);
                } catch (BeanTreeException e) {
                    throw e;
                } catch (Exception e) {
                    throw new BeanTreeException(e, i);
                }
            }
            return instance;
        } catch (BeanTreeException e) {
            throw e;
        } catch (Exception e) {
            throw new BeanTreeException(e);
        } finally {
            pop();
        }
    }

    public void push(Path path, Function<Path, String> nameFunction) {
        if (stack.contains(path)) {
            throw new IllegalStateException("ERROR: cycle detected!");
        }
        pushFile(path);
        pushName(nameFunction.apply(path));
    }

    public void pop() {
        popFile();
        popName();
    }

    private void pushName(String name) {
        names.addLast(name);
    }

    private String popName() {
        return names.removeLast();
    }

    public String peekName() {
        return names.peekLast();
    }

    public Path peekFile() {
        return stack.peekLast();
    }

    private void pushFile(Path path) {
        stack.addLast(path);
    }

    private Path popFile() {
        return stack.removeLast();
    }

    public void registerTemplate(String name, Object obj, Template settings, Injector source) {
        TemplateInfo info = new TemplateInfo();
        info.settings = settings;
        info.source = source;
        info.template = obj;
        if (templates.putIfAbsent(name, info) != null) {
            BeanTreeException e = new BeanTreeException("Template has already been defined: " + name, source);
            throw annotateTemplateError(e, name);
        }
    }

    public Object getTemplateOrInstantiate(String name, Class<?> type) {
        TemplateInfo info = templates.get(name);
        if (info == null) {
            return instantiate(type);
        }

        // clone the template with Jackson so every use of it is
        // fresh, and absent state from previous mappings
        try {
            return mapper.treeToValue(mapper.valueToTree(info.template), info.template.getClass());
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public BeanTreeException annotateTemplateError(BeanTreeException e, String name) {
        TemplateInfo info = templates.get(name);
        if (info != null) {
            e.addSuppressed(new AncillaryInformationException("Template source: " + info.source.member()));
            e.addSuppressed(new AncillaryInformationException("Template annotation: " + info.settings));
        }
        return e;
    }
}

class TemplateInfo {
    Object template;
    Template settings;
    Injector source;
}
