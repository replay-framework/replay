package play.modules.pdf;

import org.allcolor.yahp.converter.IHtmlToPdfTransformer;

public class PDF {
  public static class Options {

    public String FOOTER;
    public String FOOTER_TEMPLATE;
    public String HEADER;
    public String HEADER_TEMPLATE;
    public String ALL_PAGES;
    public String EVEN_PAGES;
    public String ODD_PAGES;

    public String filename;

    public IHtmlToPdfTransformer.PageSize pageSize = IHtmlToPdfTransformer.A4P;
  }

}
