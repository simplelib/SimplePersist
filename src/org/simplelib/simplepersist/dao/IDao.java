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

import java.io.Serializable;
import java.util.List;

import org.simplelib.simplepersist.dao.Queries.Query;

/**
 * Interface d'un DAO.
 * 
 * @author simplelib.org
 * @param <T>
 *            Type de l'objet de la DAO.
 */
interface IDao<T extends Entity<?>> extends Serializable {

    /**
     * Insert an entity in database.
     * 
     * @param entity
     *            Entity to insert.
     * @return {@code 1} if success.
     */
    int add(T entity);

    /**
     * Insère un ensemble d'objet dans la persistance. Sous certaines
     * implémentations, cet appel peut offrir de meilleures performances que des
     * appels multiples à {@link IDao#add(Object)}.
     * 
     * @param objets
     *            Tableau d'{@link Object} à insérer.
     * @return Nb d'occurences ajoutées.
     */
    int addAll(List<T> objets);

    /**
     * Libère les ressources de la DAO.
     */
    void close();

    /** Créer la table dans la BDD. 
     * @return */
    boolean createTable();

    /** Supprime la table de la BDD si elle existe. 
     * @return */
    boolean dropTableIfExists();

    /**
     * Initialisation de la Dao.
     */
    void init();

    /**
     * Supprime toutes les occurences correspondants au modèle.
     * 
     * @param modele
     *            Modèle de recherche.
     * @return Nombre d'occurences supprimées.
     */
    int remove(T modele);

    /**
     * Recherche avancée en SGBD.
     * 
     * @param query
     *            Requête SQL.
     * @return {@link List} des résultats de la requête.
     */
    List<T> search(String query);

    /**
     * Requête avancée en SGBD avec requête objet.
     * 
     * @param query
     *            Requête Objet.
     * @return Résultat de la requête.
     */
    <R> R execute(Query<R> query);

    /**
     * Recherche toutes les occurences de l'objet correspondants au modèle.
     * 
     * @param modele
     *            Modèle de recherche.
     * @return Liste des objets correspondants aux critères de recherche.
     */
    List<T> search(T modele);

    /**
     * Met à jour l'objet passé en paramètre.
     * 
     * @param objet
     *            Objet à mettre à jour.
     * @return Nombre d'objets mis à jours.
     */
    int update(T objet);

    /**
     * Met à jour un ensemble d'objets. Sous certaines implémentations, cet
     * appel peut offrir de meilleures performances que des appels multiples à
     * {@link IDao#update(Object)}.
     * 
     * @param objets
     *            Tableau d'{@link Object} à mettre à jour.
     * @return Nb d'occurences mises à jour.
     */
    int updateAll(List<T> objets);
}
