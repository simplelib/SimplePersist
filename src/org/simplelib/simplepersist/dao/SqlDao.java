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

import java.io.IOException;
import java.lang.reflect.Field;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.simplelib.simplepersist.annotation.Column;
import org.simplelib.simplepersist.annotation.Index;
import org.simplelib.simplepersist.annotation.PrimaryKey;
import org.simplelib.simplepersist.annotation.Table;
import org.simplelib.simplepersist.dao.Queries.FromClause;
import org.simplelib.simplepersist.dao.Queries.JoinClause;
import org.simplelib.simplepersist.dao.Queries.OnClause;
import org.simplelib.simplepersist.dao.Queries.Query;
import org.simplelib.simplepersist.dao.Queries.SelectClause;
import org.simplelib.simplepersist.dao.Queries.WhereClause;
import org.simplelib.simplepersist.dao.TableDescription.ColumnDescription;
import org.simplelib.simplepersist.dao.TableDescription.SqlProp;

/**
 * Classe abstraite générique des DAO implémentant {@link IDao}. De plus des
 * méthodes de SELECT, INSERT, UPDATE et DELETE génériques sont définies ici
 * ainsi que des méthodes protégées que les DAOs doivent implémenter pour les
 * faire fonctionner.
 * 
 * Dans cet DAO chaque accès requiert un accès à la base de donnée.
 * 
 * @author simplelib.org
 * @version 1.0b
 * @param <T>
 *            Interface publique de l'objet métier géré.
 */
class SqlDao<T extends Entity<?>> extends AbstractDao<T> {
    /** Generated serial version UID. */
    private static final long serialVersionUID = 4669398660990297631L;
    /** Acces au logger de la DAO. */
    static final Logger LOGGER = Logger.getLogger("dao");

    /**
     * Créer la clause de tri de la table.
     * 
     * @param order
     *            Colonne de tri.
     * @return Clause ORDER BY.
     */
    private static String createOrderClause(final String order) {
	if (order != "") {
	    return " ORDER BY " + order;
	} else {
	    return "";
	}
    }

    private static TableDescription getTableDesc(final List<Field> fieldListe,
	    final Class<? extends Entity<?>> classe) {
	PrimaryKey primAn = classe.getAnnotation(PrimaryKey.class);
	if (classe.isAnnotationPresent(Table.class)) {
	    List<ColumnDescription> columns = new ArrayList<ColumnDescription>(
		    fieldListe.size());
	    Column colAn;
	    String name = "";
	    SqlProp sqlProp = null;
	    boolean index;
	    boolean primaryKey;

	    for (Field field : fieldListe) {
		colAn = field.getAnnotation(Column.class);
		index = field.getAnnotation(Index.class) != null;
		primaryKey = field.isAnnotationPresent(PrimaryKeyField.class);

		name = getColumnName(field, classe);

		// Colonne clé primaire
		if (primaryKey && (primAn != null)) {
		    assert (classe == Entity.class);
		    // sqlProp = SqlProp.valueOf("INTEGER");
		}
		// Colonne décrite explicitement
		else {
		    if (field.isAnnotationPresent(Column.class)) {
			sqlProp = SqlProp.valueOf(colAn.type());
		    }
		    // Colonne auto-générée
		    else {
			sqlProp = SqlProp.valueOf(field.getType());
		    }
		}
		columns.add(new ColumnDescription(field, name, sqlProp, index,
			primaryKey));
	    }
	    return new TableDescription(getTableName(classe),
		    columns.toArray(new ColumnDescription[0]), classe);
	} else {
	    IllegalArgumentException e = new IllegalArgumentException(
		    "Tag Sql non renseigné.");
	    LOGGER.severe(e.getMessage());
	    throw e;
	}
    }

    /** Requete SELECT. */
    private final String SELECT;
    /** Jointure NULL. */
    private final String NULL_JOIN;
    /** Requete DELETE. */
    private final String DELETE;
    /** Requete INSERT. */
    private final String INSERT;
    /** Requete UPDATE. */
    private final String UPDATE;
    /** Clause ORDER BY. */
    private final String ORDER;

