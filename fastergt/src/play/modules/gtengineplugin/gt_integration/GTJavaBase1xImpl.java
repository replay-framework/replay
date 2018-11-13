package play.modules.gtengineplugin.gt_integration;

import org.apache.commons.lang.StringEscapeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.Play;
import play.cache.Cache;
import play.data.validation.Validation;
import play.i18n.Lang;
import play.i18n.Messages;
import play.mvc.Router;
import play.template2.GTGroovyBase;
import play.template2.GTJavaBase;
import play.template2.GTTemplateLocation;
import play.template2.exceptions.GTRuntimeException;
import play.template2.exceptions.GTTemplateNotFoundWithSourceInfo;
import play.templates.BaseTemplate;
import play.utils.HTML;

import java.util.Map;

import static org.apache.commons.lang.StringEscapeUtils.escapeHtml;

public abstract class GTJavaBase1xImpl extends GTJavaBase {
    private static final Logger logger = LoggerFactory.getLogger(GTJavaBase1xImpl.class);

    protected GTJavaBase1xImpl(Class<? extends GTGroovyBase> groovyClass, GTTemplateLocation templateLocation) {
        super(groovyClass, templateLocation);
    }

    // add extra methods used when resolving actions
    public String __reverseWithCheck_absolute_true(String action) {
        return __reverseWithCheck(action, true);
    }

    public String __reverseWithCheck_absolute_false(String action) {
        return __reverseWithCheck(action, false);
    }

    public static String __reverseWithCheck(String action, boolean absolute) {
        return Router.reverseWithCheck(action, Play.getVirtualFile(action), absolute);
    }

    @Override
    public boolean validationHasErrors() {
        return Validation.hasErrors();
    }

    @Override
    public boolean validationHasError(String key) {
        return Validation.hasError( key );
    }

    @Override
    protected String resolveMessage(Object key, Object[] args) {
        return Messages.getMessage(Lang.get(), this::getDefaultMessage, key, args);
    }

    private String getDefaultMessage(Object key) {
        logger.warn("Untranslated message: {}", key);
        return escapeHtml(key.toString());
    }

    @Override
    public Class getRawDataClass() {
        return BaseTemplate.RawData.class;
    }

    @Override
    public String convertRawDataToString(Object rawData) {
        return ((BaseTemplate.RawData)rawData).data;
    }

    @Override
    public String escapeHTML(String s) {
        return HTML.htmlEscape(s);
    }

    @Override
    public String escapeXML(String s) {
        return StringEscapeUtils.escapeXml(s);
    }

    @Override
    public String escapeCsv(String s) {
        return StringEscapeUtils.escapeCsv(s);
    }

    @Override
    public void internalRenderTemplate(Map<String, Object> args, boolean startingNewRendering, GTJavaBase callingTemplate) throws GTTemplateNotFoundWithSourceInfo, GTRuntimeException {
        // make sure the old layoutData referees to the same in map-instance as what the new impl uses
        BaseTemplate.layoutData.set( GTJavaBase.layoutData.get() );
        super.internalRenderTemplate(args, startingNewRendering, callingTemplate);
    }

    @Override
    public Object cacheGet(String key) {
        return Cache.get(key);
    }

    @Override
    public void cacheSet(String key, String data, String duration) {
        Cache.set(key, data, duration);
    }
}
