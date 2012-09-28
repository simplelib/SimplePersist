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

package org.simplelib.simplepersist.dao;

import java.lang.reflect.Field;
import java.sql.Date;
import java.sql.Timestamp;

/**
 * Description d'une table associée en BDD.
 * 
 * @author simplelib.org
 * 
 */
class TableDescription {
    /**
     * Description d'une colonne Sql.
     * 
     * @author s.mozzi
     */
    static class ColumnDescription {
	final String NAME;
	final SqlProp SQL_PROP;
	final boolean INDEX;
	final boolean PRIMARY_KEY;
	final Field FIELD;

	public ColumnDescription(final Field field, final String name,
		final SqlProp sqlProp, final boolean index,
		final boolean primaryKey) {
	    this.NAME = name;
	    this.SQL_PROP = sqlProp;
	    this.INDEX = index;
	    this.PRIMARY_KEY = primaryKey;
	    this.FIELD = field;
	}

	@Override
	public String toString() {
	    return this.NAME;
	}
    }

    /**
     * Définition d'une type de données SQL.
     * 
     * @author s.mozzi
     */
    static class SqlProp {
	static enum SqlType {
	    INTEGER, VARCHAR, DOUBLE_PRECISION, TIMESTAMP, DATE, BOOL, CHAR;

	    @Override
	    public String toString() {
		switch (this) {
		case DOUBLE_PRECISION:
		    return "DOUBLE PRECISION";
		case BOOL:
		    return "BOOLEAN"; // Standard SQL
		default:
		    return super.toString();
		}
	    };
	}

	/**
	 * Construction d'un type Sql depuis un type de données Java.
	 * 
	 * @param type
	 *            {@link Class} java à convertir.
	 * @return {@link SqlProp} : Type SQL associé.
	 */
	static SqlProp valueOf(final Class<?> type) {
	    SqlType sqlType;
	    Integer sqlLenght = null;
	    if (type == Integer.class || type == Integer.TYPE) {
		sqlType = SqlType.INTEGER;
	    } else if (type == Boolean.class || type == Boolean.TYPE) {
		sqlType = SqlType.BOOL;
	    } else if (type == Character.class || type == Character.TYPE) {
		sqlType = SqlType.CHAR;
	    } else if (type == Double.class || type == Double.TYPE) {
		sqlType = SqlType.DOUBLE_PRECISION;
	    } else if (type == String.class) {
		sqlType = SqlType.VARCHAR;
		sqlLenght = 50;
	    } else if (type == Date.class) {
		sqlType = SqlType.DATE;
	    } else if (type == Timestamp.class) {
		sqlType = SqlType.TIMESTAMP;
	    } else {
		throw new UnsupportedOperationException(
			"Type non pris en charge par la persistance :"
				+ "\nseul les références par identifiant sont prises en charge.");
	    }
	    return new SqlProp(sqlType, sqlLenght);
	}

	/**
	 * Construction d'un type Sql depuis sa représentation ASCII.
	 * 
	 * @param sqlType
	 *            Type de donnée.
	 * @return {@link SqlProp} : Type SQL associé.
	 */
	static SqlProp valueOf(final String sqlType) {
	    String[] champs = sqlType.split("\\(");
	    if (champs.length > 1) {
		champs[1] = champs[1].substring(0, champs[1].length() - 1);
		return new SqlProp(SqlType.valueOf(champs[0]),
			Integer.valueOf(champs[1]));
	    } else {
		return new SqlProp(SqlType.valueOf(champs[0]), null);
	    }
	}

	SqlType SQL_TYPE;

	final Integer LENGHT;

	/**
	 * Construction d'un type SQL.
	 * 
	 * @param type
	 *            {@link SqlType}.
	 * @param lenght
	 *            Taille du type.
	 */
	SqlProp(final SqlType type, final Integer lenght) {
	    this.SQL_TYPE = type;
	    this.LENGHT = lenght;
	}

	@Override
	public String toString() {
	    return this.LENGHT == null ? this.SQL_TYPE.toString()
		    : this.SQL_TYPE.toString() + '(' + this.LENGHT + ')';
	}
    }

    final String NAME;
    final ColumnDescription[] COLUMNS;
    final Class<? extends Entity<?>> CLASS;

    TableDescription(final String name,
	    final ColumnDescription[] columns,
	    final Class<? extends Entity<?>> classe) {
	this.NAME = name;
	this.COLUMNS = columns;
	this.CLASS = classe;
    }
}
