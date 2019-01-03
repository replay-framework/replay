package play.db.jpa;

import play.db.Configuration;
import play.db.jpa.GenericModel.JPAQuery;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.persistence.EntityManager;
import javax.persistence.Query;
import java.util.List;
import java.util.Map;

public class JPQL {

    @Nonnull
    private EntityManager em(String dbName) {
        return JPA.em(dbName);
    }

    long count(String dbName, String entity) {
        return Long.parseLong(em(dbName).createQuery("select count(*) from " + entity + " e").getSingleResult().toString());
    }

    public long count(String entity, String query, Object[] params) {
        return count(JPA.DEFAULT, entity, query, params);
    }

    long count(String dbName, String entity, String query, Object[] params) {
        return Long.parseLong(
                bindParameters(em(dbName).createQuery(
                createCountQuery(dbName, entity, query, params)), params).getSingleResult().toString());
    }

    @Nonnull
    <T extends JPABase> List<T> findAll(String dbName, String entity) {
        return em(dbName).createQuery("select e from " + entity + " e").getResultList();
    }

    @Nullable
    JPABase findById(String dbName, String entity, Object id) throws Exception {
        return (JPABase) em(dbName).find(Class.forName(entity), id);
    }

    @Nonnull
    JPAQuery find(String dbName, String entity, String query, Object[] params) {
        Query q = em(dbName).createQuery(
                createFindByQuery(dbName, entity, query, params));
        return new JPAQuery(
                createFindByQuery(dbName, entity, query, params), bindParameters(q, params));
    }

    int delete(String dbName, String entity, String query, Object[] params) {
        Query q = em(dbName).createQuery(
                createDeleteQuery(entity, query, params));
        return bindParameters(q, params).executeUpdate();
    }

    int deleteAll(String dbName, String entity) {
        Query q = em(dbName).createQuery(
                createDeleteQuery(entity, null));
        return bindParameters(q).executeUpdate();
    }

    @Nonnull
    private String createFindByQuery(@Nonnull String dbName, @Nonnull String entityName, String query, Object... params) {
        if (query == null || query.trim().isEmpty()) {
            return "from " + entityName;
        }
        if (query.matches("^by[A-Z].*$")) {
            return "from " + entityName + " where " + findByToJPQL(dbName, query);
        }
        if (query.trim().toLowerCase().startsWith("select ")) {
            return query;
        }
        if (query.trim().toLowerCase().startsWith("from ")) {
            return query;
        }
        if (query.trim().toLowerCase().startsWith("order by ")) {
            return "from " + entityName + " " + query;
        }
        if (query.trim().indexOf(' ') == -1 && query.trim().indexOf('=') == -1 && params != null && params.length == 1) {
            query += " = ?1";
        }
        if (query.trim().indexOf(' ') == -1 && query.trim().indexOf('=') == -1 && params == null) {
            query += " = null";
        }
        return "from " + entityName + " where " + query;
    }

    @Nonnull
    private String createDeleteQuery(@Nonnull String entityName, String query, Object... params) {
        if (query == null) {
            return "delete from " + entityName;
        }
        if (query.trim().toLowerCase().startsWith("delete ")) {
            return query;
        }
        if (query.trim().toLowerCase().startsWith("from ")) {
            return "delete " + query;
        }
        if (query.trim().indexOf(' ') == -1 && query.trim().indexOf('=') == -1 && params != null && params.length == 1) {
            query += " = ?1";
        }
        if (query.trim().indexOf(' ') == -1 && query.trim().indexOf('=') == -1 && params == null) {
            query += " = null";
        }
        return "delete from " + entityName + " where " + query;
    }

    @Nonnull
    private String createCountQuery(@Nonnull String dbName, @Nonnull String entityName, String query, Object... params) {
        if (query.trim().toLowerCase().startsWith("select ")) {
            return query;
        }
        if (query.matches("^by[A-Z].*$")) {
            return "select count(*) from " + entityName + " where " + findByToJPQL(dbName, query);
        }
        if (query.trim().toLowerCase().startsWith("from ")) {
            return "select count(*) " + query;
        }
        if (query.trim().toLowerCase().startsWith("order by ")) {
            return "select count(*) from " + entityName;
        }
        if (query.trim().indexOf(' ') == -1 && query.trim().indexOf('=') == -1 && params != null && params.length == 1) {
            query += " = ?1";
        }
        if (query.trim().indexOf(' ') == -1 && query.trim().indexOf('=') == -1 && params == null) {
            query += " = null";
        }
        if (query.trim().length() == 0) {
            return "select count(*) from " + entityName;
        }
        return "select count(*) from " + entityName + " e where " + query;
    }

