package play.modules.gtengineplugin.gt_integration;

import play.Play;
import play.template2.GTFileResolver;
import play.template2.GTTemplateLocationReal;
import play.vfs.VirtualFile;

import javax.annotation.Nullable;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import static java.util.stream.Collectors.toList;

public class GTFileResolver1xImpl implements GTFileResolver.Resolver {

    // when null we look for templates in working directory, if list, we look for template in those folders.
    private final List<File> templateFolders;

    public GTFileResolver1xImpl(List<VirtualFile> templatesPaths) {
        templateFolders = templatesPaths.stream().map(path -> path.getRealFile()).collect(toList());
    }

    @Nullable
    @Override 
    public GTTemplateLocationReal getTemplateLocationReal(String queryPath) {
        // look for template file in all folders in templateFolders-list
        for ( File folder : templateFolders) {

            if ( folder == null) {
                // look for template in working dir.
                File file = new File(queryPath);
                if (file.exists() && file.isFile()) {
                    try {
                        return new GTTemplateLocationReal(VirtualFile.open(file).relativePath(), file.toURI().toURL());
                    } catch (MalformedURLException e) {
                        throw new RuntimeException(e);
                    }
                }
            } else {

                File file = new File ( folder, queryPath);
                if (file.exists() && file.isFile()) {
                    try {
                        return new GTTemplateLocationReal(VirtualFile.open(file).relativePath(), file.toURI().toURL());
                    } catch (MalformedURLException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
        
        // try to find it directly on the app-root before we give up
        VirtualFile tf = Play.getVirtualFile(queryPath);
        if (tf != null && tf.exists() && !tf.isDirectory()) {
            try {
                return new GTTemplateLocationReal(tf.relativePath(), tf.getRealFile().toURI().toURL());
            } catch (MalformedURLException e) {
                throw new RuntimeException(e);
            }
        }

        // try to find in classpath (e.g. "tags/select.tag" is located in "com.codeborne.replay:framework.jar")
        URL resource = Thread.currentThread().getContextClassLoader().getResource(queryPath);
        if (resource != null) {
            return new GTTemplateLocationReal(queryPath, resource);
        }

        // didn't find it
        return null;
    }

    @Nullable
    @Override 
    public GTTemplateLocationReal getTemplateLocationFromRelativePath(String relativePath) {
        
        VirtualFile vf = VirtualFile.fromRelativePath(relativePath);
        if ( vf == null || !vf.exists() || vf.isDirectory()) {
            return null;
        }

        try {
            return new GTTemplateLocationReal(relativePath, vf.getRealFile().toURI().toURL());
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

}