    /** Le PreparedStatement de suppression d'un objet. */
//    private final PreparedStatement DELETE_STATEMENT;

    /** Nom de la table associée en BDD. */
    private final TableDescription TABLE_DESC;

    /**
     * Construction d'une Dao de type SQL.
     * 
     * @param queryCache
     *            Utilisation du cache de requêtes.
     * @param classe
     *            {@link Class} à persister.
     * @param fieldList
     *            {@link List} des {@link Field} de la {@link Class}.
     */
    SqlDao(boolean queryCache, final Class<T> classe,
	    final List<Field> fieldList) {
	super(queryCache);
	this.TABLE_DESC = getTableDesc(fieldList, classe);

	try {
	    // SELECT
	    StringBuilder selectBuild = new StringBuilder();
	    selectBuild.append("SELECT ");
	    selectBuild.append(this.TABLE_DESC.COLUMNS[0].NAME); // ID
	    selectBuild.append(", ");
	    appendColumnClause(selectBuild);
	    this.SELECT = selectBuild.toString();

	    // NULL JOIN
	    this.NULL_JOIN = " FROM " + this.TABLE_DESC.NAME;

	    // INSERT
	    StringBuilder insertBuild = new StringBuilder();
	    insertBuild.append("INSERT INTO ");
	    insertBuild.append(this.TABLE_DESC.NAME);
	    insertBuild.append(" (");
	    appendColumnClause(insertBuild);
	    insertBuild.append(")\nVALUES ");
	    this.INSERT = insertBuild.toString();
	} catch (final Exception e) {
	    LOGGER.severe("Erreur lors de l'initialisation des requêtes SQL.");
	    throw new RuntimeException(e);
	}

	this.UPDATE = "UPDATE " + this.TABLE_DESC.NAME + " SET ";
	this.DELETE = "DELETE FROM " + this.TABLE_DESC.NAME + " WHERE id=?;";

	this.ORDER = createOrderClause(""); // TODO Ajouter paramètre.
    }

    @Override
    public int add(final T o) {
	unCache();
	int res = 0;
	PreparedStatement pstmt = null;
	final List<Object> values = o.getValues();
	StringBuilder reqBuilder = new StringBuilder();

	try {
	    reqBuilder.append(this.INSERT);
	    appendValuesClause(values, reqBuilder);
	    pstmt = AccesBdd.getPreparedStatementWithGenKeys(reqBuilder
		    .toString());

	    prepareStatement(pstmt, values, 1);
	    LOGGER.log(Level.FINEST, pstmt.toString());
	    res = pstmt.executeUpdate();
	    if (res == 1) {
		ResultSet rs = pstmt.getGeneratedKeys();
		rs.next();
		o.setId(rs.getInt(1));
	    }
	} catch (Exception e) {
	    LOGGER.severe("Echec de lors de l'insertion des données.");
	    throw new RuntimeException(e);
	} finally {
	    AccesBdd.closeStatement(pstmt);
	}
	return res;
    }

    /**
     * Implémentation avancée de l'insertion multiple. Offre généralement de
     * meilleures performances que de multiples appels à
     * {@link SqlDao#add(Entity)}.
     */
    @Override
    public int addAll(final List<T> objets) {
	unCache();
	PreparedStatement pstmt = null;
	StringBuilder reqBuilder = new StringBuilder();
	List<Object> values;
	@SuppressWarnings("unchecked")
	List<Object>[] valuesBuffer = new List[objets.size()];
	int result = 0;

	try {
	    T o; // Objet courant

	    reqBuilder.append(this.INSERT);
	    boolean first = true;
	    for (int i = 0; i < objets.size(); ++i) {
		o = objets.get(i);
		if (!first) {
		    reqBuilder.append(",\n");
		} else {
		    first = false;
		}
		values = o.getValues();
		valuesBuffer[i] = values;
		appendValuesClause(values, reqBuilder);
	    }

	    pstmt = AccesBdd.getPreparedStatementWithGenKeys(reqBuilder
		    .toString());

	    int offset = 1;
	    for (int i = 0; i < objets.size(); ++i) {
		o = objets.get(i);
		values = valuesBuffer[i];
		offset = prepareStatement(pstmt, values, offset);
	    }

	    result = pstmt.executeUpdate();
	    for (int i = 0; i < result; ++i) {
		o = objets.get(i);
		ResultSet rs = pstmt.getGeneratedKeys();
		rs.next();
		o.setId(rs.getInt(1));
	    }
	} catch (Exception e) {
	    LOGGER.severe("Echec de lors de l'insertion des données.");
	    throw new RuntimeException(e);
	} finally {
	    AccesBdd.closeStatement(pstmt);
	}
	return result;
    }

