package play.template2;

public interface GTContentRenderer {

  GTRenderingResult render();

  // Sometimes when using body like it is done in CRUD, then we have to modify params in this
  // renderingResults context
  Object getRuntimeProperty(String name);

  void setRuntimeProperty(String name, Object value);
}
