package play.db.jpa;

import org.apache.commons.lang.StringUtils;
import play.data.binding.BeanWrapper;
import play.data.binding.Binder;
import play.data.binding.BindingAnnotations;
import play.data.binding.ParamNode;
import play.data.validation.Validation;
import play.exceptions.UnexpectedException;
import play.mvc.Http;
import play.mvc.Scope;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.persistence.*;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.*;

/**
 * A super class for JPA entities
 */
@MappedSuperclass
@SuppressWarnings("unchecked")
public class GenericModel extends JPABase {
    /**
     * Create a new model
     * 
     * @param rootParamNode
     *            parameters used to create the new object
     * @param name
     *            name of the object
     * @param type
     *            the class of the object
     * @param annotations
     *            annotations on the model
     * @param <T>
     *            The entity class
     * @return The created entity
     */
    @Nonnull
    public static <T extends JPABase> T create(Http.Request request, Scope.Session session, ParamNode rootParamNode, String name, Class<?> type, Annotation[] annotations) {
        try {
            Constructor c = type.getDeclaredConstructor();
            c.setAccessible(true);
            Object model = c.newInstance();
            return (T) edit(request, session, rootParamNode, name, model, annotations);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Edit a model
     * 
     * @param rootParamNode
     *            parameters used to create the new object
     * @param name
     *            name of the object
     * @param o
     *            the entity to update
     * @param annotations
     *            annotations on the model
     * @param <T>
     *            class of the entity
     * @return the entity
     */
    @Nonnull
    public static <T extends JPABase> T edit(Http.Request request, Scope.Session session, ParamNode rootParamNode, String name, Object o, Annotation[] annotations) {
        return edit(request, session, JPA.DEFAULT, rootParamNode, name, o, annotations);
    }

    /**
     * Edit a model
     * 
     * @param dbName
     *            the db name
     * @param rootParamNode
     *            parameters used to create the new object
     * @param name
     *            name of the object
     * @param o
     *            the entity to update
     * @param annotations
     *            annotations on the model
     * @param <T>
     *            class of the entity
     * @return the entity
     */
    @Nonnull
    private static <T extends JPABase> T edit(Http.Request request, Scope.Session session, String dbName, ParamNode rootParamNode, String name, @Nonnull Object o, Annotation[] annotations) {
        // #1601 - If name is empty, we're dealing with "root" request parameters (without prefixes).
        // Must not call rootParamNode.getChild in that case, as it returns null. Use rootParamNode itself instead.
        ParamNode paramNode = StringUtils.isEmpty(name) ? rootParamNode : rootParamNode.getChild(name, true);
        // #1195 - Needs to keep track of whick keys we remove so that we can restore it before
        // returning from this method.
        List<ParamNode.RemovedNode> removedNodesList = new ArrayList<>();
        try {
            BeanWrapper bw = new BeanWrapper(o.getClass());
            // Start with relations
            Set<Field> fields = new HashSet<>();
            Class<?> clazz = o.getClass();
            while (!clazz.equals(Object.class)) {
                Collections.addAll(fields, clazz.getDeclaredFields());
                clazz = clazz.getSuperclass();
            }
            for (Field field : fields) {
                boolean isEntity = false;
                String relation = null;
                boolean multiple = false;

                // First try the field
                Annotation[] fieldAnnotations = field.getAnnotations();
                // and check with the profiles annotations
                BindingAnnotations bindingAnnotations = new BindingAnnotations(fieldAnnotations,
                        new BindingAnnotations(annotations).getProfiles());
                if (bindingAnnotations.checkNoBinding()) {
                    continue;
                }

                if (field.isAnnotationPresent(OneToOne.class) || field.isAnnotationPresent(ManyToOne.class)) {
                    isEntity = true;
                    relation = field.getType().getName();
                }
                if (field.isAnnotationPresent(OneToMany.class) || field.isAnnotationPresent(ManyToMany.class)) {
                    Class<?> fieldType = (Class<?>) ((ParameterizedType) field.getGenericType()).getActualTypeArguments()[0];
                    isEntity = true;
                    relation = fieldType.getName();
                    multiple = true;
                }

                if (isEntity) {

                    ParamNode fieldParamNode = paramNode.getChild(field.getName(), true);

                    Class<Model> c = loadClass(relation);
                    if (JPABase.class.isAssignableFrom(c)) {
                        String keyName = Model.Manager.factoryFor(c).keyName();
                        if (multiple && Collection.class.isAssignableFrom(field.getType())) {
                            Collection l = new ArrayList<>();
                            if (SortedSet.class.isAssignableFrom(field.getType())) {
                                l = new TreeSet();
                            } else if (Set.class.isAssignableFrom(field.getType())) {
                                l = new HashSet();
                            }
                            String[] ids = fieldParamNode.getChild(keyName, true).getValues();
                            if (ids != null) {
                                // Remove it to prevent us from finding it again later
                                fieldParamNode.removeChild(keyName, removedNodesList);
                                for (String _id : ids) {
                                    if (_id == null || _id.equals("")) {
                                        continue;
                                    }

                                    Query q = JPA.em(dbName).createQuery("from " + relation + " where " + keyName + " = ?1");
                                    q.setParameter(1, Binder.directBind(rootParamNode.getOriginalKey(), request, session, annotations, _id,
                                            Model.Manager.factoryFor(loadClass(relation)).keyType(), null));
                                    try {
                                        l.add(q.getSingleResult());

                                    } catch (NoResultException e) {
                                        Validation.addError(name + "." + field.getName(), "validation.notFound", _id);
                                    }
                                }
                                bw.set(field.getName(), o, l);
                            }
                        } else {
                            String[] ids = fieldParamNode.getChild(keyName, true).getValues();
                            if (ids != null && ids.length > 0 && !ids[0].equals("")) {

                                Query q = JPA.em(dbName).createQuery("from " + relation + " where " + keyName + " = ?1");
                                q.setParameter(1, Binder.directBind(rootParamNode.getOriginalKey(), request, session, annotations, ids[0],
                                        Model.Manager.factoryFor(loadClass(relation)).keyType(), null));
                                try {
                                    Object to = q.getSingleResult();
                                    edit(request, session, paramNode, field.getName(), to, field.getAnnotations());
                                    // Remove it to prevent us from finding it again later
                                    paramNode.removeChild(field.getName(), removedNodesList);
                                    bw.set(field.getName(), o, to);
                                } catch (NoResultException e) {
                                    Validation.addError(fieldParamNode.getOriginalKey(), "validation.notFound", ids[0]);
                                    // Remove only the key to prevent us from finding it again later
                                    // This how the old impl does it..
                                    fieldParamNode.removeChild(keyName, removedNodesList);
                                    if (fieldParamNode.getAllChildren().size() == 0) {
                                        // remove the whole node..
                                        paramNode.removeChild(field.getName(), removedNodesList);
                                    }

                                }

                            } else if (ids != null && ids.length > 0 && ids[0].equals("")) {
                                bw.set(field.getName(), o, null);
                                // Remove the key to prevent us from finding it again later
                                fieldParamNode.removeChild(keyName, removedNodesList);
                            }
                        }
                    }
                }
            }
            // #1601 - If name is empty, we're dealing with "root" request parameters (without prefixes).
            // Must not call rootParamNode.getChild in that case, as it returns null. Use rootParamNode itself instead.
            ParamNode beanNode = StringUtils.isEmpty(name) ? rootParamNode : rootParamNode.getChild(name, true);
            Binder.bindBean(request, session, beanNode, o, annotations);
            return (T) o;
        } catch (Exception e) {
            throw new UnexpectedException(e);
        } finally {
            // restoring changes to paramNode
            ParamNode.restoreRemovedChildren(removedNodesList);
        }
    }

    @Nonnull
    private static <T> Class<T> loadClass(String className) throws ClassNotFoundException {
        return (Class<T>) Thread.currentThread().getContextClassLoader().loadClass(className);
    }

    /**
     * Edit a model
     * 
     * @param rootParamNode
     *            parameters used to create the new object
     * @param name
     *            name of the entity
     * @param <T>
     *            class of the entity
     * @return the entity
     */
    @Nonnull
    public <T extends GenericModel> T edit(Http.Request request, Scope.Session session, ParamNode rootParamNode, String name) {
        edit(request, session, rootParamNode, name, this, null);
        return (T) this;
    }

    @Nonnull
    public <T extends JPABase> T save() {
        _save();
        return (T) this;
    }

    /**
     * Merge this object to obtain a managed entity (useful when the object comes from the Cache).
     * 
     * @param <T>
     *            class of the entity
     * @return The given entity
     */
    @Nonnull
    public <T extends JPABase> T merge() {
        return (T) em(JPA.getDBName(this.getClass())).merge(this);
    }

    /**
     * Delete the entity.
     * 
     * @param <T>
     *            class of the entity
     * @return The deleted entity.
     */
    @Nonnull
    public <T extends JPABase> T delete() {
        _delete();
        return (T) this;
    }

    public static class JPAQuery {

        public Query query;
        public String sq;

        public JPAQuery(String sq, Query query) {
            this.query = query;
            this.sq = sq;
        }

        public  JPAQuery(Query query) {
            this.query = query;
            this.sq = query.toString();
        }

        @Nullable
        public <T> T first() {
            try {
                List<T> results = query.setMaxResults(1).getResultList();
                if (results.isEmpty()) {
                    return null;
                }
                return results.get(0);
            } catch (Exception e) {
                throw new JPAQueryException("Error while executing query <strong>" + sq + "</strong>", JPAQueryException.findBestCause(e));
            }
        }

        /**
         * Bind a JPQL named parameter to the current query. Careful, this will also bind count results. This means that
         * Integer get transformed into long so hibernate can do the right thing. Use the setParameter if you just want
         * to set parameters.
         * 
         * @param name
         *            name of the object
         * @param param
         *            current query
         * @return The query
         */
        @Nonnull
        public JPAQuery bind(String name, Object param) {
            if (param.getClass().isArray()) {
                param = Arrays.asList((Object[]) param);
            }
            if (param instanceof Integer) {
                param = ((Integer) param).longValue();
            }
            query.setParameter(name, param);
            return this;
        }

        /**
         * Set a named parameter for this query.
         * 
         * @param name
         *            Parameter name
         * @param param
         *            The given parameters
         * @return The query
         */
        @Nonnull
        public JPAQuery setParameter(String name, Object param) {
            query.setParameter(name, param);
            return this;
        }

        /**
         * Retrieve all results of the query
         * 
         * @param <T>
         *            the type of the entity
         * @return A list of entities
         */
        @Nonnull
        public <T> List<T> fetch() {
            try {
                return query.getResultList();
            } catch (Exception e) {
                throw new JPAQueryException("Error while executing query <strong>" + sq + "</strong>", JPAQueryException.findBestCause(e));
            }
        }

        /**
         * Retrieve results of the query
         * 
         * @param max
         *            Max results to fetch
         * @param <T>
         *            The entity class
         * @return A list of entities
         */
        @Nonnull
        public <T> List<T> fetch(int max) {
            try {
                query.setMaxResults(max);
                return query.getResultList();
            } catch (Exception e) {
                throw new JPAQueryException("Error while executing query <strong>" + sq + "</strong>", JPAQueryException.findBestCause(e));
            }
        }

        /**
         * Set the position to start
         * 
         * @param position
         *            Position of the first element
         * @return A new query
         */
        @Nonnull
        public JPAQuery from(int position) {
            query.setFirstResult(position);
            return this;
        }

        /**
         * Retrieve a page of result
         * 
         * @param page
         *            Page number (start at 1)
         * @param length
         *            (page length)
         * @param <T>
         *            The entity class
         * @return a list of entities
         */
        @Nonnull
        public <T> List<T> fetch(int page, int length) {
            if (page < 1) {
                page = 1;
            }
            query.setFirstResult((page - 1) * length);
            query.setMaxResults(length);
            try {
                return query.getResultList();
            } catch (Exception e) {
                throw new JPAQueryException("Error while executing query <strong>" + sq + "</strong>", JPAQueryException.findBestCause(e));
            }
        }
    }
}
