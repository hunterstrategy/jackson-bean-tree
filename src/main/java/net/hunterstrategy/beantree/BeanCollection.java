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
import net.hunterstrategy.beantree.processor.BeanCollectionProcessor;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.METHOD})
@ConfigTreeAnnotation(processor = BeanCollectionProcessor.class)
@JacksonAnnotationsInside
@JsonMerge
public @interface BeanCollection {
    /**
     * The directory to scan for beans to deserialize.
     */
    String value() default "";

    /**
     * The value to use to lookup defaults, coordinating with
     * {@link Template} annotated beans. If unspecified,
     * the name of the Java member is used.
     */
    String template() default "";

    /**
     * The way in which to map the directory.
     */
    Mapping mapping() default Mapping.CONF_DIR;

    /**
     * The type to be deserialized.
     */
    Class<?> type() default void.class;

    /**
     * Influence the order in which this property is loaded.
     */
    int index() default 0;

    public static enum Mapping {
        /**
         * Maps every file inside one specifically named directory.
         *
         * Map each file in the directory to the target type. This
         * allows for creation of "dot d" style configuration directories,
         * where each file becomes its own bean instance. The value of
         * the BeanCollection will be a literal directory name. Mapped
         * values will be either the loaded file name, or the result of
         * the Nameable interface.
         *
         * In other words, scan for `VALUE/*.json` (or configured extension).
         */
        CONF_DIR,
        /**
         * Maps multiple directories that contain a specifically named file.
         *
         * Search based on a glob expression for subdirectories with a single
         * file that matches the value of this annotation. For example, if the
         * value is 'account.json', match all child directories that contain
         * an 'account.json' file. The mapped name will be the parent directory
         * name, or the result of the Nameable interface.
         *
         * In other words, scan for `*\/VALUE.json` (or configured extension).
         */
        MULTI_DIRS;
    }
}
