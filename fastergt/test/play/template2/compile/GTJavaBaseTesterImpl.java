package play.template2.compile;

import play.template2.GTGroovyBase;
import play.template2.GTJavaBase;
import play.template2.GTTemplateLocation;

public abstract class GTJavaBaseTesterImpl extends GTJavaBase {
  protected GTJavaBaseTesterImpl(
      Class<? extends GTGroovyBase> groovyClass, GTTemplateLocation templateLocation) {
    super(groovyClass, templateLocation);
  }

  @Override
  public Class getRawDataClass() {
    return null;
  }

  @Override
  public String convertRawDataToString(Object rawData) {
    return null;
  }

  @Override
  public String escapeHTML(String s) {
    return null;
  }

  @Override
  public String escapeXML(String s) {
    return null;
  }

  @Override
  public String escapeCsv(String s) {
    return null;
  }

  @Override
  public boolean validationHasErrors() {
    return false;
  }

  @Override
  public boolean validationHasError(String key) {
    return false;
  }

  @Override
  protected String resolveMessage(Object key, Object[] args) {
    return null;
  }

  @Override
  public Object cacheGet(String key) {
    return null;
  }

  @Override
  public void cacheSet(String key, String data, String duration) {}
}
