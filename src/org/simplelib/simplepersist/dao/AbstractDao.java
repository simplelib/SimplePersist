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

import java.util.List;
import java.util.regex.Pattern;

import org.simplelib.simplepersist.dao.Queries.Query;

abstract class AbstractDao<T extends Entity<?>> implements IDao<T> {
    /** Generated serial version UID. */
    private static final long serialVersionUID = -8893032967789623818L;
    protected static final Pattern RC_PATTERN = Pattern.compile("\\r?\\n");
    /** Using cache for last query. */
    private final boolean USE_QUERY_CACHE;
    /** Last search model. */
    private T lastModel;
    /** Last result. */
    private List<T> lastResult;

    public AbstractDao(final boolean queryCache) {
	this.USE_QUERY_CACHE = Queries.useCache;
    }

    /**
     * Standard implementation of multiple insert.
     * <p>
     * This implementation iterates over the specified array, adding all
     * elements in database, one at a time, using {@link IDao#add(Entity)}.
     * </p>
     */
    @Override
    public int addAll(final List<T> objects) {
	unCache();
	int i = 0;
	for (T o : objects) {
	    i += add(o);
	}
	return i;
    }

    /**
     * Standard implementation of multiple update.
     * <p>
     * This implementation iterates over the specified array, updating all
     * elements in database, one at a time, using {@link IDao#update(Entity)}.
     * </p>
     */
    @Override
    public int updateAll(final List<T> objects) {
	unCache();
	int i = 0;
	for (T o : objects) {
	    i += update(o);
	}
	return i;
    };

    @Override
    public boolean createTable() {
	AccesBdd.LOGGER.warning("Unsupported operation on this DAO.");
	return true;
	// throw new UnsupportedOperationException(
	// "Unsupported operation on this DAO.");
    }

    @Override
    public boolean dropTableIfExists() {
	AccesBdd.LOGGER.warning("Unsupported operation on this DAO.");
	return true;
	// throw new UnsupportedOperationException(
	// "Unsupported operation on this DAO.");
    }

    @Override
    public void init() {
	// Do nothing
    }

    @Override
    public List<T> search(final String query) {
	throw new UnsupportedOperationException(
		"Unsupported operation on this DAO."
			+ " Use @Table annotation to link an entity with a SQL database.");
    }

    public <R> R execute(Query<R> query) {
	throw new UnsupportedOperationException(
		"Unsupported operation on this DAO."
			+ " Use @Table annotation to link an entity with a SQL database.");
    }

    protected boolean isInCache(final T model) {
	return USE_QUERY_CACHE && Queries.useCache
		&& model.equals(this.lastModel);
    }

    protected void unCache() {
	if (USE_QUERY_CACHE) {
	    lastModel = null;
	}
    };

    protected List<T> getCache(final T model) {
	AccesBdd.LOGGER.finest("Using query cache.");
	return this.lastResult;
    }

    @SuppressWarnings("unchecked")
    protected void toCache(final T model, final List<T> result) {
	if (USE_QUERY_CACHE && Queries.useCache) {
	    try {
		this.lastModel = (T) model.getClass().getMethod("clone")
			.invoke(model);
		this.lastResult = result;
	    } catch (Exception e) {
		AccesBdd.LOGGER
			.warning("Using query cache on a non clonable entity.");
		this.lastModel = null;
		this.lastResult = null;
	    }
	}
    }
}
