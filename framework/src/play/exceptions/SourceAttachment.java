package play.exceptions;

/**
 * Exception has source attachment
 */
public interface SourceAttachment {

    String getSourceFile();
    Integer getLineNumber();
}
