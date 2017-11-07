package play.template2.legacy;

/**
 * Legacy fast-tags is only used to be backward compatible with old play 1.x FastTag-methods.
 * Legacy fastTags are slower than the new ones.
 * Legacy-Fast-tag methods must look like this one:
 *
 *
public static void tag_testFastTag(String tagName, GTJavaBase template, Map<String, Object> args, Closure body ) {
        template.out.append("[testFastTag before]");
        template.insertOutput( content.render());
        template.out.append("[from testFastTag after]");
    }


 */
public interface GTLegacyFastTagResolver {
    class LegacyFastTagInfo {
        // Full name to a static method which is responsible of calling the legacy fast tag
        public final String bridgeFullMethodName;
        public final String legacyFastTagClassname;
        public final String legacyFastTagMethodName;

        public LegacyFastTagInfo(String bridgeFullMethodName, String legacyFastTagClassname, String legacyFastTagMethodName) {
            this.bridgeFullMethodName = bridgeFullMethodName;
            this.legacyFastTagClassname = legacyFastTagClassname;
            this.legacyFastTagMethodName = legacyFastTagMethodName;
        }
    }

    LegacyFastTagInfo resolveLegacyFastTag(String tagName);
}