    /**
     * Construction de la liste des colonne.
     * 
     * @param seq
     *            {@link Appendable} séquence de sortie.
     * @throws IOException
     *             Erreur de sortie.
     */
    protected void appendColumnClause(final Appendable seq) throws IOException {
	boolean first = true;
	for (int i = 1; i < this.TABLE_DESC.COLUMNS.length; ++i) {
	    ColumnDescription col = this.TABLE_DESC.COLUMNS[i];
	    if (!first) {
		seq.append(", ");
	    } else {
		first = false;
	    }
	    seq.append(col.NAME);
	}
    }

    /**
     * Créer les paramètres des requêtes INSERT en fonctions de la liste des
     * paramètres du modèle.
     * 
     * @param columns
     *            Liste des colonnes de la table.
     * @return Paramètres de la requête.
     * @throws IOException
     */
    protected void appendValuesClause(final List<Object> params,
	    final Appendable seq) throws IOException {
	seq.append('(');
	boolean first = true;
	for (int i = 1; i < this.TABLE_DESC.COLUMNS.length; ++i) {
	    if (!first) {
		seq.append(", ");
	    } else {
		first = false;
	    }
	    if (params.get(i) != null) {
		seq.append('?');
	    } else {
		seq.append("null");
	    }
	}
	seq.append(')');
    }

    /**
     * Libération des ressources de la DAO. Fermeture de tous les statements.
     */
    @Override
    public void close() {
	// DO NOTHING
//	try {
//	    this.DELETE_STATEMENT.close();
//	} catch (SQLException e) {
//	    LOGGER.severe("Echec de la fermeture de la DAO.");
//	    throw new RuntimeException(e);
//	}
    }

    /**
     * Construit le corps d'un WHERE en fonction d'un tableau de clauses fourni
     * en paramètre.
     * 
     * @param clauses
     *            Le tableau de clause.
     * @param builder
     *            StringBuilder.
     * @return Le corps du WHERE.
     */
    protected String appendWhereClause(final List<String> clauses,
	    final StringBuilder builder) {
	builder.append(" WHERE ");
	if (clauses.size() > 0) {
	    builder.append(clauses.get(0));
	    for (int i = 1; i < clauses.size(); i++) {
		builder.append(" AND ");
		builder.append(clauses.get(i));
	    }
	} else {
	    builder.append("1=1"); // Tout
	}
	builder.append(this.ORDER);
	return builder.toString();
    }

    /**
     * Créer les paramètres des requêtes UPDATE en fonctions de la liste des
     * colonnes de la table.
     * 
     * @param columns
     *            Liste des colonnes de la table.
     * @return Paramètres de la requête.
     */
    protected String createSetClause(final List<String> clauses) {
	StringBuilder clause = new StringBuilder();
	if (clauses.size() > 0) {
	    clause.append(clauses.get(0));
	}
	for (int i = 1; i < clauses.size(); i++) {
	    clause.append(", ");
	    clause.append(clauses.get(i));
	}
	return clause.toString();
    };

