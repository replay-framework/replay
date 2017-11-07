package play.template2.compile;

public interface GTTypeResolver {

    byte[] getTypeBytes(String name);
    
    boolean isApplicationClass(String className);
}
