package play.inject;

public class Injector {

    private static BeanSource beanSource = new DefaultBeanSource();

    public static void setBeanSource(BeanSource beanSource) {
        Injector.beanSource = beanSource;
    }

    /**
     * @deprecated Use Play.beanSource instead
     */
    @Deprecated
    public static <T> T getBeanOfType(String className) {
        try {
            return getBeanOfType((Class<T>) Class.forName(className));
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Cannot instantiate " + className, e);
        }
    }

    /**
     * @deprecated Use Play.beanSource instead
     */
    @Deprecated
    public static <T> T getBeanOfType(Class<T> clazz) {
        return beanSource.getBeanOfType(clazz);
    }
}
