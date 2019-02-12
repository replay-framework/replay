package play.modules.gtengineplugin;

import play.Play;
import play.PlayPlugin;
import play.templates.Template;
import play.vfs.VirtualFile;

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
        for( VirtualFile vf : Play.templatesPath ) {
            // This is our path if we find the special file here..
            if (vf.child("__faster_groovy_templates.txt").exists()) {
                // This is our path.
                if ( index == 1) {
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
    }

    @Override
    public Optional<Template> loadTemplate(VirtualFile file) {
        return Optional.of(TemplateLoader.load(file));
    }
}
