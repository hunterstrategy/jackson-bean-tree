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
import java.lang.reflect.Type;
import net.hunterstrategy.beantree.processor.AnnotationProcessor;

class FieldInjector implements Injector {
    private Field f;
    private Annotation anno;
    private AnnotationProcessor<Annotation> processor;

    FieldInjector(Field f, Annotation anno, AnnotationProcessor<Annotation> processor) {
        this.f = f;
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
        return this.f;
    }

    @Override
    public String name() {
        return f.getName();
    }

    @Override
    public Class<?> targetType() {
        return f.getType();
    }

    @Override
    public Type genericType() {
        try {
            return f.getGenericType();
        } catch (Exception e) {
            return void.class;
        }
    }

    @Override
    public void inject(DeserializationContext context, Object target) {
        try {
            Object value = processor.instantiate(context, anno, this, f.get(target));
            f.set(target, value);
        } catch (IllegalAccessException iae) {
            throw new IllegalStateException(iae);
        }
    }

    @Override
    public String toString() {
        return String.format("%s: %s", member().getName(), annotation());
    }
}
