package play.modules.gtengineplugin;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.List;
import javax.inject.Inject;
import play.Play;
import play.PlayPlugin;
import play.jobs.Job;
import play.mvc.PlayController;
import play.template2.FastTags;
import play.template2.JavaExtensions;
import play.templates.Template;

import java.io.File;
import java.util.Optional;

public class GTEnginePlugin extends PlayPlugin {
    private synchronized void init() {
        fixTemplatesPathOrder();
        TemplateLoader.init();
    }

    private void fixTemplatesPathOrder() {
        // Make sure our app/view-folder is the first one amongst the modules listed in Play.templatesPath
        // Look for our path
        int index = 0;
        for (File vf : Play.templatesPath ) {
            // This is our path if we find the special file here
            if (new File(vf, "__faster_groovy_templates.txt").exists()) {
                // This is our path.
                if (index == 1) {
                    // the location is correct
                } else {
                    // move it to location 1 (right after the app-view folder
                    Play.templatesPath.remove( index );
                    Play.templatesPath.add(1, vf);
                }
                break;
            }
            index++;
        }
    }

    @Override
    public void onLoad() {
        // Must init right away since we have to be inited when routes are being parsed
        init();
    }

    @Override
    public void onApplicationStart() {
        // need to re-init when app restarts
        init();
        injectStaticFields();
    }

    private void injectStaticFields() {
        injectStaticFields(Play.classes.getAssignableClasses(PlayController.class));
        injectStaticFields(Play.classes.getAssignableClasses(Job.class));
        injectStaticFields(Play.classes.getAssignableClasses(FastTags.class));
        injectStaticFields(Play.classes.getAssignableClasses(JavaExtensions.class));
    }

    private <T> void injectStaticFields(List<Class<? extends T>> classes) {
        for (Class<?> clazz : classes) {
            injectStaticFields(clazz);
        }
    }

    private void injectStaticFields(Class<?> clazz) {
        for (Field field : clazz.getDeclaredFields()) {
            if (isStaticInjectable(field)) {
                inject(field);
            }
        }
    }

    private boolean isStaticInjectable(Field field) {
        return Modifier.isStatic(field.getModifiers()) && field.isAnnotationPresent(Inject.class);
    }

    private void inject(Field field) {
        field.setAccessible(true);
        try {
            field.set(null, Play.beanSource.getBeanOfType(field.getType()));
        }
        catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Optional<Template> loadTemplate(File file) {
        return Optional.of(TemplateLoader.load(file));
    }
}
