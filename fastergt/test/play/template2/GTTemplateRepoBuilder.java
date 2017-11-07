package play.template2;

import play.template2.compile.*;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

public class GTTemplateRepoBuilder {
    
    public File templateRootFolder = null;
    public boolean fakeWindowsNewLines = false;
    public GTJavaExtensionMethodResolver gtJavaExtensionMethodResolver = null;
    public GTPreCompilerFactory preCompilerFactory = new GTPreCompilerFactoryImpl();
    
    public GTTemplateRepoBuilder withTemplateRootFolder( File folder) {
        this.templateRootFolder = folder;
        return this;
    }

    public GTTemplateRepoBuilder withPreCompilerFactory(GTPreCompilerFactory preCompilerFactory) {
        this.preCompilerFactory = preCompilerFactory;
        return this;
    }

    public GTTemplateRepoBuilder withGTJavaExtensionMethodResolver(GTJavaExtensionMethodResolver gtJavaExtensionMethodResolver) {
        this.gtJavaExtensionMethodResolver = gtJavaExtensionMethodResolver;
        return this;
    }

    public GTTemplateRepoBuilder withFakeWindowsNewLines(boolean value) {
        this.fakeWindowsNewLines = value;
        return this;
    }

    public static class GTPreCompilerFactoryImpl implements GTPreCompilerFactory {

        //public GTTemplateRepo templateRepo;

        @Override public GTPreCompiler createCompiler(GTTemplateRepo templateRepo) {
            return new GTPreCompiler(templateRepo) {
                @Override
                public Class<? extends GTJavaBase> getJavaBaseClass() {
                    return GTJavaBaseTesterImpl.class;
                }
            };
        }

    }


    private GTTemplateRepo createTemplateRepo(final GTPreCompilerFactory preCompilerFactory) {

        GTJavaCompileToClass.typeResolver = new GTTypeResolver() {
            @Override public byte[] getTypeBytes(String name) {

                try {
                    InputStream in = getClass().getClassLoader().getResourceAsStream( name.replaceAll("\\.", "/") + ".class");
                    if ( in==null) {
                        return null;
                    }

                    byte[] buffer = new byte[1024];
                    int bytesRead;
                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    while ( (bytesRead = in.read(buffer))>0 ) {
                        out.write(buffer, 0, bytesRead);
                    }

                    return out.toByteArray();

                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public boolean isApplicationClass(String className) {
                return false;
            }
        };

        final GTTemplateRepo templateRepo = new GTTemplateRepo(getClass().getClassLoader(), false, preCompilerFactory, false, null);
        return templateRepo;
    }

    /**
     * Can be used to fake windows line feeds..
     */
    private static class SpecialGTTemplateLocationReal extends GTTemplateLocationReal {

        private final boolean fakeWindowsNewLines;

        private SpecialGTTemplateLocationReal(String relativePath, URL realFileURL, boolean fakeWindowsNewLines) {
            super(relativePath, realFileURL);
            this.fakeWindowsNewLines = fakeWindowsNewLines;
        }

        @Override
        public String readSource() {
            String src = super.readSource();
            if (fakeWindowsNewLines) {
                src = src.replaceAll("\\r?\\n", "\n").replaceAll("\\n", "\r\n");
            }
            return src;
        }
    }

    private static class FolderResolver implements GTFileResolver.Resolver {

        private final File folder;
        private final boolean fakeWindowsNewLines;

        private FolderResolver(File folder, boolean fakeWindowsNewLines) {
            this.folder = folder;
            this.fakeWindowsNewLines = fakeWindowsNewLines;
        }

        @Override
        public GTTemplateLocationReal getTemplateLocationReal(String queryPath) {
            File f = new File(folder, queryPath);
            if (!f.exists() || !f.isFile()) {
                return null;
            }

            try {
                return new SpecialGTTemplateLocationReal(queryPath, f.toURI().toURL(), fakeWindowsNewLines);
            } catch (MalformedURLException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public GTTemplateLocationReal getTemplateLocationFromRelativePath(String relativePath) {
            // in our case, since we don't have stuff like '{modules: blah}/' in our path, this is the same as queryPayj
            return getTemplateLocationReal(relativePath);
        }
    }

    public GTTemplateRepo build() {
        if ( this.templateRootFolder == null) {
            GTFileResolver.impl = null;
        } else {
            GTFileResolver.impl = new FolderResolver(templateRootFolder, fakeWindowsNewLines);
        }

        if ( this.gtJavaExtensionMethodResolver != null ) {
            GTGroovyPimpTransformer.gtJavaExtensionMethodResolver = this.gtJavaExtensionMethodResolver;
        } else {
            // default empty resolver
            GTGroovyPimpTransformer.gtJavaExtensionMethodResolver = new GTJavaExtensionMethodResolver() {
                @Override public Class findClassWithMethod(String methodName) {
                    return null;
                }
            };
        }
        
        return createTemplateRepo(preCompilerFactory);
    }
}
