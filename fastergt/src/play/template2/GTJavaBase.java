package play.template2;

import groovy.lang.Binding;
import groovy.lang.Script;
import org.apache.commons.collections.iterators.ArrayIterator;
import play.template2.exceptions.GTCompilationException;
import play.template2.exceptions.GTRuntimeException;
import play.template2.exceptions.GTTemplateNotFoundWithSourceInfo;
import play.template2.exceptions.GTTemplateRuntimeException;
import play.template2.legacy.GTContentRendererFakeClosure;
import play.templates.JavaScriptEscaper;

import java.io.OutputStream;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collection;
import java.util.Formattable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;


public abstract class GTJavaBase extends GTRenderingResult {

    private static final String executeNextElseKeyName = "_executeNextElse";

    public StringWriter out;

    protected Script groovyScript;
    public Binding binding;
    private final Class<? extends GTGroovyBase> groovyClass;

    private Map<String, Object> orgArgs;

    // if this tag uses #{extends}, then the templatePath we extends is stored here.
    protected GTTemplateLocationReal extendsTemplateLocation; // default is not to extend anything...
    protected GTJavaBase extendedTemplate;
    protected GTJavaBase extendingTemplate; // if someone is extending us, this is the ref to their rendered template - used when dumping their output

    // When invoking a template as a tag, the content of the tag / body is stored here..
    public GTContentRenderer contentRenderer;

    public GTTemplateRepo templateRepo;

    public final GTTemplateLocation templateLocation;

    public static final ThreadLocal<Map<Object, Object>> layoutData = new ThreadLocal<>();
    private static final Object[] emptyArray = new Object[] {};

    protected GTJavaBase(Class<? extends GTGroovyBase> groovyClass, GTTemplateLocation templateLocation) {
        this.groovyClass = groovyClass;
        this.templateLocation = templateLocation;

        initNewOut();
    }


    @Override
    public void writeOutput(OutputStream ps, String encoding) {
        // if we have extended another template, we must pass this on to this template-instance,
        // because "it" has all the output
        if (extendedTemplate != null) {
            extendedTemplate.writeOutput(ps, encoding);
            return ;
        }
        super.writeOutput( ps, encoding);
    }

    public void insertOutput(GTRenderingResult otherTemplate) {
        allOuts.addAll( otherTemplate.allOuts);
        initNewOut();
    }

    protected void insertNewOut( StringWriter outToInsert) {
        allOuts.add(outToInsert);
        initNewOut();
    }

    protected void initNewOut() {
        // must create new live out
        out = new StringWriter();
        allOuts.add(out);
    }

    public void renderTemplate(Map<String, Object> args) throws GTTemplateNotFoundWithSourceInfo, GTRuntimeException{
        // this is the main rendering start of the actual template

        // init layout data which should be visible for all templates involved
        layoutData.set(new HashMap<>() );

        // clear outputs in case this is a second rendering
        allOuts.clear();
        initNewOut();

        GTContentRendererFakeClosure.initRendering();

        // clear extend-stuff
        extendsTemplateLocation = null;
        extendedTemplate = null;
        extendingTemplate = null;


        internalRenderTemplate(args, true, null);
    }

    public void internalRenderTemplate(Map<String, Object> args, boolean startingNewRendering, GTJavaBase callingTemplate) throws GTTemplateNotFoundWithSourceInfo, GTRuntimeException{
        internalRenderTemplate(args, null, startingNewRendering, callingTemplate);
    }