    @SuppressWarnings("unchecked")
    @Nonnull
    private Query bindParameters(@Nonnull Query q, Object... params) {
        if (params == null) {
            return q;
        }
        if (params.length == 1 && params[0] instanceof Map) {
            return bindParameters(q, (Map<String, Object>) params[0]);
        }
        for (int i = 0; i < params.length; i++) {
            q.setParameter(i + 1, params[i]);
        }
        return q;
    }

    @Nonnull
    private Query bindParameters(@Nonnull Query q, Map<String,Object> params) {
        if (params == null) {
            return q;
        }
        for (String key : params.keySet()) {
            q.setParameter(key, params.get(key));
        }
        return q;
    }

    @Nonnull
    private String findByToJPQL(String dbName, String findBy) {
        findBy = findBy.substring(2);
        StringBuilder jpql = new StringBuilder();
        String subRequest;
        if (findBy.contains("OrderBy"))
            subRequest = findBy.split("OrderBy")[0];
        else subRequest = findBy;
        String[] parts = subRequest.split("And");
        int index = 1;
        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];
            if (part.endsWith("NotEqual")) {
                String prop = extractProp(part, "NotEqual");
                jpql.append(prop).append(" <> ?").append(index++);
            } else if (part.endsWith("Equal")) {
                String prop = extractProp(part, "Equal");
                jpql.append(prop).append(" = ?").append(index++);
            } else if (part.endsWith("IsNotNull")) {
                String prop = extractProp(part, "IsNotNull");
                jpql.append(prop).append(" is not null");
            } else if (part.endsWith("IsNull")) {
                String prop = extractProp(part, "IsNull");
                jpql.append(prop).append(" is null");
            } else if (part.endsWith("LessThan")) {
                String prop = extractProp(part, "LessThan");
                jpql.append(prop).append(" < ?").append(index++);
            } else if (part.endsWith("LessThanEquals")) {
                String prop = extractProp(part, "LessThanEquals");
                jpql.append(prop).append(" <= ?").append(index++);
            } else if (part.endsWith("GreaterThan")) {
                String prop = extractProp(part, "GreaterThan");
                jpql.append(prop).append(" > ?").append(index++);
            } else if (part.endsWith("GreaterThanEquals")) {
                String prop = extractProp(part, "GreaterThanEquals");
                jpql.append(prop).append(" >= ?").append(index++);
            } else if (part.endsWith("Between")) {
                String prop = extractProp(part, "Between");
                jpql.append(prop).append(" < ?").append(index++).append(" AND ").append(prop).append(" > ?").append(index++);
            } else if (part.endsWith("Like")) {
                String prop = extractProp(part, "Like");
                // HSQL -> LCASE, all other dbs lower
                if (this.isHSQL(dbName)) {
                    jpql.append("LCASE(").append(prop).append(") like ?").append(index++);
                } else {
                    jpql.append("LOWER(").append(prop).append(") like ?").append(index++);
                }
            } else if (part.endsWith("Ilike")) {
                String prop = extractProp(part, "Ilike");
                 if (this.isHSQL(dbName)) {
                    jpql.append("LCASE(").append(prop).append(") like LCASE(?").append(index++).append(")");
                 } else {
                    jpql.append("LOWER(").append(prop).append(") like LOWER(?").append(index++).append(")");
                 }
            } else if (part.endsWith("Elike")) {
                String prop = extractProp(part, "Elike");
                jpql.append(prop).append(" like ?").append(index++);
            } else {
                String prop = extractProp(part, "");
                jpql.append(prop).append(" = ?").append(index++);
            }
            if (i < parts.length - 1) {
                jpql.append(" AND ");
            }
        }
        return jpql.toString();
    }

    private boolean isHSQL(String dbName) {
        Configuration dbConfig = new Configuration(dbName);
        String db = dbConfig.getProperty("db");
        return ("mem".equals(db) || "fs".equals(db) || "org.hsqldb.jdbcDriver".equals(dbConfig.getProperty("db.driver")));
    }

    protected static String extractProp(String part, String end) {
        String prop = part.substring(0, part.length() - end.length());
        prop = (prop.charAt(0) + "").toLowerCase() + prop.substring(1);
        return prop;
    }
    public static JPQL instance = null;
}
