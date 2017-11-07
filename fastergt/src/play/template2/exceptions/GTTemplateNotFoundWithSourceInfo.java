package play.template2.exceptions;

import play.template2.GTTemplateLocation;

public class GTTemplateNotFoundWithSourceInfo extends GTTemplateNotFound{
    public final GTTemplateLocation templateLocation;
    public final int lineNo;
    
    public GTTemplateNotFoundWithSourceInfo(String askedForQueryPath, GTTemplateLocation templateLocation, int lineNo) {
        super(askedForQueryPath);
        this.templateLocation = templateLocation;
        this.lineNo = lineNo;
    }
}
