package play.template2.exceptions;


import play.template2.GTTemplateLocation;

public class GTCompilationExceptionWithSourceInfo extends GTCompilationException {

    public final String specialMessage;
    public final GTTemplateLocation templateLocation;
    public final int oneBasedLineNo;


    public GTCompilationExceptionWithSourceInfo(String specialMessage, GTTemplateLocation templateLocation, int oneBasedLineNo) {
        this.specialMessage = specialMessage;
        this.templateLocation = templateLocation;
        this.oneBasedLineNo = oneBasedLineNo;
    }

    public GTCompilationExceptionWithSourceInfo(String specialMessage, GTTemplateLocation templateLocation, int oneBasedLineNo, Throwable throwable) {
        super(throwable);
        this.specialMessage = specialMessage;
        this.templateLocation = templateLocation;
        this.oneBasedLineNo = oneBasedLineNo;
    }

    @Override
    public String getMessage() {
        return String.format("CompilationError: %s. Template %s:%d", specialMessage, templateLocation.relativePath, oneBasedLineNo);
    }

}
