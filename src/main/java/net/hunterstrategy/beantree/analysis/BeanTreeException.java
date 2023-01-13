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
import java.lang.reflect.Member;

public class BeanTreeException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public BeanTreeException(String message, Injector i) {
        this(new IllegalStateException(message), i);
    }

    public BeanTreeException(Throwable cause) {
        super(cause);
    }

    public BeanTreeException(Throwable cause, Injector i) {
        this(cause, i.name(), i.member(), i.annotation());
    }

    public BeanTreeException(Throwable cause, String name, Member member, Annotation annotation) {
        this(cause);
        addSuppressed(new AncillaryInformationException(String.format("Error at target %s: %s", name, member)));
        addSuppressed(new AncillaryInformationException("Error with annotation: " + annotation));
    }

    @Override
    public synchronized Throwable fillInStackTrace() {
        return null;
    }

    @Override
    public String getMessage() {
        StringBuilder msg = new StringBuilder(getCause().toString());
        for (Throwable t : getSuppressed()) {
            msg.append("\n").append("\t=> ").append(t.getMessage());
        }
        return msg.toString();
    }
}
