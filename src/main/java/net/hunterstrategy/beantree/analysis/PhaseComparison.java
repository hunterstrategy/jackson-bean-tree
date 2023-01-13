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


import net.hunterstrategy.beantree.analysis.ConfigTreeAnnotation.Phase;

public final class PhaseComparison {
    private PhaseComparison() {}

    static Phase phaseOf(Injector i) {
        ConfigTreeAnnotation cta = i.annotation().annotationType().getAnnotation(ConfigTreeAnnotation.class);
        return cta.phase();
    }

    public static int ordinal(Injector i) {
        return phaseOf(i).ordinal();
    }
}
