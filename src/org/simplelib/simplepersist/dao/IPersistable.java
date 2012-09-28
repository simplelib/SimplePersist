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

/**
 * Interface commune aux classes persistables.
 * 
 * @author simplelib.org
 * 
 * @param <T>
 */
public interface IPersistable<T> {
    /**
     * Suppression de l'entité de la persistance.
     * 
     * @return Nb d'occurence supprimées.
     */
    int delete();

    /**
     * Identifiant en persistance de l'entité.
     * 
     * @return {@link Integer} : identifiant.
     */
    Integer getId();

    /**
     * Insertion de l'entité dans la persistance.
     */
    void insert();

    /**
     * Recherche de toutes les entités en persistance correspondant à cette
     * instance.
     * 
     * @return {@link List} des entités trouvées.
     */
    List<T> search();

    /**
     * Mise à jour de l'entité dans la persistance.
     * 
     * @return Nb occurences mises à jour.
     */
    int update();
}