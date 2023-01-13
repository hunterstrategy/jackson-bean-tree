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


import java.beans.Introspector;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import net.hunterstrategy.beantree.processor.AnnotationProcessor;

class MethodInjector implements Injector {
    private Method m;
    private Annotation anno;
    private AnnotationProcessor<Annotation> processor;

    MethodInjector(Method m, Annotation anno, AnnotationProcessor<Annotation> processor) {
        this.m = m;
        this.anno = anno;
        this.processor = processor;
    }

    @Override
    public Annotation annotation() {
        return this.anno;
    }

    @Override
    public AnnotationProcessor<Annotation> processor() {
        return this.processor;
    }

    @Override
    public Member member() {
        return this.m;
    }

    @Override
    public String name() {
        // validation has ensured that the method is a setter, and follows
        // JavaBean pattern of "setXxx". Magic number 3 = "set".length()
        return Introspector.decapitalize(m.getName().substring(3));
    }

    private Object possiblyGet(Object target) {
        String prefix = "get";
        if (boolean.class.equals(targetType()) || Boolean.class.equals(targetType())) {
            prefix = "is";
        }
        String getter = prefix + m.getName().substring(3);
        try {
            Method getMethod = m.getDeclaringClass().getMethod(getter);
            return getMethod.invoke(target);
        } catch (Exception e) {
            return null; // operation is optional
        }
    }

    @Override
    public Class<?> targetType() {
        return m.getParameterTypes()[0];
    }

    @Override
    public Type genericType() {
        try {
            return m.getGenericParameterTypes()[0];
        } catch (Exception e) {
            return void.class;
        }
    }

    @Override
    public void inject(DeserializationContext context, Object target) {
        try {
            Object value = processor.instantiate(context, anno, this, possiblyGet(target));
            m.invoke(target, value);
        } catch (InvocationTargetException ite) {
            throw ReflectionSupport.handle(ite);
        } catch (IllegalAccessException iae) {
            throw new IllegalStateException(iae);
        }
    }

    @Override
    public String toString() {
        return String.format("%s: %s", member(), annotation());
    }
}
