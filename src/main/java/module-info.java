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
module net.hunterstrategy.jackson_config_tree {
    /*
     * Compile-only dependencies.
     */
    requires static com.github.spotbugs.annotations;

    /*
     * Jackson is exposed in the API, hence it should be transitive.
     */
    requires transitive com.fasterxml.jackson.databind;

    /*
     * Used for JavaBean name mangling (reflection)
     */
    requires java.desktop;

    /*
     * Allow Jackson to reflect into this package.
     */
    opens net.hunterstrategy.beantree to
            com.fasterxml.jackson.databind;

    /*
     * Public API packages.
     */
    exports net.hunterstrategy.beantree;
}
