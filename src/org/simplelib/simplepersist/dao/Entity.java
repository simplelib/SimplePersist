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

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.simplelib.simplepersist.annotation.Index;
import org.simplelib.simplepersist.annotation.QueryCache;
import org.simplelib.simplepersist.annotation.Table;

/**
 * Abstract class of all persistable entity.
 * 
 * @author simplelib.org
 * 
 * @param <T>
 *            type of the child class.
 */
public abstract class Entity<T extends Entity<T>> implements IPersistable<T> {
    class PrivilegedFieldsAccess implements PrivilegedAction<List<Field>> {
	@Override
	public List<Field> run() {
	    List<Field> liste = new ArrayList<Field>();
	    List<Field> result = new ArrayList<Field>();
	    // Récupération des Field de la Class et de ses SuperClass.
	    Class<?> currentClass = Entity.this.getClass();
	    while (currentClass != null) {
		liste.addAll(Arrays.asList(currentClass.getDeclaredFields()));
		currentClass = currentClass.getSuperclass();
	    }
	    // Identification des Field à mettre en persistance.
	    Field field;
	    // Class<?> fieldType;
	    // Important : inversion de l'ordre de parcours pour conserver les
	    // ID.
	    for (int i = liste.size() - 1; i >= 0; --i) {
		field = liste.get(i);
		int mod = field.getModifiers();
		if (!Modifier.isVolatile(mod) && !Modifier.isTransient(mod)
			&& !Modifier.isStatic(mod)) {
		    field.setAccessible(true);
		    result.add(field);
		    // fieldType = field.getType();
		}
	    }
	    return result;
	}
    }

    /**
     * Get the class Dao of the specified entity class.
     * 
     * @param classe
     *            Entity {@link Class}.
     * @return {@link IDao} linked.
     */
    @SuppressWarnings("unchecked")
    static <T extends Entity<?>> IDao<T> getDao(final Class<T> classe) {
	EntityDescriptor<?> result = getEntityDescriptor(classe);
	return (IDao<T>) result.DAO;
    }
    
    IDao<T> getDao() {
	return this.DESC.DAO;
    }

    private static EntityDescriptor<?> getEntityDescriptor(
	    Class<? extends Entity<?>> classe) {
	EntityDescriptor<?> result = MAP_DESC.get(classe);
	// Initialisation de la DAO si pas encore effectuée.
	if (result == null) {
	    try {
		classe.newInstance();
	    } catch (final Exception e) {
		throw new RuntimeException(e);
	    }
	    result = MAP_DESC.get(classe);
	}
	return result;
    }

    private static List<Field> getSubTableFields(final Field field) {
	List<Field> listeFieldSubT;
	@SuppressWarnings("unchecked")
	Class<? extends Entity<?>> castedClass = (Class<? extends Entity<?>>) field
		.getType();
	try {
	    listeFieldSubT = new ArrayList<Field>(
		    castedClass.newInstance().DESC.FIELD_LIST);
	    // Hack de suppression de l'id
	    listeFieldSubT.remove(0);
	} catch (final Exception e) {
	    throw new RuntimeException(e);
	}
	return listeFieldSubT;
    }

    static Field getField(Class<? extends Entity<?>> classe, String columnName) {
	EntityDescriptor<?> description = getEntityDescriptor(classe);
	for (Field field : description.FULL_FIELD_LIST) {
	    if (field.getName().equals(columnName)) {
		return field;
	    }
	}
	return null;
    }

    private static class EntityDescriptor<U extends Entity<?>> {
	protected final IDao<U> DAO;
	protected final List<Field> FIELD_LIST;
	protected final List<Field> FULL_FIELD_LIST;

	public EntityDescriptor(final IDao<U> dao, final List<Field> fieldList,
		final List<Field> fullFieldList) {
	    this.DAO = dao;
	    this.FIELD_LIST = fieldList;
	    this.FULL_FIELD_LIST = fullFieldList;
	}

    }

    /** Entity id in persistance. */
    @PrimaryKeyField
    private Integer id;

    private static final Map<Class<?>, EntityDescriptor<?>> MAP_DESC = new HashMap<Class<?>, EntityDescriptor<?>>();

    private final transient EntityDescriptor<T> DESC;

    private final transient PrivilegedFieldsAccess PRIV_FIELDS_ACCESS = new PrivilegedFieldsAccess();

    @SuppressWarnings("unchecked")
    protected Entity() {
	EntityDescriptor<T> desc;
	IDao<T> dao;
	List<Field> fieldList;
	List<Field> fullFieldList;

	// If first instantiation
	if (!MAP_DESC.containsKey(getClass())) {
	    // Field List
	    fieldList = AccessController.doPrivileged(this.PRIV_FIELDS_ACCESS);

	    // Full field list building
	    fullFieldList = new ArrayList<Field>(fieldList.size() * 2);
	    for (Field field : fieldList) {
		if (Entity.class.isAssignableFrom(field.getType())) {
		    fullFieldList.addAll(getSubTableFields(field));
		} else {
		    fullFieldList.add(field);
		}
	    }

	    // DAO
	    dao = createDAO(fullFieldList);
	    dao.init();

	    desc = new EntityDescriptor<T>(dao, fieldList, fullFieldList);

	    MAP_DESC.put(getClass(), desc);
	} else {
	    desc = (EntityDescriptor<T>) MAP_DESC.get(getClass());
	}
	this.DESC = desc;
    }