    @Override
    public boolean createTable() {
	StringBuilder query = new StringBuilder();
	query.append("CREATE TABLE ");
	query.append(this.TABLE_DESC.NAME);
	query.append(" (\n");

	try {
	    ColumnDescription primary = null;
	    for (ColumnDescription col : this.TABLE_DESC.COLUMNS) {
		if (col.PRIMARY_KEY) {
		    primary = col;
		    AccesBdd.currentDb().getType()
			    .appendAutomaticKey(query, col.NAME);
		} else {
		    query.append(col.NAME);
		    query.append(' ');
		    query.append(col.SQL_PROP.toString());
		}
		query.append(",\n");
	    }
	    query.append("PRIMARY KEY (");
	    query.append(primary.NAME);
	    query.append(")\n)");

	    Statement stmt = AccesBdd.getStatement();
	    LOGGER.finest(query.toString());
	    stmt.addBatch(query.toString());
	    stmt.executeBatch();
	    stmt.close();
	} catch (final Exception e) {
	    LOGGER.severe("Erreur SQL lors de la création de la table : "
		    + this.TABLE_DESC.NAME);
	    throw new RuntimeException(e);
	}
	return true;
    }

    @Override
    public boolean dropTableIfExists() {
	StringBuilder query = new StringBuilder();
	query.append("DROP TABLE IF EXISTS ");
	query.append(this.TABLE_DESC.NAME);
	query.append("");
	Statement stmt = AccesBdd.getStatement();
	try {
	    LOGGER.finest(query.toString());
	    stmt.addBatch(query.toString());
	    stmt.executeBatch();
	} catch (final SQLException e) {
	    LOGGER.severe("Erreur SQL lors de la suppression de la table : "
		    + this.TABLE_DESC.NAME);
	    throw new RuntimeException(e);
	}
	return true;
    }

    /** Extraction des entités d'un {@link ResultSet}. */
    @SuppressWarnings("unchecked")
    private List<T> extract(final ResultSet rs) throws SQLException,
	    InstantiationException, IllegalAccessException {
	List<T> res = new ArrayList<T>();
	T t;
	while (rs.next()) {
	    t = (T) this.TABLE_DESC.CLASS.newInstance();
	    t.setObject(rs, 0, true);
	    res.add(t);
	}
	return res;
    }

    @Override
    protected void finalize() throws Throwable {
	try {
	    close();
	} finally {
	    super.finalize();
	}
    }

    /**
     * Créer et renvoie le tableau des clauses utilisable dans un
     * PreparedStatement à partir des attributs renseignés dans le modèle. Plus
     * compliqué que d'utiliser directement un Statement mais évite les
     * injections SQL !
     * 
     * @param o
     *            Le modèle de recherche.
     * @return Les clauses du PreparedStatement.
     */
    protected List<String> getClauses(final List<Object> params,
	    boolean includeNull) {
	final List<String> clauses = new ArrayList<String>();

	for (int i = 0; i < params.size(); ++i) {
	    if (params.get(i) != null) {
		clauses.add(this.TABLE_DESC.COLUMNS[i].NAME + "=?");
		LOGGER.finer(this.TABLE_DESC.COLUMNS[i].NAME
			+ " has a null value : ignored.");
	    } else if (includeNull) {
		clauses.add(this.TABLE_DESC.COLUMNS[i].NAME + "=null");
	    }
	}
	return clauses;
    }

    // @Override
    // public void init() {
    // super.init();
    // // dropTableIfExists();
    // // createTable();
    // }

    /**
     * Preparing statement from {@link List} parameters.
     * 
     * @param pstmt
     *            The {@link PreparedStatement}.
     * @param params
     *            Parameters.
     * @param offset
     *            Specify an offset to ignore {@code offset} first fields.
     * @return Next column index in the statement.
     */
    protected int prepareStatement(final PreparedStatement pstmt,
	    final List<Object> params, final int offset) {
	int i = offset;
	try {
	    for (Object param : params) {
		if (param != null) {
		    if(param instanceof Character) {
			// HACK MYSQL
			pstmt.setString(i++, ((Character) param).toString());
		    } else {
			pstmt.setObject(i++, param);
		    }
		}
	    }
	} catch (SQLException e) {
	    LOGGER.severe("Preparation of statement failed.");
	    throw new RuntimeException(e);
	}
	return i;
    }

