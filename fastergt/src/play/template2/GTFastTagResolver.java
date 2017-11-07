package play.template2;

/**
 * Fast-tag methods must look like this one:
 *
 *
public static void tag_testFastTag(GTJavaBase template, Map<String, Object> args, GTContentRenderer content ) {
        template.out.append("[testFastTag before]");
        template.insertOutput( content.render());
        template.out.append("[from testFastTag after]");
    }
 */
public interface GTFastTagResolver {

    // if fastTag is valid, this method returns full method-name for a static method..
    String resolveFastTag(String tagName);
}
