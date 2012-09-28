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

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author simplelib.org
 */
public final class Queries {
    static boolean useCache = true;
    
    public static class FromClause<R> extends Query<R> {
	private final Class<? extends Entity<?>> sourceClass;

	private FromClause(final Query<R> parent,
		final Class<? extends Entity<?>> sourceClass) {
	    super(parent);
	    this.sourceClass = sourceClass;
	}

	Class<? extends Entity<?>> getSourceClass() {
	    return this.sourceClass;
	}

	public JoinClause<R> innerJoin(
		final Class<? extends Entity<?>> joinedClass) {
	    return toChild(new JoinClause<R>(this, joinedClass, JoinType.INNER));
	}

	public JoinClause<R> leftJoin(
		final Class<? extends Entity<?>> joinedClass) {
	    return toChild(new JoinClause<R>(this, joinedClass, JoinType.LEFT));
	}

	public JoinClause<R> rightJoin(
		final Class<? extends Entity<?>> joinedClass) {
	    return toChild(new JoinClause<R>(this, joinedClass, JoinType.RIGHT));
	}

	public WhereClause<R> where(final String whereClause) {
	    return toChild(new WhereClause<R>(this, whereClause));
	}
    }

    public static class JoinClause<R> extends Query<R> {
	private final Class<? extends Entity<?>> joinedClass;
	private final JoinType joinType;

	private JoinClause(final Query<R> parent,
		final Class<? extends Entity<?>> joinedClass,
		final JoinType type) {
	    super(parent);
	    this.joinedClass = joinedClass;
	    this.joinType = type;
	}

	Class<? extends Entity<?>> getJoinedClass() {
	    return this.joinedClass;
	}

	JoinType getType() {
	    return joinType;
	}

	public JoinClause<R> innerJoin(
		final Class<? extends Entity<?>> joinedClass) {
	    return toChild(new JoinClause<R>(this, joinedClass, JoinType.INNER));
	}

	public JoinClause<R> leftJoin(
		final Class<? extends Entity<?>> joinedClass) {
	    return toChild(new JoinClause<R>(this, joinedClass, JoinType.LEFT));
	}

	public JoinClause<R> rightJoin(
		final Class<? extends Entity<?>> joinedClass) {
	    return toChild(new JoinClause<R>(this, joinedClass, JoinType.RIGHT));
	}

	public OnClause<R> on(final String booleanClause) {
	    return toChild(new OnClause<R>(this, booleanClause));
	}
    }

    /**
     * Type de jointure. Tels que définis en SQL.
     * 
     * @author s.mozzi
     * 
     */
    static enum JoinType {
	INNER, LEFT, RIGHT
    }

    public static class OnClause<R> extends Query<R> {
	private final String booleanClause;

	private OnClause(final Query<R> parent, final String booleanClause) {
	    super(parent);
	    this.booleanClause = booleanClause;
	}

	public WhereClause<R> where(final String whereClause) {
	    return toChild(new WhereClause<R>(this, whereClause));
	}

	String getClause() {
	    return this.booleanClause;
	}
    }

    public static class SetClause<T extends Entity<?>, R> extends Query<R> {
	private final T value;

	private SetClause(final Query<R> parent, T value) {
	    super(parent);
	    this.value = value;
	}

	public WhereClause<R> where(final String whereClause) {
	    return toChild(new WhereClause<R>(this, whereClause));
	}

	T getValue() {
	    return this.value;
	}
    }

    public static abstract class Query<R> {
	private Query<R> parent;
	private Query<R> child;
	private Object cache;

	protected Query(final Query<R> parent) {
	    this.parent = parent;
	}

	public R execute() {
	    // Execution de la requête depuis la racine
	    if (this.parent != null) {
		return this.parent.execute();
	    } else {
		throw new UnsupportedOperationException(
			"Execution de ce type de requête non pris en charge.");
	    }
	}

	Query<R> getParent() {
	    return this.parent;
	}

	Query<R> getChild() {
	    return this.child;
	}

	protected <T extends Query<R>> T toChild(final T childQuery) {
	    this.child = childQuery;
	    this.cache = null;
	    return childQuery;
	}

	/**
	 * Service de mise en cache de la requête une fois compilée par une DAO.
	 * 
	 * @param compiledQuery
	 *            Requête compilée.
	 */
	void setCache(Object compiledQuery) {
	    this.cache = compiledQuery;
	}

	/**
	 * Service de récuppération d'une requête compilée mise en cache.
	 * 
	 * @return Requête compilée.
	 */
	Object getCache() {
	    return this.cache;
	}
    }

    public static class SelectClause<T extends Entity<?>> extends
	    Query<List<T>> {
	private final Class<T> result;
	private final boolean distinct;

	private SelectClause(final Class<T> result, final boolean distinct) {
	    super(null);
	    this.result = result;
	    this.distinct = distinct;
	}

	@Override
	public List<T> execute() {
	    return Entity.getDao(this.result).execute(this);
	}

	Class<T> getResultClass() {
	    return this.result;
	}

	boolean isDistinct() {
	    return this.distinct;
	}

	public FromClause<List<T>> from(final Class<? extends Entity<?>> source) {
	    return toChild(new FromClause<List<T>>(this, source));
	}

	public FromClause<List<T>> fromIt() {
	    return toChild(new FromClause<List<T>>(this, this.result));
	}
    }

