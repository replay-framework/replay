package play.modules.gtengineplugin.gt_integration;

import play.Play;
import play.classloading.ApplicationClasses;
import play.classloading.ApplicationClassloaderState;
import play.template2.GTFastTag;
import play.template2.GTFastTagResolver;

import java.util.ArrayList;
import java.util.List;

public class GTFastTagResolver1x implements GTFastTagResolver {

    private static final Object lock = new Object();
    private static ApplicationClassloaderState _lastKnownApplicationClassloaderState;
    private static List<GTFastTag> fastTagClasses;

    @Override public String resolveFastTag(String tagName) {

        synchronized (lock) {
            if (_lastKnownApplicationClassloaderState == null || !_lastKnownApplicationClassloaderState.equals(Play.classloader.currentState) || fastTagClasses == null) {
                _lastKnownApplicationClassloaderState = Play.classloader.currentState;
                fastTagClasses = new ArrayList<>();
                for (ApplicationClasses.ApplicationClass appClass : Play.classes.getAssignableClasses( GTFastTag.class ) ) {
                    try {
                        fastTagClasses.add( (GTFastTag)appClass.javaClass.newInstance());
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }

        for ( GTFastTag fastTag : fastTagClasses) {
            String res = fastTag.resolveFastTag( tagName);
            if ( res != null) {
                return res;
            }
        }
        return null;
    }
}
