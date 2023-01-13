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
package net.hunterstrategy.beantree;


import com.fasterxml.jackson.annotation.JacksonAnnotationsInside;
import com.fasterxml.jackson.annotation.JsonMerge;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import net.hunterstrategy.beantree.analysis.ConfigTreeAnnotation;
import net.hunterstrategy.beantree.processor.BeanProcessor;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.METHOD})
@ConfigTreeAnnotation(processor = BeanProcessor.class)
@JacksonAnnotationsInside
@JsonMerge
public @interface Bean {
    /**
     * The name of the file to inject. If blank, use the Java member name.
     * If no extension is specified, used the contextual default file
     * extension.
     */
    String value() default "";

    /**
     * Use an external template. If this is left blank, then the in-place
     * template in the declaring class's file will be used (if present).
     * See {@link Template}.
     *
     * Note: Declaring this will always override an inline template.
     */
    String template() default "";

    /**
     * Override the type to be deserialized. (Use with caution.)
     */
    Class<?> type() default void.class;

    /**
     * Influence the order in which this property is loaded.
     */
    int index() default 0;
}
