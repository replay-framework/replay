package play.data.binding;

import org.apache.commons.lang.StringUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.Play;
import play.data.Upload;
import play.data.binding.types.*;
import play.data.validation.Validation;
import play.db.Model;
import play.exceptions.UnexpectedException;
import play.mvc.Http;
import play.mvc.Scope.Session;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.ParseException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.apache.commons.lang.StringUtils.isBlank;

/**
 * The binder try to convert String values to Java objects.
 */
public abstract class Binder {
    private static final Logger logger = LoggerFactory.getLogger(Binder.class);

    public static final Object MISSING = new Object();
    private static final Object DIRECTBINDING_NO_RESULT = new Object();
    private static final Object NO_BINDING = new Object();

    static final Map<Class<?>, TypeBinder<?>> supportedTypes = new HashMap<>();

    // TODO: something a bit more dynamic? The As annotation allows you to inject your own binder
    static {
        supportedTypes.put(Date.class, new DateBinder());
        supportedTypes.put(DateTime.class, new DateTimeBinder());
        supportedTypes.put(LocalDate.class, new LocalDateBinder());
        supportedTypes.put(LocalDateTime.class, new LocalDateTimeBinder());
        supportedTypes.put(File.class, new FileBinder());
        supportedTypes.put(File[].class, new FileArrayBinder());
        supportedTypes.put(Model.BinaryField.class, new BinaryBinder());
        supportedTypes.put(Upload.class, new UploadBinder());
        supportedTypes.put(Upload[].class, new UploadArrayBinder());
        supportedTypes.put(Calendar.class, new CalendarBinder());
        supportedTypes.put(Locale.class, new LocaleBinder());
        supportedTypes.put(byte[].class, new ByteArrayBinder());
        supportedTypes.put(byte[][].class, new ByteArrayArrayBinder());
    }

    /**
     * Add custom binder for any given class
     * 
     * E.g. @{code Binder.register(BigDecimal.class, new MyBigDecimalBinder());}
     * 
     * NB! Do not forget to UNREGISTER your custom binder when applications is reloaded (most probably in method
     * onApplicationStop()). Otherwise you will have a memory leak.
     * 
     * @param clazz
     *            The class to register
     * @param typeBinder
     *            The custom binder
     * @param <T>
     *            The Class type to register
     * @see #unregister(Class)
     */
    public static <T> void register(@Nonnull Class<T> clazz, @Nonnull TypeBinder<T> typeBinder) {
        supportedTypes.put(checkNotNull(clazz), checkNotNull(typeBinder));
    }

    /**
     * Remove custom binder that was add with method #register(java.lang.Class, play.data.binding.TypeBinder)
     * 
     * @param clazz
     *            The class to remove the custom binder
     * @param <T>
     *            The Class type to register
     */
    public static <T> void unregister(@Nonnull Class<T> clazz) {
        supportedTypes.remove(checkNotNull(clazz));
    }

    static Map<Class<?>, BeanWrapper> beanwrappers = new HashMap<>();

    static BeanWrapper getBeanWrapper(@Nonnull Class<?> clazz) {
        checkNotNull(clazz);
        if (!beanwrappers.containsKey(clazz)) {
            BeanWrapper beanwrapper = new BeanWrapper(clazz);
            beanwrappers.put(clazz, beanwrapper);
        }
        return beanwrappers.get(clazz);
    }

    @Nullable
    public static Object bind(Http.Request request, Session session, RootParamNode parentParamNode, String name, Class<?> clazz, Type type, Annotation[] annotations) {
        ParamNode paramNode = parentParamNode.getChild(name, true);

        Object result = null;
        if (paramNode == null) {
            result = MISSING;
        }

        BindingAnnotations bindingAnnotations = new BindingAnnotations(annotations);

        if (bindingAnnotations.checkNoBinding()) {
            return NO_BINDING;
        }

        if (paramNode != null) {

            // Let a chance to plugins to bind this object
            Optional<Object> bound = Play.pluginCollection.bind(request, session, parentParamNode, name, clazz, type, annotations);
            if (bound.isPresent()) {
                return bound.get();
            }

            result = internalBind(request, session, paramNode, clazz, type, bindingAnnotations);
        }

        if (result == MISSING) {
            if (clazz.equals(boolean.class)) {
                return false;
            }
            if (clazz.equals(int.class)) {
                return 0;
            }
            if (clazz.equals(long.class)) {
                return 0;
            }
            if (clazz.equals(double.class)) {
                return 0;
            }
            if (clazz.equals(short.class)) {
                return 0;
            }
            if (clazz.equals(byte.class)) {
                return 0;
            }
            if (clazz.equals(char.class)) {
                return ' ';
            }
            return null;
        }

        return result;

    }

