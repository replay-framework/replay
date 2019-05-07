package play.modules.excel;

import net.sf.jxls.transformer.XLSTransformer;
import org.apache.poi.ss.usermodel.Workbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.exceptions.UnexpectedException;
import play.mvc.Http.Request;
import play.mvc.Http.Response;
import play.mvc.Scope.Flash;
import play.mvc.Scope.RenderArgs;
import play.mvc.Scope.Session;
import play.mvc.results.Result;
import play.vfs.VirtualFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import static java.lang.System.nanoTime;
import static java.util.concurrent.TimeUnit.NANOSECONDS;

/**
 * 200 OK with application/excel
 * 
 * This Result try to render Excel file with given template and beans map The
 * code use jxls and poi library to render Excel
 */
@SuppressWarnings("serial")
public class RenderExcel extends Result {
    private static final Logger logger = LoggerFactory.getLogger(RenderExcel.class);

    public static final String RA_FILENAME = "__FILE_NAME__";

    private final VirtualFile file;
    private final String fileName; // recommended report file name
    private final Map<String, Object> beans;

    public RenderExcel(VirtualFile file, Map<String, Object> beans) {
        this(file, beans, null);
    }

    public RenderExcel(VirtualFile file, Map<String, Object> beans, String fileName) {
        this.file = file;
        this.beans = beans;
        this.fileName = fileName == null ? fileName_(file.relativePath()) : fileName;
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
        if (null == excel) {
            logger.debug("use sync excel rendering");
            long start = nanoTime();
            try {
                InputStream is = file.inputstream();
                Workbook workbook = new XLSTransformer()
                        .transformXLS(is, beans);
                workbook.write(response.out);
                is.close();
                logger.debug("Excel sync render takes {}ms", NANOSECONDS.toMillis(nanoTime() - start));
            } catch (Exception e) {
                throw new UnexpectedException(e);
            }
        } else {
            logger.debug("use async excel rendering...");
            try {
                response.out.write(excel);
            } catch (IOException e) {
                throw new UnexpectedException(e);
            }
        }
    }

    private byte[] excel;
}