    @Override
    public int remove(final T o) {
	unCache();
	try {
	    PreparedStatement stmt = AccesBdd.getPreparedStatementWithGenKeys(this.DELETE);
	    stmt.setInt(1, o.getId());
	    LOGGER.finest(stmt.toString());
	    int r = stmt.executeUpdate();
	    stmt.close();
	    return r;
	} catch (SQLException e) {
	    LOGGER.severe("Removal failed.");
	    throw new RuntimeException(e);
	}
    }

    private <R> R select(final Query<R> query) {
	Object cache = query.getCache();
	if (cache != null) {
	    // return (R) sqlSearch((String) cache);
	}
	StringBuilder clauseBuilder = new StringBuilder(32);
	clauseBuilder.append("SELECT ");
	if (((SelectClause<?>) query).isDistinct()) {
	    clauseBuilder.append("DISTINCT ");
	}
	clauseBuilder.append(this.TABLE_DESC.COLUMNS[0].NAME); // ID
	clauseBuilder.append(", ");
	try {
	    appendColumnClause(clauseBuilder);
	} catch (IOException e) {
	    LOGGER.severe(e.getMessage());
	    throw new RuntimeException(e);
	}
	Query<?> currentQ = query.getChild();
	while (currentQ != null) {
	    if (currentQ instanceof FromClause) {
		currentQ = appendFromClause(currentQ, clauseBuilder);
	    } else if (currentQ instanceof JoinClause) {
		currentQ = appendJoinClause(currentQ, clauseBuilder);
	    } else if (currentQ instanceof WhereClause) {
		currentQ = appendWhereClause(currentQ, clauseBuilder);
	    } else {
		LOGGER.warning("Clause non supportée. Ignorée.");
		currentQ = currentQ.getChild();
	    }
	}
	String clause = clauseBuilder.toString();
	query.setCache(clause);

	@SuppressWarnings("unchecked")
	R result = (R) sqlSearch(clause);
	return result;
    }

    @Override
    public <R> R execute(final Query<R> query) {
	if (query instanceof SelectClause) {
	    return select(query);
	}
	throw new UnsupportedOperationException(
		"Execution de ce type de requête non supporté par cette DAO.");
    }

    @Override
    public List<T> search(String query) {
	StringBuilder clauseBuilder = new StringBuilder(32);
	clauseBuilder.append(this.SELECT);
	final String CLAUSE;
	clauseBuilder.append(this.NULL_JOIN);
	query = query.trim();
	String upperCaseQuery = query.toUpperCase();
	if (!query.isEmpty() && !upperCaseQuery.startsWith("WHERE ")
		&& !upperCaseQuery.startsWith("ORDER ")
		&& !upperCaseQuery.startsWith("LIMIT ")
		&& !upperCaseQuery.startsWith("GROUP ")
		&& !upperCaseQuery.startsWith("OFFSET ")) {
	    clauseBuilder.append(" WHERE ");
	}
	clauseBuilder.append(query);
	CLAUSE = clauseBuilder.toString();
	return sqlSearch(CLAUSE);
    }

    private List<T> sqlSearch(String clause) {
	Statement stmt = AccesBdd.getStatement();
	final List<T> res;
	LOGGER.log(Level.FINEST, clause);
	try {
	    ResultSet rs = stmt.executeQuery(clause);
	    res = extract(rs);
	} catch (Exception e) {
	    LOGGER.severe("L'execution de la requête a échoué : " + clause);
	    throw new RuntimeException(e);
	} finally {
	    // On ferme le statement
	    AccesBdd.closeStatement(stmt);
	}
	return res;
    }

