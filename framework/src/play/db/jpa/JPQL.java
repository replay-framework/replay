package play.db.jpa;

import static java.lang.Long.parseLong;

import com.google.errorprone.annotations.CheckReturnValue;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import play.db.Configuration;
import play.db.jpa.GenericModel.JPAQuery;

@NullMarked
@CheckReturnValue
@SuppressWarnings("SqlSourceToSinkFlow")
public class JPQL {

  private static final Pattern BY_PATTERN = Pattern.compile("^by[A-Z].*$");
  private static final Pattern ORDER_BY_PATTERN = Pattern.compile("OrderBy");
  private static final Pattern AND_PATTERN = Pattern.compile("And");

  @Nullable
  public static final JPQL instance = new JPQL();

  private EntityManager em(String dbName) {
    return JPA.em(dbName);
  }

  long count(String dbName, String entity) {
    return parseLong(
        em(dbName)
            .createQuery("select count(*) from " + entity + " e")
            .getSingleResult()
            .toString());
  }

  public long count(String entity, String query, Object[] params) {
    return count(JPA.DEFAULT, entity, query, params);
  }

  long count(String dbName, String entity, String query, Object[] params) {
    return parseLong(
        bindParameters(
                em(dbName).createQuery(createCountQuery(dbName, entity, query, params)), params)
            .getSingleResult()
            .toString());
  }

  @SuppressWarnings("unchecked")
  <T extends JPABase> List<T> findAll(String dbName, String entity) {
    return em(dbName).createQuery("select e from " + entity + " e").getResultList();
  }

  @Nullable
  JPABase findById(String dbName, String entity, Object id) {
    return (JPABase) em(dbName).find(getEntityClass(entity), id);
  }

  private static Class<?> getEntityClass(String entity) {
    try {
      return Class.forName(entity);
    } catch (ClassNotFoundException e) {
      throw new RuntimeException("Failed to load entity class: \"%s\"".formatted(entity), e);
    }
  }

  JPAQuery find(String dbName, String entity, String query, Object[] params) {
    Query q = em(dbName).createQuery(createFindByQuery(dbName, entity, query, params));
    return new JPAQuery(
        createFindByQuery(dbName, entity, query, params), bindParameters(q, params));
  }

  int delete(String dbName, String entity, String query, Object[] params) {
    Query q = em(dbName).createQuery(createDeleteQuery(entity, query, params));
    return bindParameters(q, params).executeUpdate();
  }

  int deleteAll(String dbName, String entity) {
    Query q = em(dbName).createQuery(createDeleteQuery(entity, null));
    return bindParameters(q).executeUpdate();
  }

  private String createFindByQuery(
      String dbName, String entityName, @Nullable String query, Object @Nullable ... params) {
    if (query == null || query.trim().isEmpty()) {
      return "from " + entityName;
    }
    if (BY_PATTERN.matcher(query).matches()) {
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
    if (query.trim().indexOf(' ') == -1
        && query.trim().indexOf('=') == -1
        && params != null
        && params.length == 1) {
      query += " = ?1";
    }
    if (query.trim().indexOf(' ') == -1 && query.trim().indexOf('=') == -1 && params == null) {
      query += " = null";
    }
    return "from " + entityName + " where " + query;
  }

  private String createDeleteQuery(
      String entityName,
      @Nullable String query,
      Object @Nullable ... params
  ) {
    if (query == null) {
      return "delete from " + entityName;
    }
    if (query.trim().toLowerCase().startsWith("delete ")) {
      return query;
    }
    if (query.trim().toLowerCase().startsWith("from ")) {
      return "delete " + query;
    }
    if (query.trim().indexOf(' ') == -1
        && query.trim().indexOf('=') == -1
        && params != null
        && params.length == 1) {
      query += " = ?1";
    }
    if (query.trim().indexOf(' ') == -1 && query.trim().indexOf('=') == -1 && params == null) {
      query += " = null";
    }
    return "delete from " + entityName + " where " + query;
  }

  private String createCountQuery(
      String dbName, String entityName, String query, Object @Nullable ... params) {
    if (query.trim().toLowerCase().startsWith("select ")) {
      return query;
    }
    if (BY_PATTERN.matcher(query).matches()) {
      return "select count(*) from " + entityName + " where " + findByToJPQL(dbName, query);
    }
    if (query.trim().toLowerCase().startsWith("from ")) {
      return "select count(*) " + query;
    }
    if (query.trim().toLowerCase().startsWith("order by ")) {
      return "select count(*) from " + entityName;
    }
    if (query.trim().indexOf(' ') == -1
        && query.trim().indexOf('=') == -1
        && params != null
        && params.length == 1) {
      query += " = ?1";
    }
    if (query.trim().indexOf(' ') == -1 && query.trim().indexOf('=') == -1 && params == null) {
      query += " = null";
    }
    if (query.trim().isEmpty()) {
      return "select count(*) from " + entityName;
    }
    return "select count(*) from " + entityName + " e where " + query;
  }

  @SuppressWarnings("unchecked")
  private Query bindParameters(Query q, Object @Nullable ... params) {
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

  private Query bindParameters(Query q, @Nullable Map<String, Object> params) {
    if (params == null) {
      return q;
    }
    for (Map.Entry<String, Object> entry : params.entrySet()) {
      q.setParameter(entry.getKey(), entry.getValue());
    }
    return q;
  }

  private String findByToJPQL(String dbName, String findBy) {
    findBy = findBy.substring(2);
    StringBuilder jpql = new StringBuilder();
    String subRequest;
    if (findBy.contains("OrderBy")) subRequest = ORDER_BY_PATTERN.split(findBy)[0];
    else subRequest = findBy;
    String[] parts = AND_PATTERN.split(subRequest);
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
        jpql.append(prop)
            .append(" < ?")
            .append(index++)
            .append(" AND ")
            .append(prop)
            .append(" > ?")
            .append(index++);
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
    return ("mem".equals(db)
        || "fs".equals(db)
        || "org.hsqldb.jdbcDriver".equals(dbConfig.getProperty("db.driver")));
  }

  protected static String extractProp(String part, String end) {
    String prop = part.substring(0, part.length() - end.length());
    prop = (prop.charAt(0) + "").toLowerCase() + prop.substring(1);
    return prop;
  }
}
