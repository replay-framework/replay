/**
 * Copyright 2010, greenlaw110@gmail.com.
 *
 * <p>This is free software; you can redistribute it and/or modify it under the terms of the GNU
 * Lesser General Public License as published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * <p>This software is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * <p>You should have received a copy of the GNU Lesser General Public License along with this
 * software; if not, write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor,
 * Boston, MA 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 *
 * <p>User: Green Luo Date: Mar 26, 2010
 */
package play.modules.excel;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import org.apache.commons.codec.net.URLCodec;
import play.Play;
import play.PlayPlugin;
import play.exceptions.UnexpectedException;
import play.mvc.Http.Header;
import play.mvc.Http.Request;
import play.mvc.Http.Response;
import play.mvc.Scope;
import play.mvc.Scope.RenderArgs;
import play.mvc.Scope.Session;
import play.mvc.results.Result;
import play.templates.Template;

public class Plugin extends PlayPlugin {

  private static final Pattern EXCEL_MIME =
      Pattern.compile(".*application/(excel|vnd\\.ms-excel|x-excel|x-msexcel).*");
  private static final Pattern IE_USER_AGENT = Pattern.compile(".*MSIE\\s+[678]\\.0.*");
  private static final Pattern SPREADSHEET_EXTENSION = Pattern.compile("(csv|xls|xlsx)");
  private static final Pattern EXCEL_EXTENSION = Pattern.compile(".*\\.(xls|xlsx)");

  public static PlayPlugin templateLoader;

  @Override
  public Optional<Template> loadTemplate(File file) {
    if (!EXCEL_EXTENSION.matcher(file.getName()).matches()) return Optional.empty();
    if (null == templateLoader) return Optional.of(new ExcelTemplate(file));
    return templateLoader.loadTemplate(file);
  }

  /** Extend play format processing */
  @Override
  public void beforeActionInvocation(
      Request request,
      Response response,
      Session session,
      RenderArgs renderArgs,
      Scope.Flash flash,
      Method actionMethod) {
    Header h = request.headers.get("user-agent");
    if (null == h) return;
    String userAgent = h.value();
    if (IE_USER_AGENT.matcher(userAgent).matches())
      return; // IE678 is tricky!, IE678 is buggy, IE678 is evil!
    if (request.headers.get("accept") != null) {
      String accept = request.headers.get("accept").value();
      if (accept.contains("text/csv")) request.format = "csv";
      if (EXCEL_MIME.matcher(accept).matches()) request.format = "xls";
      if (accept.contains("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
        request.format = "xlsx";
    }
  }

  /** Set response header if needed */
  private static final URLCodec encoder = new URLCodec();

  @Override
  public void onActionInvocationResult(
      Request request, Response response, Session session, RenderArgs renderArgs, Result result) {
    if (null == request.format || !SPREADSHEET_EXTENSION.matcher(request.format).matches()) return;

    if (!response.headers.containsKey("Content-Disposition")) {
      String fileName = renderArgs.get(RenderExcel.RA_FILENAME, String.class);
      if (fileName == null) {
        response.setHeader("Content-Disposition", "attachment; filename=export." + request.format);
      } else {
        try {
          response.setHeader(
              "Content-Disposition", "attachment; filename=" + encoder.encode(fileName, "utf-8"));
        } catch (UnsupportedEncodingException e) {
          throw new UnexpectedException(e);
        }
      }

      if ("xls".equals(request.format)) {
        response.setContentTypeIfNotSet("application/vnd.ms-excel");
      } else if ("xlsx".equals(request.format)) {
        response.setContentTypeIfNotSet(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
      } else if ("csv".equals(request.format)) {
        response.setContentTypeIfNotSet("text/csv");
      }
    }
  }

  public static class ExcelTemplate extends Template {
    private File file;
    private RenderExcel renderExcel;

    public ExcelTemplate(File file) {
      super(Play.relativePath(file));
      this.file = file;
    }

    public ExcelTemplate(RenderExcel render) {
      super(render.getFileName());
      renderExcel = render;
    }

    @Override
    public void compile() {
      if (!file.canRead()) throw new UnexpectedException("template file not readable: " + name);
    }

    @Override
    protected String internalRender(Map<String, Object> args) {
      throw null == renderExcel ? new RenderExcel(file, args) : renderExcel;
    }
  }
}
