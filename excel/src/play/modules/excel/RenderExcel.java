package play.modules.excel;

import net.sf.jxls.transformer.XLSTransformer;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Workbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.Play;
import play.exceptions.UnexpectedException;
import play.mvc.Http.Request;
import play.mvc.Http.Response;
import play.mvc.Scope.Flash;
import play.mvc.Scope.RenderArgs;
import play.mvc.Scope.Session;
import play.mvc.results.Result;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import static java.lang.System.nanoTime;
import static java.util.concurrent.TimeUnit.NANOSECONDS;

/**
 * 200 OK with application/excel
 *
 * This Result try to render Excel file with given template and beans map The
 * code use jxls and poi library to render Excel
 */
public class RenderExcel extends Result {
    private static final Logger logger = LoggerFactory.getLogger(RenderExcel.class);

    public static final String RA_FILENAME = "__FILE_NAME__";

    private final File file;
    private final String fileName; // recommended report file name
    private final Map<String, Object> beans;

    public RenderExcel(File file, Map<String, Object> beans) {
        this(file, beans, null);
    }

    public RenderExcel(File file, Map<String, Object> beans, String fileName) {
        this.file = file;
        this.beans = beans;
        this.fileName = fileName == null ? fileName_(Play.relativePath(file)) : fileName;
    }

    public String getFileName() {
        return fileName;
    }

    private static String fileName_(String path) {
        if (RenderArgs.current().data.containsKey(RA_FILENAME))
            return RenderArgs.current().get(RA_FILENAME, String.class);
        int i = path.lastIndexOf("/");
        if (-1 == i)
            return path;
        return path.substring(++i);
    }

    @Override
    public void apply(Request request, Response response, Session session, RenderArgs renderArgs, Flash flash) {
          logger.debug("use sync excel rendering");
          try (InputStream is = new FileInputStream(file)) {
              long start = nanoTime();
              Map<String, Object> args = new HashMap<>(beans);
              Workbook workbook = new XLSTransformer().transformXLS(is, args);
              workbook.write(response.out);
              logger.debug("Excel sync render takes {}ms", NANOSECONDS.toMillis(nanoTime() - start));
          } catch (IOException | InvalidFormatException e) {
              throw new UnexpectedException("Failed to generate Excel file from template " + file.getAbsolutePath(), e);
          }
    }

    @Override
    public boolean isRenderingTemplate() {
        return true;
    }
}
