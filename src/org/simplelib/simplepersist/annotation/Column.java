/*
 * Copyright 2012 simplelib.org
 * 
 * This file is part of SimplePersist.
 * 
 * SimplePersist is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * SimplePersist is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with SimplePersist. If not, see <http://www.gnu.org/licenses/>.
 */

package org.simplelib.simplepersist.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Column specifications in a database linked to a class field.
 * 
 * @author simplelib.org
 * 
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Column {
    /**
     * Column name in database.
     * 
     * @return {@link String}: column name.
     */
    String name();

    /**
     * Sql type in the table with type and capacity. Example :
     * <ul>
     * <li>VARCHAR(10)</li>
     * <li>INTEGER</li>
     * </ul>
     * 
     * @return {@link String}: column SQL type.
     */
    String type();
}
