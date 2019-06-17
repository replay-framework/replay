package play.template2;

import play.template2.compile.GTCompiler;
import play.template2.compile.GTJavaCompileToClass;
import play.template2.compile.GTPreCompiler;
import play.template2.compile.GTPreCompilerFactory;
import play.template2.exceptions.GTCompilationException;
import play.template2.exceptions.GTCompilationExceptionWithSourceInfo;
import play.template2.exceptions.GTException;
import play.template2.exceptions.GTTemplateNotFound;

import javax.annotation.Nullable;
import java.io.File;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class GTTemplateRepo {

    private final boolean checkForChanges;
    private final GTPreCompilerFactory preCompilerFactory;
    private final boolean preCompiledMode;
    @Nullable private final File folderToDumpClassesIn;

    private final Map<String, TemplateInfo> loadedTemplates = new ConcurrentHashMap<>();
    protected Map<String, TemplateInfo> classname2TemplateInfo = new ConcurrentHashMap<>();


    public static class TemplateInfo {
        public final GTTemplateLocation templateLocation;
        public final long fileSize;
        public final long fileDate;
        public final GTTemplateInstanceFactory templateInstanceFactory;

        private TemplateInfo(GTTemplateLocation templateLocation, GTTemplateInstanceFactory templateInstanceFactory) {
            this.templateLocation = templateLocation;

            if ( templateLocation instanceof GTTemplateLocationReal) {
                GTTemplateLocationReal real = (GTTemplateLocationReal)templateLocation;
                // store fileSize and time so we can detect changes.
                IO.FileInfo fileInfo = IO.getFileInfo(real.realFileURL);
                
                fileSize = fileInfo.size;
                fileDate = fileInfo.lastModified;

            } else {
                fileSize = 0;
                fileDate = 0;
            }
            this.templateInstanceFactory = templateInstanceFactory;
        }

        public boolean isModified() {

            if ( !(templateLocation instanceof GTTemplateLocationReal) ) {
                // Cannot check for changes - does not have a file
                return false;
            }
            
            File freshFile = IO.getFileFromURL(((GTTemplateLocationReal)templateLocation).realFileURL);
            if ( freshFile == null) {
                return false;
            }
            if (!freshFile.exists() || !freshFile.isFile()) {
                return true;
            }
            if (fileSize != freshFile.length()) {
                return true;
            }

            if ( fileDate != freshFile.lastModified()) {
                return true;
            }

            return false;
        }

        public Class<? extends GTJavaBase> getTemplateClass() {
            return templateInstanceFactory.getTemplateClass();
        }

        public GTLineMapper getGroovyLineMapper() {
            try {
                return (GTLineMapper)getTemplateClass().getDeclaredMethod("getGroovyLineMapper").invoke(null);
            } catch ( Exception e) {
                throw new RuntimeException(e);
            }
        }

        public GTLineMapper getJavaLineMapper() {
            try {
                return (GTLineMapper)getTemplateClass().getDeclaredMethod("getJavaLineMapper").invoke(null);
            } catch ( Exception e) {
                throw new RuntimeException(e);
            }
        }
    }


    public GTTemplateRepo(boolean checkForChanges, GTPreCompilerFactory preCompilerFactory, boolean preCompiledMode, @Nullable File folderToDumpClassesIn) {
        this.checkForChanges = checkForChanges;

        this.preCompilerFactory = preCompilerFactory;
        if ( preCompilerFactory ==null ) {
            throw new GTException("preCompilerFactory cannot be null");
        }

        this.preCompiledMode = preCompiledMode;
        this.folderToDumpClassesIn = folderToDumpClassesIn;
    }


    public boolean templateExists(GTTemplateLocation templateLocation) {
        if (templateLocation == null) return false;
        try {
            getTemplateInstance(templateLocation, false);
            return true;
        }
        catch (GTTemplateNotFound e) {
            return false;
        }
    }

    private void removeTemplate ( String templatePath ) {
        TemplateInfo ti = loadedTemplates.remove( templatePath);
        if ( ti!=null) {
            classname2TemplateInfo.remove(ti.getTemplateClass().getName());
        }
    }

    private void addTemplate ( String templatePath, TemplateInfo ti) {
        loadedTemplates.put(templatePath, ti);
        classname2TemplateInfo.put(ti.getTemplateClass().getName(), ti);
    }

    @Nullable
    public GTJavaBase getTemplateInstance( final GTTemplateLocation templateLocation) throws GTTemplateNotFound {
        return getTemplateInstance(templateLocation, true);
    }

    @Nullable
    private GTJavaBase getTemplateInstance( final GTTemplateLocation templateLocation, boolean doCompile) throws GTTemplateNotFound {

        // Is this a loaded template ?
        TemplateInfo ti = loadedTemplates.get(templateLocation.relativePath);
        if ( ti == null || checkForChanges ) {
            synchronized(loadedTemplates) {

                ti = loadedTemplates.get(templateLocation.relativePath);

                if ( ti == null ) {
                    // look for compiled class (precompiled class or class in cache.)
                    ti = lookForPreCompiledOrCached(templateLocation);
                }

                if ( ti != null) {
                    // is it changed on disk?
                    if (ti.isModified()) {
                        // remove it
                        removeTemplate( templateLocation.relativePath);
                        ti = null;
                    }
                }

                if (ti == null ) {
                    // new or modified - must compile it

                    if (templateLocation instanceof GTTemplateLocationReal) {

                        try {
                            // test if it works
                            ((GTTemplateLocationReal)templateLocation).realFileURL.openStream().close();
                        } catch (Exception e ) {
                            throw new GTTemplateNotFound( templateLocation.relativePath);
                        }

                    }

                    if ( !doCompile) {
                        // We know the compile exists - skip compiling.
                        return null;
                    }

                    ti = compileTemplate(templateLocation);

                }

                if ( ti != null) {
                    // store it
                    addTemplate(templateLocation.relativePath, ti);

                }
            }
        }

        if ( ti == null) {
            throw new GTTemplateNotFound(templateLocation.relativePath);
        }

        // already compile and unchanged - lets return the template instance
        return ti.templateInstanceFactory.create(this);
    }

    // If running in precompiled mode, we look in parent classloader,
    // if not we're looking for class on disk.
    @Nullable
    private TemplateInfo lookForPreCompiledOrCached(GTTemplateLocation templateLocation) {
        String templateClassName = GTPreCompiler.generatedPackageName + "." + GTPreCompiler.generateTemplateClassname( templateLocation.relativePath);
        if (preCompiledMode) {
            // compiled template classes are loaded by framework as regular classes....
            // look for it
            try {
                Class<? extends GTJavaBase> templateClass = (Class<? extends GTJavaBase>) Class.forName(templateClassName);
                // found it
                return new TemplateInfo( templateLocation, new GTTemplateInstanceFactoryRegularClass(templateClass));
            } catch (ClassNotFoundException e) {
                // nop..
            }
        } else if(folderToDumpClassesIn != null){

            return loadTemplateFromDisk(templateLocation, templateClassName);
        }

        return null;
    }

    @Nullable
    private TemplateInfo loadTemplateFromDisk(GTTemplateLocation templateLocation, String templateClassName) {
        // generate filename
        final String classFilenameWithPath = templateClassName.replace('.','/') + ".class";
        final File file = new File(folderToDumpClassesIn, classFilenameWithPath);
        if ( !file.exists()) {
            return null;
        }

        // create templateLocationReal to actuall template src file
        GTTemplateLocationReal templateLocationReal = GTFileResolver.impl.getTemplateLocationFromRelativePath(templateLocation.relativePath);

        if ( templateLocationReal == null) {
            // could not fund the corresponding template-source-file
            file.delete();
            return null;
        }
        
        // need an actual file-object
        File realFile = IO.getFileFromURL(templateLocationReal.realFileURL);
        if ( realFile == null) {
            return null;
        }

        // check if class file and template-src have the same lastModified date
        if ( realFile.lastModified() != file.lastModified()) {
            // cached classes are old. cannot use them. delete it so we don't find it again
            file.delete();
            return null;
        }


        // found the main template class file - must load all classes for this template - which all starts with the same name..
        final File folder = file.getParentFile();
        final String simpleFilename = file.getName().substring(0, file.getName().length() - 6); // remove ".class"
        File[] allClassFiles = folder.listFiles((file1, s) -> s.startsWith( simpleFilename));

        GTJavaCompileToClass.CompiledClass[] compiledClasses = new GTJavaCompileToClass.CompiledClass[allClassFiles.length];
        int i=0;
        for ( File classFile : allClassFiles) {
            byte[] bytes = IO.readContent( classFile);
            String className = GTPreCompiler.generatedPackageName + "." + classFile.getName().substring(0, classFile.getName().length() - 6); // remove ".class";

            compiledClasses[i++] = new GTJavaCompileToClass.CompiledClass(className, bytes);
        }

        GTCompiler.CompiledTemplate compiledTemplate = new GTCompiler.CompiledTemplate(templateClassName, compiledClasses);


        return new TemplateInfo( templateLocationReal, new GTTemplateInstanceFactoryLive(compiledTemplate) );
        

    }

    public TemplateInfo compileTemplate(GTTemplateLocation templateLocation) throws GTException {
        TemplateInfo ti;
        try {
            // compile it
            GTCompiler.CompiledTemplate compiledTemplate = new GTCompiler(this, preCompilerFactory, true).compile( templateLocation);

            if (folderToDumpClassesIn != null && templateLocation instanceof GTTemplateLocationReal) {
                // Must dump these classes in folder...

                IO.FileInfo fileInfo = IO.getFileInfo(((GTTemplateLocationReal)templateLocation).realFileURL);

                for ( GTJavaCompileToClass.CompiledClass compiledClass : compiledTemplate.compiledJavaClasses) {
                    String filename = compiledClass.classname.replace('.','/') + ".class";
                    File file = new File(folderToDumpClassesIn, filename);
                    file.getParentFile().mkdirs();
                    IO.write( compiledClass.bytes, file);
                    // set lastModified date on file equal to the one from the template src - then we can check if cache is valid later..
                    file.setLastModified(fileInfo.lastModified);
                }

            }

            GTTemplateInstanceFactory templateInstanceFactory = new GTTemplateInstanceFactoryLive(compiledTemplate);

            ti = new TemplateInfo(templateLocation, templateInstanceFactory);
        } catch(GTTemplateNotFound | GTCompilationExceptionWithSourceInfo e) {
            throw e;
        }
        catch (Exception e) {
            // Must only store it if no error occurs
            throw new GTCompilationException(e);
        }
        return ti;
    }

    public GTException fixException(Throwable e) {
        return new GTExceptionFixer(this).fixException(e);
    }


}
