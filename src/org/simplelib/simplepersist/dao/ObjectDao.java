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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

/**
 * Implémentation d'une DAO volatile en java. Celle-ci est utilisée en l'absence
 * de persistance basée sur un SGBD. Elle procure nottament de meilleures
 * performances. En utilisant une interface commune, elle permet une
 * implémentation semblable des entités avec ou sans persistance.
 * 
 * Dans cette DAO, aucun accès à une base de données n'est requis.
 * 
 * @author simplelib.org
 * 
 * @param <T>
 */
class ObjectDao<T extends Entity<?>> extends AbstractDao<T> {
    /** Generated serial version UID. */
    private static final long serialVersionUID = 1040296095680082863L;
    /** Index du dernier objet inséré. */
    protected int index = 1;
    /** Liste conteneur des objets de la DAO. */
    protected final Map<Integer, T> conteneur;
    /** Taille initiale du conteneur. */
    private final int INIT_CAPACITY;
    /** Nombre d'indexs attendus du conteneur. */
    private final int NB_INDEX;
    /** Map des autres indexs */
    private final Map<Object, Map<Integer, T>> indexMap;
    /** Active/Désactive l'utilisationn des indexs. */
    private static final boolean USE_INDEX = true;
    /** Tri automatique des requêtes. */
    private static final boolean SORT_ON_SEARCH = true;

    /**
     * Construction d'une Dao purement Java.
     * @param queryCache 
     * @param initCap 
     * @param nbIndex 
     */
    @SuppressWarnings("unchecked")
    public ObjectDao(boolean queryCache, int initCap, int nbIndex) {
	super(queryCache);
	ObjectInputStream is;
	File fDao = new File(this.getClass().getName());
	INIT_CAPACITY = initCap;
	NB_INDEX = nbIndex;
	if (saveOnExit() && fDao.exists()) {
	    try {
		is = new ObjectInputStream(new FileInputStream(fDao));
		ObjectDao<T> dao = (ObjectDao<T>) is.readObject();
		this.conteneur = dao.conteneur;
		this.indexMap = dao.indexMap;
		this.index = dao.index;
		is.close();
	    } catch (final Exception e) {
		e.printStackTrace();
		throw new RuntimeException(
			"Erreur lors de la lecture de la DAO.");
	    }
	} else {
	    this.conteneur = new HashMap<Integer, T>(INIT_CAPACITY);
	    this.indexMap = new IdentityHashMap<Object, Map<Integer, T>>(
		    NB_INDEX);
	}

    }

    @Override
    public int add(final T objet) {
	unCache();
	objet.setId(this.index);
	this.conteneur.put(this.index, objet);

	// On complète les indexs
	if (USE_INDEX) {
	    List<Object> listIndexs = objet.getIndexs();
	    Map<Integer, T> mapValues;
	    for (Object index : listIndexs) {
		mapValues = this.indexMap.get(index);
		if (mapValues == null) {
		    mapValues = new HashMap<Integer, T>(INIT_CAPACITY
			    / NB_INDEX);
		    this.indexMap.put(index, mapValues);
		}
		mapValues.put(this.index, objet);
	    }
	}

	++this.index;
	return 1;
    }

    @Override
    public void close() {
	if (!saveOnExit()) {
	    return;
	}
	ObjectOutputStream os;
	try {
	    os = new ObjectOutputStream(new FileOutputStream(this.getClass()
		    .getName()));
	    os.writeObject(this);
	    os.close();
	} catch (final Exception e) {
	    throw new RuntimeException(
		    "Object writing error.", e);
	}
    }

    /**
     * Permet la suppression d'un objet en fonction de son index et non du test
     * d'égalité .equals().
     * 
     * @param liste
     *            Liste contenant l'objet.
     * @param o
     *            Objet à supprimer.
     * @return vrai si suppression.
     */
    @SuppressWarnings("unused")
    private boolean idRemove(final List<T> liste, final T o) {
	int size = liste.size();
	for (int index = 0; index < size; index++) {
	    if (o.getId().intValue() == liste.get(index).getId().intValue()) {
		liste.remove(index);
		return true;
	    }
	}
	return false;
    }

    @Override
    public int remove(final T modele) {
	unCache();
	this.conteneur.remove(modele.getId());

	// On supprime des indexs
	if (USE_INDEX) {
	    List<Object> listIndexs = modele.getIndexs();
	    Map<Integer, T> mapValues;
	    for (Object index : listIndexs) {
		mapValues = this.indexMap.get(index);
		if (mapValues == null) {
		    throw new IllegalStateException(
			    "L'index ne peut être null.");
		}
		mapValues.remove(modele.getId());
	    }
	}
	return 1;
    }

    /**
     * Sauvegarde de la Dao dans un fichier à la fermeture ?
     * 
     * @return <code>true</code> si la sauvegarde doit être effectuée.
     */
    protected boolean saveOnExit() {
	return false;
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<T> search(final T modele) {
	if (isInCache(modele)) {
	    return getCache(modele);
	}

	List<T> result = new ArrayList<T>((this.conteneur.size() + 1) / 2);
	List<Object> valuesModele = modele.getValues();
	List<Object> valuesObjet;
	Collection<T> ensemble;

	// En cas de recherche sur l'id, on effectue une recherche rapide.
	if (modele.isInserted()) {
	    T objetTrouve = this.conteneur.get(modele.getId());
	    ensemble = new ArrayList<T>(1);
	    ensemble.add(objetTrouve);
	} else {
	    // Sinon, on restreint l'espace de recherche grace à l'indexs le
	    // plus restrictif.
	    ensemble = this.conteneur.values();
	    if (USE_INDEX) {
		Collection<T> ensembleIdx = Collections.emptyList();
		for (Object index : modele.getIndexs()) {
		    if (index != null) {
			Map<Integer, T> indexMap = this.indexMap.get(index);
			if (indexMap == null) {
			    ensembleIdx = Collections.emptyList();
			} else {
			    ensembleIdx = this.indexMap.get(index).values();
			}
			if (ensembleIdx.size() < ensemble.size()) {
			    ensemble = ensembleIdx;
			}
		    }
		}
	    }
	}

	ListIterator<Object> itM;
	ListIterator<Object> itO;
	Object paramModele;
	Object paramObjet;
	boolean matche;

	// Pour chaque objet de l'ensemble de recherche.
	for (T obj : ensemble) {
	    matche = true;
	    valuesObjet = obj.getValues();

	    itM = valuesModele.listIterator();
	    itO = valuesObjet.listIterator();

	    // Chaque valeur du modele correspond aux valeurs de l'objet ?
	    while (itM.hasNext() && itO.hasNext()) {
		paramModele = itM.next();
		paramObjet = itO.next();

		if ((paramModele != null) && !paramModele.equals(paramObjet)) {
		    matche = false;
		    break;
		}
	    }
	    if (matche) {
		result.add(obj);
	    }
	}
	if (SORT_ON_SEARCH) {
	    // Si la classe le permet, on trie la liste.
	    if (modele instanceof Comparable<?>) {
		Object[] a = result.toArray();
		Arrays.sort(a);
		ListIterator<T> i = result.listIterator();
		for (Object element : a) {
		    i.next();
		    i.set((T) element);
		}
	    }
	}

	toCache(modele, result);

	return result;
    }

    @Override
    public int update(final T objet) {
	unCache();

	this.conteneur.put(objet.getId(), objet);
	return 1;
    }
}