    public static class UpdateClause<T extends Entity<?>> extends
	    Query<Integer> {
	private final Class<T> source;

	private UpdateClause(final Class<T> source) {
	    super(null);
	    this.source = source;
	}

	@Override
	public Integer execute() {
	    return Entity.getDao(this.source).execute(this);
	}

	Class<T> getSourceClass() {
	    return this.source;
	}

	public SetClause<T, Integer> set(final T valeur) {
	    return toChild(new SetClause<T, Integer>(this, valeur));
	}
    }

    public static class DeleteClause<T extends Entity<?>> extends
	    Query<Integer> {
	private final Class<T> cible;

	private DeleteClause(final Class<T> cible) {
	    super(null);
	    this.cible = cible;
	}

	@Override
	public Integer execute() {
	    return Entity.getDao(this.cible).execute(this);
	}
    }

    public static class WhereClause<R> extends Query<R> {
	private final String whereClause;

	private WhereClause(final Query<R> parent, final String whereClause) {
	    super(parent);
	    this.whereClause = whereClause;
	}

	String getClause() {
	    return this.whereClause;
	}
    }

    public static class CreateTableClause<T extends Entity<?>> extends
	    Query<Boolean> {
	private final Class<T> source;

	private CreateTableClause(final Class<T> source) {
	    super(null);
	    this.source = source;
	}

	Class<T> getSourceClass() {
	    return this.source;
	}

	@Override
	public Boolean execute() {
	    return Entity.getDao(this.source).createTable();
	}
    }

    public static class DropTableClause<T extends Entity<?>> extends
	    Query<Boolean> {
	private final Class<T> cible;
	private final boolean ifExists;

	private DropTableClause(final Class<T> cible, boolean ifexists) {
	    super(null);
	    this.cible = cible;
	    this.ifExists = ifexists;
	}

	Class<T> getSourceClass() {
	    return this.cible;
	}

	@Override
	public Boolean execute() {
	    if (ifExists) {
		return Entity.getDao(this.cible).dropTableIfExists();
	    } else {
		throw new UnsupportedOperationException();
	    }
	}
    }

    public static <T extends Entity<?>> CreateTableClause<T> createTable(
	    final Class<T> source) {
	return new CreateTableClause<T>(source);
    }

    public static <T extends Entity<?>> FromClause<List<T>> select(
	    final Class<T> result) {
	return new SelectClause<T>(result, false).fromIt();
    }

    public static <T extends Entity<?>> FromClause<List<T>> selectDistinct(
	    final Class<T> result) {
	return new SelectClause<T>(result, true).fromIt();
    }

    public static <T extends Entity<?>> UpdateClause<T> update(Class<T> classe) {
	return new UpdateClause<T>(classe);
    }

    public static <T extends Entity<?>> DeleteClause<T> delete(Class<T> classe) {
	return new DeleteClause<T>(classe);
    }

    public static <T extends Entity<?>> DropTableClause<T> dropTableIfExists(
	    final Class<T> cible) {
	return new DropTableClause<T>(cible, true);
    }
    
    public static <T extends Entity<T>> int delete(T objet) {
	return objet.getDao().remove(objet);
    }
    
    public static <T extends Entity<T>> int update(T objet) {
	return objet.getDao().update(objet);
    }

    public static List<Object> runBatch(List<Query> batch) {
	ArrayList<Object> result = new ArrayList<Object>(batch.size());
	// Do a more powerful implementation
	for (Query q : batch) {
	    result.add(q.execute());
	}
	return result;
    }

    public static <T extends Entity<?>> void updateAll(List<T> objet) {
	if (objet == null || objet.isEmpty()) {
	    return;
	}
	// Check if T == get(0).getClass()
	Class<T> classe = (Class<T>) objet.get(0).getClass();
	Entity.getDao(classe).updateAll(objet);
    }

    public static <T extends Entity<T>> List<T> search(final T modele) {
	return modele.getDao().search(modele);
    }

    public static <T extends Entity<T>> T search(Class<T> classe, int id) {
	try {
	    T modele = classe.newInstance();
	    modele.setId(id);
	    List<T> result = Queries.search(modele);
	    if (!result.isEmpty()) {
		return result.get(0);
	    } else {
		return null;
	    }
	} catch (Exception e) {
	    e.printStackTrace();
	    return null;
	}
    }
    
    public static void beginTran() throws SQLException {
	AccesBdd.getConnection().setAutoCommit(false);
    }
    
    public static void commit() throws SQLException {
	AccesBdd.getConnection().commit();
	AccesBdd.getConnection().setAutoCommit(true);
    }
    
    public static void rollback() throws SQLException {
	AccesBdd.getConnection().rollback();
	AccesBdd.getConnection().setAutoCommit(true);
    }
    
    public static void setUseCache(boolean use) {
	Queries.useCache = use;
    }

    public static <T extends Entity<T>> void insert(T objet) {
	objet.getDao().add(objet);
    }

    public static <T extends Entity<T>> void insertAll(List<T> objet) {
	if (objet == null || objet.isEmpty()) {
	    return;
	}
	// Check if T == get(0).getClass()
	objet.get(0).getDao().addAll(objet);
    }

    // TODO Disabled method for uniformity. Maybe later.
    // public static <T extends Entity<?>> SetClause<T, Integer> updateAndSet(T
    // value) {
    // return new UpdateClause<T>((Class<T>) value.getClass()).set(value);
    // }

    private Queries() {
    }
}