    // args passed in orgArgs are passed along to include, extend, and tags
    // tagArgs are not passed along
    public void internalRenderTemplate(Map<String, Object> orgArgs, Map<String, Object> tagArgs, boolean startingNewRendering, GTJavaBase callingTemplate) throws GTTemplateNotFoundWithSourceInfo, GTRuntimeException{

        if ( startingNewRendering ) {
            // start with fresh tag-stack
            GTTagContext.singleton.init();
        }

        try {

            // must store a copy of args, so we can pass the same (unchnaged) args to an extending template.
            this.orgArgs = new HashMap<>(orgArgs);
            // Must create a new map to prevent script-generated variables to leak out
            final HashMap<String, Object> bindingsMap = new HashMap<>(orgArgs);
            if( tagArgs != null ) {
                // make them available at scope, but not in orgArgs so they do not get passed along..
                bindingsMap.putAll(tagArgs);
            }
            this.binding = new Binding(bindingsMap);
            this.binding.setProperty("java_class", this);
            // must init our groovy script

            groovyScript = groovyClass.newInstance();
            groovyScript.setBinding( binding );


            // create a property in groovy so that groovy can find us (this)

            // call _renderTemplate directly
            _renderTemplate();

            // check if "we" have extended another template..
            if (extendsTemplateLocation != null) {
                
                if ( callingTemplate == null ) {
                    // This is the out-most template using extends
                    // yes, we've extended another template
                    // Get the template we are extending
                    extendedTemplate = templateRepo.getTemplateInstance( extendsTemplateLocation );
    
                    // tell it that "we" extended it..
                    extendedTemplate.extendingTemplate = this;
    
                    // ok, render it with original args..
                    extendedTemplate.internalRenderTemplate(bindingsMap, false, null );
                } else {
                    // Extends have been specified somewhere when rendering this template/tag.
                    // Must pass the extends-info up the chain
                    callingTemplate.extendsTemplateLocation = this.extendsTemplateLocation;
                }
            }

        } catch ( GTCompilationException e) {
            // just throw it
            throw e;
        } catch ( Throwable e) {
            // wrap it in a GTRuntimeException
            throw templateRepo.fixException(e);

        }
    }

    protected abstract void _renderTemplate();

    protected void enterTag( String tagName) {
        GTTagContext.singleton.enterTag(tagName);
    }

    protected void leaveTag( String tagName) {
        GTTagContext.singleton.exitTag();
    }
    
    /**
     * return the class/interface that, when an object is instanceof it, we should use
     * convertRawDataToString when converting it to String.
     * Framework should override.
     */
    public abstract Class getRawDataClass();

    /**
     *  See getRawDataClass for info
     */
    public abstract String convertRawDataToString(Object rawData);

    public abstract String escapeHTML( String s);
    public abstract String escapeXML( String s);
    public abstract String escapeCsv( String s);



    // We know that o is never null
    public String objectToString(Object o) {
        if (isRawData(o)) {
            return convertRawDataToString(o);
        }

        String objectAsString = o.toString();
        if (templateLocation.relativePath.endsWith(".xml")) {
            return escapeXML(objectAsString);
        }
        if (templateLocation.relativePath.endsWith(".csv")) {
            return escapeCsv(objectAsString);
        }
        if (!templateLocation.relativePath.endsWith(".html")) {
            return objectAsString;
        }
        if (binding.hasVariable("__inside_script_tag") && "true".equals(binding.getVariable("__inside_script_tag"))) {
            return JavaScriptEscaper.escape(objectAsString);
        }
        else {
            return escapeHTML(objectAsString);
        }
    }

    private boolean isRawData(Object o) {
        Class rawDataClass = getRawDataClass();
        return rawDataClass != null && rawDataClass.isAssignableFrom(o.getClass());
    }

    public boolean evaluateCondition(Object test) {
        if (test != null) {
            if (test instanceof Boolean) {
                return ((Boolean) test).booleanValue();
            } else if (test instanceof String) {
                return !((String) test).isEmpty();
            } else if (test instanceof BigInteger) {
                return ((BigInteger) test).compareTo(BigInteger.ZERO) != 0;
            } else if (test instanceof BigDecimal) {
                return ((BigDecimal) test).compareTo(BigDecimal.ZERO) != 0;
            } else if (test instanceof Number) {
                return ((Number) test).doubleValue() != 0.0;
            } else if (test instanceof Collection) {
                return !((Collection) test).isEmpty();
            } else if (test instanceof Map) {
                return !((Map) test).isEmpty();
            } else {
                return true;
            }
        }
        return false;
    }

