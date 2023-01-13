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


import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import net.hunterstrategy.beantree.processor.AnnotationProcessor;

public class ConfigAnalyzerCache {
    private final ConcurrentMap<Class<?>, List<Injector>> injectors = new ConcurrentHashMap<>();
    private final ConcurrentMap<
                    Class<? extends AnnotationProcessor<? extends Annotation>>,
                    AnnotationProcessor<? extends Annotation>>
            instances = new ConcurrentHashMap<>();

    public List<Injector> injectors(Class<?> clazz, DeserializationContext context) {
        return injectors.compute(clazz, (c, list) -> {
            if (list == null) {
                return ConfigAnalyzer.analyze(c, context);
            }
            ConfigAnalyzer.validateInContext(list, context);
            return list;
        });
    }

    public AnnotationProcessor<? extends Annotation> processor(ConfigTreeAnnotation cta) {
        return instances.computeIfAbsent(cta.processor(), p -> {
            try {
                return p.getConstructor().newInstance();
            } catch (InvocationTargetException ite) {
                throw ReflectionSupport.handle(ite);
            } catch (NoSuchMethodException | IllegalAccessException | InstantiationException e) {
                throw new IllegalStateException(e);
            }
        });
    }
}