    @Nullable
    private static Object internalBind(Http.Request request, Session session, ParamNode paramNode, Class<?> clazz, Type type, BindingAnnotations bindingAnnotations) {

        if (paramNode == null) {
            return MISSING;
        }

        if (paramNode.getValues() == null && paramNode.getAllChildren().isEmpty()) {
            return MISSING;
        }

        if (bindingAnnotations.checkNoBinding()) {
            return NO_BINDING;
        }

        try {

            if (Enum.class.isAssignableFrom(clazz)) {
                return bindEnum(clazz, paramNode);
            }

            if (Map.class.isAssignableFrom(clazz)) {
                return bindMap(request, session, type, paramNode, bindingAnnotations);
            }

            if (Collection.class.isAssignableFrom(clazz)) {
                return bindCollection(request, session, clazz, type, paramNode, bindingAnnotations);
            }

            Object directBindResult = internalDirectBind(paramNode.getOriginalKey(), request, session, bindingAnnotations.annotations,
                    paramNode.getFirstValue(clazz), clazz, type);

            if (directBindResult != DIRECTBINDING_NO_RESULT) {
                // we found a value/result when direct binding
                return directBindResult;
            }

            // Must do the default array-check after direct binding, since some custom-binders checks for specific
            // arrays
            if (clazz.isArray()) {
                return bindArray(request, session, clazz, paramNode, bindingAnnotations);
            }

            if (!paramNode.getAllChildren().isEmpty()) {
                return internalBindBean(request, session, clazz, paramNode, bindingAnnotations);
            }

            return null; // give up
        } catch (NumberFormatException | ParseException e) {
            logBindingNormalFailure(paramNode, e);
            addValidationError(paramNode);
        }
        return MISSING;
    }

    private static void addValidationError(ParamNode paramNode) {
        Validation.addError(paramNode.getOriginalKey(), "validation.invalid");
    }

    private static void logBindingNormalFailure(ParamNode paramNode, Exception e) {
        logger.debug("Failed to bind {}={}: {}", paramNode.getOriginalKey(), Arrays.toString(paramNode.getValues()), e);
    }

    private static Object bindArray(Http.Request request, Session session, Class<?> clazz, ParamNode paramNode, BindingAnnotations bindingAnnotations) {

        Class<?> componentType = clazz.getComponentType();

        int invalidItemsCount = 0;
        int size;
        Object array;
        String[] values = paramNode.getValues();
        if (values != null) {

            if (bindingAnnotations.annotations != null) {
                for (Annotation annotation : bindingAnnotations.annotations) {
                    if (annotation.annotationType().equals(As.class)) {
                        As as = ((As) annotation);
                        String separator = as.value()[0];
                        values = values[0].split(separator);
                    }
                }
            }

            size = values.length;
            array = Array.newInstance(componentType, size);
            for (int i = 0; i < size; i++) {
                String thisValue = values[i];
                try {
                    Array.set(array, i - invalidItemsCount, directBind(paramNode.getOriginalKey(), request, session, bindingAnnotations.annotations,
                            thisValue, componentType, componentType));
                } catch (Exception e) {
                    logger.debug("Bad item #{}: {}", i, e);
                    invalidItemsCount++;
                }
            }
        } else {
            size = paramNode.getAllChildren().size();
            array = Array.newInstance(componentType, size);
            int i = 0;
            for (ParamNode child : paramNode.getAllChildren()) {
                Object childValue = internalBind(request, session, child, componentType, componentType, bindingAnnotations);
                if (childValue != NO_BINDING && childValue != MISSING) {
                    try {
                        Array.set(array, i - invalidItemsCount, childValue);
                    } catch (Exception e) {
                        logger.debug("Bad item #{}: {}", i, e);
                        invalidItemsCount++;
                    }
                }
                i++;
            }
        }

        if (invalidItemsCount > 0) {
            // must remove some elements from the end..
            int newSize = size - invalidItemsCount;
            Object newArray = Array.newInstance(componentType, newSize);
            for (int i = 0; i < newSize; i++) {
                Array.set(newArray, i, Array.get(array, i));
            }
            array = newArray;
        }

        return array;
    }

