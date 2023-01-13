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
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import net.hunterstrategy.beantree.processor.AnnotationProcessor;

public class ConfigAnalyzer {
    private static final Comparator<Injector> COMPARATOR = Comparator.comparingInt(PhaseComparison::ordinal)
            .thenComparing(Injector::index)
            .thenComparing(TemplateDependencyComparison::templateDependencyOrder);

    private static interface InjectorFactory<T> {
        Injector instantiate(T target, Annotation a, AnnotationProcessor<Annotation> p);
    }

    private static <T extends Member> Injector _createInjector(
            DeserializationContext context, InjectorFactory<T> fac, T target, Annotation a, ConfigTreeAnnotation cta) {
        AnnotationProcessor<Annotation> p = context.processorOf(cta);
        String name = null;
        try {
            p.validateAnnotation(a);
            Injector i = fac.instantiate(target, a, p);
            name = i.name();
            p.validateInContext(context, i);
            return i;
        } catch (RuntimeException e) {
            throw new BeanTreeException(e, name == null ? target.getName() : name, target, a);
        }
    }

    public static List<Injector> analyze(Class<?> type, DeserializationContext context) {
        List<Injector> injectionPoints = new ArrayList<>();
        _analyze(type, injectionPoints, context);
        Collections.sort(injectionPoints, COMPARATOR);
        return injectionPoints;
    }

    public static void validateInContext(List<Injector> injectors, DeserializationContext context) {
        for (Injector i : injectors) {
            try {
                i.processor().validateInContext(context, i);
            } catch (RuntimeException e) {
                throw new BeanTreeException(e, i);
            }
        }
    }

    private static <T extends Member> void _targetAnnotations(
            Annotation[] annotations,
            DeserializationContext context,
            InjectorFactory<T> fac,
            T target,
            List<Injector> injectionPoints) {
        Arrays.asList(annotations).stream()
                .filter(a -> a.annotationType().isAnnotationPresent(ConfigTreeAnnotation.class))
                .collect(Collectors.toMap(a -> a, a -> a.annotationType().getAnnotation(ConfigTreeAnnotation.class)))
                .entrySet()
                .stream()
                .map(e -> _createInjector(context, fac, target, e.getKey(), e.getValue()))
                .sequential()
                .collect(Collectors.toCollection(() -> injectionPoints));
    }

    private static void _analyzeField(Field f, List<Injector> injectionPoints, DeserializationContext context) {
        f.setAccessible(true);
        _targetAnnotations(f.getAnnotations(), context, FieldInjector::new, f, injectionPoints);
    }

    private static void _analyzeMethod(Method m, List<Injector> injectionPoints, DeserializationContext context) {
        if (!void.class.equals(m.getReturnType())) {
            return;
        }
        if (m.getParameterCount() != 1) {
            return;
        }
        if (!m.getName().startsWith("set")) {
            return;
        }
        _targetAnnotations(m.getAnnotations(), context, MethodInjector::new, m, injectionPoints);
    }

    static void _analyze(Class<?> type, List<Injector> injectionPoints, DeserializationContext context) {
        if (type.getPackageName().startsWith("java")) {
            return; // don't need to recurse into platform types
        }

        // recurse type hierarchy
        if (type.getSuperclass() != null) {
            _analyze(type.getSuperclass(), injectionPoints, context);
        }

        for (Field f : type.getDeclaredFields()) {
            _analyzeField(f, injectionPoints, context);
        }

        for (Method m : type.getDeclaredMethods()) {
            _analyzeMethod(m, injectionPoints, context);
        }
    }
}