    private void addAllParamsFrom(final Field field, final List<Object> list)
	    throws IllegalArgumentException, IllegalAccessException,
	    InstantiationException {
	Class<?> fieldType = field.getType();
	if (Entity.class.isAssignableFrom(fieldType)) {
	    Entity<?> entite = (Entity<?>) field.get(this);
	    if (entite == null) {
		entite = (Entity<?>) fieldType.newInstance();
	    }
	    int offset = 1; // Id removing hack
	    List<Object> subTableParams = entite.getValues(offset);
	    list.addAll(subTableParams);
	} else {
	    boolean nullPrimitive = (fieldType == Integer.TYPE | fieldType == Character.TYPE)
		    && field.getInt(this) == 0
		    || fieldType == Double.TYPE
		    && field.getDouble(this) == 0
		    || fieldType == Boolean.TYPE
		    && field.getBoolean(this) == false;
	    if (nullPrimitive) {
		list.add(null);
	    } else {
		list.add(field.get(this));
	    }
	}
    }

    private IDao<T> createDAO(final List<Field> fullFieldList) {
	@SuppressWarnings("unchecked")
	Class<T> classe = (Class<T>) getClass();
	boolean queryCache = classe.isAnnotationPresent(QueryCache.class);
	if (classe.isAnnotationPresent(Table.class)) {
	    return new SqlDao<T>(queryCache, classe, fullFieldList);
	} else {
	    // TODO Utiliser des valeurs d'initialisation plus cohérentes que
	    // celles par défaut.
	    return new ObjectDao<T>(queryCache, 100, 2);
	}
    }

    @Override
    public int delete() {
	return Queries.update(getInstance());
    }

    private <A extends Annotation> List<Object> getAnnoted(
	    final Class<A> annotationClass) {
	List<Object> result = new ArrayList<Object>();
	try {
	    for (Field field : this.DESC.FIELD_LIST) {
		if (field.isAnnotationPresent(annotationClass)) {
		    addAllParamsFrom(field, result);
		}
	    }
	} catch (Exception e) {
	    throw new RuntimeException(e);
	}
	return result;
    }

    @Override
    public final Integer getId() {
	if (this.id == null) {
	    throw new IllegalStateException(
		    "This entity has not been inserted yet.");
	}
	return this.id;
    }

    /**
     * Return the values {@link List} of all fields annoted {@link Index}.
     * 
     * @return {@link List}&lt;{@link Object}&gt; : values {@link List}.
     */
    List<Object> getIndexs() {
	return getAnnoted(Index.class);
    }

    /**
     * Return the current instance under the child class. Avoid unchecked casts.
     */
    @SuppressWarnings("unchecked")
    protected final T getInstance() {
	return (T) this;
    }

    List<Object> getValues() {
	return getValues(0);
    }

    /**
     * Retourne l'ensemble des valeurs des membre de l'objet depuis l'offset
     * spécifié.
     * 
     * @param offset
     *            Offset de départ.
     * @return {@link List} des valeurs.
     */
    private List<Object> getValues(final int offset) {
	List<Object> result = new ArrayList<Object>();
	try {
	    Field field;
	    for (int i = offset, max = this.DESC.FIELD_LIST.size(); i < max; ++i) {
		field = this.DESC.FIELD_LIST.get(i);
		addAllParamsFrom(field, result);
	    }
	} catch (Exception e) {
	    throw new RuntimeException(e);
	}
	return result;
    }

    @Override
    public int hashCode() {
	return this.id.hashCode();
    }

    @Override
    public void insert() {
	Queries.insert(getInstance());
    }

    final boolean isInserted() {
	return this.id != null;
    }

    @Override
    public List<T> search() {
	return Queries.search(getInstance());
    }
    
    /**
     * Set the entity id.
     * 
     * @param id
     *            Entity id.
     */
    final void setId(final Integer id) {
	this.id = id;
    }

    int setObject(final ResultSet rs, final int offset, final boolean setId)
	    throws SQLException, IllegalArgumentException,
	    IllegalAccessException, InstantiationException {
	int o = offset;
	Entity<?> subTableObj;
	Field field;
	Class<?> fieldType;
	for (int i = setId ? 0 : 1, max = this.DESC.FIELD_LIST.size(); i < max; ++i) {
	    field = this.DESC.FIELD_LIST.get(i);
	    fieldType = field.getType();
	    // Support des subtables
	    if (Entity.class.isAssignableFrom(fieldType)) {
		subTableObj = (Entity<?>) fieldType.newInstance();
		field.set(this, subTableObj);
		o = subTableObj.setObject(rs, o, false);
		continue;
	    }
	    // Character support
	    if (fieldType == Character.class || fieldType == Character.TYPE) {
		String chaine = rs.getString(++o);
		if (chaine != null) {
		    field.set(this, chaine.charAt(0));
		    continue;
		} else if (fieldType == Character.TYPE) {
		    field.setChar(this, '\00');
		    continue;
		} else {
		    --o;
		}
	    }
	    Object result = rs.getObject(++o);
	    // Primitive support
	    if (fieldType.isPrimitive() && result == null) {
		if (fieldType == Boolean.TYPE) {
		    field.setBoolean(this, false);
		} else {
		    field.set(this, 0);
		}
	    } else {
		field.set(this, result);
	    }
	}
	return o;
    }

    @Override
    public int update() {
	return Queries.update(getInstance());
    }
}