    private static Object internalBindBean(Http.Request request, Session session, Class<?> clazz, ParamNode paramNode, BindingAnnotations bindingAnnotations) {
        Object bean = createNewInstance(clazz);
        internalBindBean(request, session, paramNode, bean, bindingAnnotations);
        return bean;
    }

    private static <T> T createNewInstance(@Nonnull Class<T> clazz) {
        try {
            Constructor<T> constructor = clazz.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            logger.warn("Failed to create instance of {}: {}", clazz.getName(), e);
            throw new UnexpectedException(e);
        } catch (NoSuchMethodException | InvocationTargetException e) {
            logger.error("Failed to create instance of {}: {}", clazz.getName(), e);
            throw new UnexpectedException(e);
        }
    }

    /**
     * Does NOT invoke plugins
     * 
     * @param paramNode
     *            List of parameters
     * @param bean
     *            the bean object
     * @param annotations
     *            annotations associated with the object
     */
    public static void bindBean(Http.Request request, Session session, ParamNode paramNode, Object bean, Annotation[] annotations) {
        internalBindBean(request, session, paramNode, bean, new BindingAnnotations(annotations));
    }

    private static void internalBindBean(Http.Request request, Session session, ParamNode paramNode, Object bean, BindingAnnotations bindingAnnotations) {

        BeanWrapper bw = getBeanWrapper(bean.getClass());
        for (BeanWrapper.Property prop : bw.getWrappers()) {
            ParamNode propParamNode = paramNode.getChild(prop.getName());
            if (propParamNode != null) {
                // Create new ParamsContext for this property
                Annotation[] annotations;
                // first we try with annotations resolved from property
                annotations = prop.getAnnotations();
                BindingAnnotations propBindingAnnotations = new BindingAnnotations(annotations, bindingAnnotations.getProfiles());
                Object value = internalBind(request, session, propParamNode, prop.getType(), prop.getGenericType(), propBindingAnnotations);
                if (value != MISSING) {
                    if (value != NO_BINDING) {
                        prop.setValue(bean, value);
                    }
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    @Nullable
    private static Object bindEnum(Class<?> clazz, ParamNode paramNode) {
        if (paramNode.getValues() == null) {
            return MISSING;
        }

        String value = paramNode.getFirstValue(null);

        if (StringUtils.isEmpty(value)) {
            return null;
        }
        return Enum.valueOf((Class<? extends Enum>) clazz, value);
    }

    private static Object bindMap(Http.Request request, Session session, Type type, ParamNode paramNode, BindingAnnotations bindingAnnotations) {
        Class keyClass = String.class;
        Class valueClass = String.class;
        if (type instanceof ParameterizedType) {
            keyClass = (Class) ((ParameterizedType) type).getActualTypeArguments()[0];
            valueClass = (Class) ((ParameterizedType) type).getActualTypeArguments()[1];
        }

        Map<Object, Object> r = new HashMap<>();

        for (ParamNode child : paramNode.getAllChildren()) {
            try {
                Object keyObject = directBind(paramNode.getOriginalKey(), request, session, bindingAnnotations.annotations, child.getName(), keyClass, keyClass);
                Object valueObject = internalBind(request, session, child, valueClass, valueClass, bindingAnnotations);
                if (valueObject == NO_BINDING || valueObject == MISSING) {
                    valueObject = null;
                }
                r.put(keyObject, valueObject);
            } catch (ParseException | NumberFormatException e) {
                // Just ignore the exception and continue on the next item
                logBindingNormalFailure(paramNode, e);
            }
        }

        return r;
    }

    @SuppressWarnings("unchecked")
    private static Object bindCollection(Http.Request request, Session session, Class<?> clazz, Type type, ParamNode paramNode, BindingAnnotations bindingAnnotations) {
        if (clazz.isInterface()) {
            if (clazz.equals(List.class)) {
                clazz = ArrayList.class;
            } else if (clazz.equals(Set.class)) {
                clazz = HashSet.class;
            } else if (clazz.equals(SortedSet.class)) {
                clazz = TreeSet.class;
            } else {
                clazz = ArrayList.class;
            }
        }

        Class componentClass = String.class;
        Type componentType = String.class;
        if (type instanceof ParameterizedType) {
            componentType = ((ParameterizedType) type).getActualTypeArguments()[0];
            if (componentType instanceof ParameterizedType) {
                componentClass = (Class) ((ParameterizedType) componentType).getRawType();
            } else {
                componentClass = (Class) componentType;
            }
        }

        if (paramNode.getAllChildren().isEmpty()) {
            // should use value-array as collection
            String[] values = paramNode.getValues();

            if (values == null) {
                return MISSING;
            }

            if (bindingAnnotations.annotations != null) {
                for (Annotation annotation : bindingAnnotations.annotations) {
                    if (annotation.annotationType().equals(As.class)) {
                        As as = ((As) annotation);
                        String separator = as.value()[0];
                        if (separator != null && !separator.isEmpty()) {
                            values = values[0].split(separator);
                        }
                    }
                }
            }

            Collection l;
            if (clazz.equals(EnumSet.class)) {
                l = EnumSet.noneOf(componentClass);
            } else {
                l = (Collection) createNewInstance(clazz);
            }
            boolean hasMissing = false;
            for (int i = 0; i < values.length; i++) {
                try {
                    Object value = internalDirectBind(paramNode.getOriginalKey(), request, session, bindingAnnotations.annotations, values[i], componentClass,
                            componentType);
                    if (value == DIRECTBINDING_NO_RESULT) {
                        hasMissing = true;
                    } else {
                        l.add(value);
                    }
                } catch (Exception e) {
                    // Just ignore the exception and continue on the next item
                    logBindingNormalFailure(paramNode, e); // TODO debug or error?
                }
            }
            if (hasMissing && l.size() == 0) {
                return MISSING;
            }
            return l;
        }

        Collection r = (Collection) createNewInstance(clazz);

        if (List.class.isAssignableFrom(clazz)) {
            // Must add items at position resolved from each child's key
            List l = (List) r;

            // must get all indexes and sort them so we add items in correct order.
            Set<String> indexes = new TreeSet<>((arg0, arg1) -> {
                try {
                    return Integer.valueOf(arg0).compareTo(Integer.valueOf(arg1));
                } catch (NumberFormatException e) {
                    return arg0.compareTo(arg1);
                }
            });
            indexes.addAll(paramNode.getAllChildrenKeys());

            // get each value in correct order with index

            for (String index : indexes) {
                ParamNode child = paramNode.getChild(index);
                Object childValue = internalBind(request, session, child, componentClass, componentType, bindingAnnotations);
                if (childValue != NO_BINDING && childValue != MISSING) {

                    // must make sure we place the value at the correct position
                    int pos = Integer.parseInt(index);
                    // must check if we must add empty elements before adding this item
                    int paddingCount = (l.size() - pos) * -1;
                    if (paddingCount > 0) {
                        for (int p = 0; p < paddingCount; p++) {
                            l.add(null);
                        }
                    }
                    l.add(childValue);
                }
            }

            return l;

        }

        for (ParamNode child : paramNode.getAllChildren()) {
            Object childValue = internalBind(request, session, child, componentClass, componentType, bindingAnnotations);
            if (childValue != NO_BINDING && childValue != MISSING) {
                r.add(childValue);
            }
        }

        return r;
    }

    /**
     * This method calls the user's defined binders prior to bind simple type
     * 
     * @param name
     *            name of the object
     * @param annotations
     *            annotation on the object
     * @param value
     *            value to bind
     * @param clazz
     *            class of the object
     * @param type
     *            type to bind
     * @return The binding object
     */
    @Nullable
    public static Object directBind(String name, Http.Request request, Session session, Annotation[] annotations, String value, Class<?> clazz, Type type) throws ParseException {
        // calls the direct binding and returns null if no value could be resolved..
        Object r = internalDirectBind(name, request, session, annotations, value, clazz, type);
        if (r == DIRECTBINDING_NO_RESULT) {
            return null;
        } else {
            return r;
        }
    }

    // If internalDirectBind was not able to bind it, it returns a special variable instance: DIRECTBIND_MISSING
    // Needs this because sometimes we need to know if no value was returned..
    @Nullable
    private static Object internalDirectBind(String name, Http.Request request, Session session, Annotation[] annotations, String value, Class<?> clazz, Type type)
            throws ParseException {
        boolean nullOrEmpty = isBlank(value);

        if (annotations != null) {
            for (Annotation annotation : annotations) {
                if (annotation.annotationType().equals(As.class)) {
                    Class<? extends TypeBinder<?>> toInstantiate = ((As) annotation).binder();
                    if (!(toInstantiate.equals(As.DEFAULT.class))) {
                        // Instantiate the binder
                        TypeBinder<?> myInstance = createNewInstance(toInstantiate);
                        return myInstance.bind(request, session, name, annotations, value, clazz, type);
                    }
                }
            }
        }

        // application custom types have higher priority. If unable to bind proceed with the next one
        for (Class<? extends TypeBinder> c : Play.classes.getAssignableClasses(TypeBinder.class)) {
            if (c.isAnnotationPresent(Global.class)) {
                Class<?> forType = (Class) ((ParameterizedType) c.getGenericInterfaces()[0]).getActualTypeArguments()[0];
                if (forType.isAssignableFrom(clazz)) {
                    Object result = createNewInstance(c).bind(request, session, name, annotations, value, clazz, type);
                    if (result != null) {
                        return result;
                    }
                }
            }
        }

        // custom types
        for (Class<?> c : supportedTypes.keySet()) {
            logger.trace("directBind: value [{}] c [{}] Class [{}]", value, c, clazz);

            if (c.isAssignableFrom(clazz)) {
                logger.trace("directBind: isAssignableFrom is true");
                return supportedTypes.get(c).bind(request, session, name, annotations, value, clazz, type);
            }
        }

        // raw String
        if (clazz.equals(String.class)) {
            return value;
        }

        // Handles the case where the model property is a sole character
        if (clazz.equals(Character.class)) {
            return value.charAt(0);
        }

        // Enums
        if (Enum.class.isAssignableFrom(clazz)) {
            return nullOrEmpty ? null : Enum.valueOf((Class<Enum>) clazz, value);
        }

        // int or Integer binding
        if ("int".equals(clazz.getName()) || clazz.equals(Integer.class)) {
            if (nullOrEmpty) {
                return clazz.isPrimitive() ? 0 : null;
            }

            return Integer.parseInt(value.contains(".") ? value.substring(0, value.indexOf('.')) : value);
        }

        // long or Long binding
        if ("long".equals(clazz.getName()) || clazz.equals(Long.class)) {
            if (nullOrEmpty) {
                return clazz.isPrimitive() ? 0L : null;
            }

            return Long.parseLong(value.contains(".") ? value.substring(0, value.indexOf('.')) : value);
        }

        // byte or Byte binding
        if ("byte".equals(clazz.getName()) || clazz.equals(Byte.class)) {
            if (nullOrEmpty) {
                return clazz.isPrimitive() ? (byte) 0 : null;
            }

            return Byte.parseByte(value.contains(".") ? value.substring(0, value.indexOf('.')) : value);
        }

        // short or Short binding
        if ("short".equals(clazz.getName()) || clazz.equals(Short.class)) {
            if (nullOrEmpty) {
                return clazz.isPrimitive() ? (short) 0 : null;
            }

            return Short.parseShort(value.contains(".") ? value.substring(0, value.indexOf('.')) : value);
        }

        // float or Float binding
        if ("float".equals(clazz.getName()) || clazz.equals(Float.class)) {
            if (nullOrEmpty) {
                return clazz.isPrimitive() ? 0.0f : null;
            }

            return Float.parseFloat(value);
        }

        // double or Double binding
        if ("double".equals(clazz.getName()) || clazz.equals(Double.class)) {
            if (nullOrEmpty) {
                return clazz.isPrimitive() ? 0.0d : null;
            }

            return Double.parseDouble(value);
        }

        // BigDecimal binding
        if (clazz.equals(BigDecimal.class)) {
            return nullOrEmpty ? null : new BigDecimal(value);
        }

        // BigInteger binding
        if (clazz.equals(BigInteger.class)) {
            return nullOrEmpty ? null : new BigInteger(value);
        }

        // boolean or Boolean binding
        if ("boolean".equals(clazz.getName()) || clazz.equals(Boolean.class)) {
            if (nullOrEmpty) {
                return clazz.isPrimitive() ? false : null;
            }

            if ("1".equals(value) || "on".equals(value.toLowerCase()) || "yes".equals(value.toLowerCase())) {
                return true;
            }

            return Boolean.parseBoolean(value);
        }

        return DIRECTBINDING_NO_RESULT;
    }
}