    protected void invokeTagFile(String tagName, String tagFilePath, GTContentRenderer contentRenderer, Map<String, Object> tagArgs) {

        GTTemplateLocationReal tagTemplateLocation = GTFileResolver.impl.getTemplateLocationReal(tagFilePath);
        if ( tagTemplateLocation == null ) {
            throw new GTTemplateRuntimeException("Cannot find tag-file '"+tagFilePath+"'");
        }
        GTJavaBase tagTemplate = templateRepo.getTemplateInstance(tagTemplateLocation);
        // must set contentRenderes so that when the tag/template calls doBody, we can inject the output of the content of this tag
        tagTemplate.contentRenderer = contentRenderer;
        // render the tag
        // input should be all org args
        Map<String, Object> completeTagArgs = new HashMap<>();

        // and all scoped variables under _caller
        completeTagArgs.put("_caller", this.binding.getVariables());

        // Add new arg named "body" which is a fake closure which can be used to get the text-output
        // from the content of this tag..
        // Used in selenium.html-template and by users (eg: Greenscript)
        completeTagArgs.put("_body", new GTContentRendererFakeClosure(this, contentRenderer));

        // TODO: Must handle tag args like  _:_

        // and of course the tag args:
        // must prefix all tag args with '_'
        for ( String key : tagArgs.keySet()) {
            completeTagArgs.put("_"+key, tagArgs.get(key));
        }

        // Must also add all tag-args (the map) with original names as a new value named '_attrs'
        completeTagArgs.put("_attrs", tagArgs);

        tagTemplate.internalRenderTemplate(orgArgs, completeTagArgs, false, this);
        //grab the output
        insertOutput( tagTemplate );
    }


    // must be overridden by play framework
    public abstract boolean validationHasErrors();

    // must be overridden by play framework
    public abstract boolean validationHasError(String key);

    // Implement this method to do the actual message-resolving
    protected abstract String resolveMessage(Object key, Object[] args);

    public void clearElseFlag() {
        GTTagContext.singleton.parent().getData().remove(executeNextElseKeyName);
    }

    public void setElseFlag() {
        GTTagContext.singleton.parent().getData().put(executeNextElseKeyName, true);
    }

    public boolean elseFlagIsSet() {
        Boolean v = (Boolean)GTTagContext.singleton.parent().getData().get(executeNextElseKeyName);
        if ( v != null) {
            return v;
        } else {
            return false;
        }
    }

    protected String handleMessageTag(Object _args) {
        List argsList = (List) _args;

        switch (argsList.size()) {
            case 0: throw new GTTemplateRuntimeException("It looks like you don't have anything in your Message tag");
            case 1: return resolveMessage(assertKeyNonNull(argsList.get(0)), emptyArray);
            default: return resolveMessage(assertKeyNonNull(argsList.get(0)), messageArguments(argsList));
        }
    }

    private Object assertKeyNonNull(Object key) {
        if (key == null) {
            throw new GTTemplateRuntimeException("You are trying to resolve a message with an expression " +
              "that is resolved to null - " +
              "have you forgotten quotes around the message-key?");
        }
        return key;
    }

    private Object[] messageArguments(List argsList) {
        Object[] args = new Object[argsList.size() - 1];
        for (int i = 1; i < argsList.size(); i++) {
            args[i - 1] = escapeMessageArgument(argsList.get(i));
        }
        return args;
    }

    private Object escapeMessageArgument(Object arg) {
        return arg instanceof Formattable ? arg : objectToString(arg);
    }

    protected Iterator convertToIterator(final Object o) {

        if ( o instanceof Iterator) {
            return (Iterator)o;
        }

        if ( o instanceof Iterable ) {
            return ((Iterable)o).iterator();
        }

        if ( o instanceof Map ) {
            return (((Map)o).entrySet()).iterator();
        }

        if ( o.getClass().isArray()) {
            return ((Iterable) () -> new ArrayIterator(o)).iterator();
        }

        if ( o instanceof Class && ((Class<?>)o).isEnum()) {
            return ((Iterable) () -> new ArrayIterator(((Class<?>) o).getEnumConstants())).iterator();

        }

        throw new GTTemplateRuntimeException("Cannot convert object-reference to Iterator: " + o);
    }

    /**
     * If name starts with './', then we look for the template/name in the same folder as this template.
     * If not, we look for name/path in all template-places.
     * @param name
     * @return
     */
    public GTTemplateLocationReal resolveTemplateLocation (String name) {
        if (name.startsWith("./")) {
            String ct = this.templateLocation.relativePath;
            if (ct.matches("^/lib/[^/]+/app/views/.*")) {
                ct = ct.substring(ct.indexOf('/', 5));
            }
            ct = ct.substring(0, ct.lastIndexOf('/'));
            name = ct + name.substring(1);
            return GTFileResolver.impl.getTemplateLocationFromRelativePath(name);
        } else {
            return GTFileResolver.impl.getTemplateLocationReal(name);
        }

    }

    public abstract Object cacheGet(String key);
    public abstract void cacheSet(String key, String data, String duration);
}
