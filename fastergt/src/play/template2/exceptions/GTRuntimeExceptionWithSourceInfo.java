package play.template2.exceptions;

import play.template2.GTTemplateLocation;

public class GTRuntimeExceptionWithSourceInfo extends GTRuntimeException {

    public final GTTemplateLocation templateLocation;
    public final int lineNo;

    public GTRuntimeExceptionWithSourceInfo(String s, Throwable throwable, GTTemplateLocation templateLocation, int lineNo) {
        super(s, throwable);
        this.templateLocation = templateLocation;
        this.lineNo = lineNo;
    }
}