    @Override
    public List<T> search(final T o) {
	if (isInCache(o)) {
	    return getCache(o);
	}

	final List<Object> values = o.getValues();
	StringBuilder clauseBuilder = new StringBuilder();
	clauseBuilder.append(this.SELECT);
	clauseBuilder.append(this.NULL_JOIN);
	appendWhereClause(getClauses(values, false), clauseBuilder);
	String clause = clauseBuilder.toString();
	final PreparedStatement select = AccesBdd.getPreparedStatement(clause);
	final List<T> res;

	LOGGER.log(Level.FINEST, clause);
	try {
	    prepareStatement(select, values, 1);
	    ResultSet rs = select.executeQuery();
	    res = extract(rs);
	} catch (Exception e) {
	    LOGGER.severe("L'execution de la requête a échoué : " + clause);
	    throw new RuntimeException(e);
	} finally {
	    // On ferme le statement
	    AccesBdd.closeStatement(select);
	}

	toCache(o, res);
	return res;
    }

    @Override
    public int update(final T o) {
	unCache();
	final List<Object> values = o.getValues();
	final PreparedStatement pstmt = AccesBdd
		.getPreparedStatementWithGenKeys(this.UPDATE
			+ createSetClause(getClauses(values, true)) + " WHERE "
			+ this.TABLE_DESC.COLUMNS[0].NAME + "=?;");

	int i = prepareStatement(pstmt, values, 1);
	try {
	    if (o.getId() != null) {
		pstmt.setInt(i, o.getId());
	    }
	    LOGGER.finest(pstmt.toString());
	    return pstmt.executeUpdate();
	} catch (SQLException e) {
	    LOGGER.severe("Echec lors de la mise à jour des données.");
	    throw new RuntimeException(e);
	} finally {
	    AccesBdd.closeStatement(pstmt);
	}
    }

    private static String getTableName(Class<? extends Entity<?>> classe) {
	Table tableAn = classe.getAnnotation(Table.class);
	return tableAn == null ? classe.getName() : tableAn.name();
    }

    private static String getColumnName(Field field,
	    Class<? extends Entity<?>> classe) {
	boolean primaryKey = field.isAnnotationPresent(PrimaryKeyField.class);
	PrimaryKey primAn = classe.getAnnotation(PrimaryKey.class);
	if (primaryKey && primAn != null) {
	    return primAn.name();
	}
	Column colAn = field.getAnnotation(Column.class);
	return colAn == null ? field.getName() : colAn.name();
    }

    private static Query<?> appendFromClause(Query<?> query,
	    StringBuilder builder) {
	// if (!(query instanceof FromClause)) {
	// throw new IllegalArgumentException("Type de requête incorrecte.");
	// }
	FromClause<?> fromClause = (FromClause<?>) query;
	builder.append(" FROM ");
	builder.append(getTableName(fromClause.getSourceClass()));
	return fromClause.getChild();
    }

    private static Query<?> appendWhereClause(Query<?> query,
	    StringBuilder builder) {
	// if (!(query instanceof WhereClause)) {
	// throw new IllegalArgumentException("Type de requête incorrecte.");
	// }
	WhereClause<?> whereClause = (WhereClause<?>) query;
	builder.append(" WHERE ");
	// TODO check de l'expression SQL.
	builder.append(whereClause.getClause());
	return whereClause.getChild();
    }

    private static Query<?> appendJoinClause(Query<?> query,
	    StringBuilder builder) {
	// if (!(query instanceof JoinClause)) {
	// throw new IllegalArgumentException("Type de requête incorrecte.");
	// }
	Query<?> currentQ = query;
	JoinClause<?> currentJ;
	do {
	    currentJ = (JoinClause<?>) currentQ;
	    switch (currentJ.getType()) {
	    case INNER:
		builder.append(" INNER");
		break;
	    case LEFT:
		builder.append(" LEFT");
		break;
	    case RIGHT:
		builder.append(" RIGHT");
		break;
	    }
	    builder.append(" JOIN ");
	    builder.append(getTableName(currentJ.getJoinedClass()));
	    currentQ = currentQ.getChild();
	} while (currentQ instanceof JoinClause);

	OnClause<?> currentO;
	if (currentQ instanceof OnClause) {
	    currentO = (OnClause<?>) currentQ;
	    builder.append(" ON ");
	    // TODO check de l'expression SQL.
	    builder.append(currentO.getClause());
	    currentQ = currentQ.getChild();
	}

	return currentQ;
    }
}
