package play.db;

import play.db.jpa.JPAModelLoader;
import play.exceptions.UnexpectedException;

import javax.persistence.Entity;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.List;

public interface Model {

    void _save();
    void _delete();
    Object _key();

    class Property {
        public String name;
        public Class<?> type;
        public Field field;
        public boolean isSearchable;
        public boolean isMultiple;
        public boolean isRelation;
        public boolean isGenerated;
        public Class<?> relationType;
        public Choices choices;
    }

    interface Choices {
        List<Object> list();
    }

    interface Factory {
        String keyName();
        Class<?> keyType();
        Object keyValue(Model m);
        Model findById(Object id);
        List<Model> fetch(int offset, int length, String orderBy, String orderDirection, List<String> properties, String keywords, String where);
        Long count(List<String> properties, String keywords, String where);
        void deleteAll();
        List<Model.Property> listProperties();
    }

    class Manager {
        public static Model.Factory factoryFor(Class<? extends Model> clazz) {
            if (!clazz.isAnnotationPresent(Entity.class)) {
                throw new UnexpectedException("Model " + clazz.getName() + " is not @Entity");
            }
            return new JPAModelLoader(clazz);
        }
    }

    interface BinaryField {
        InputStream get();
        void set(InputStream is, String type);
        long length();
        String type();
        boolean exists();
    }
}
