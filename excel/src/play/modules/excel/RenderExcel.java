package play.modules.excel;

import net.sf.jxls.transformer.XLSTransformer;
import org.apache.poi.ss.usermodel.Workbook;
import play.Logger;
import play.exceptions.UnexpectedException;
import play.mvc.Http.Request;
import play.mvc.Http.Response;
import play.mvc.Scope.RenderArgs;
import play.mvc.results.Result;
import play.vfs.VirtualFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

/**
 * 200 OK with application/excel
 * 
 * This Result try to render Excel file with given template and beans map The
 * code use jxls and poi library to render Excel
 */
@SuppressWarnings("serial")
public class RenderExcel extends Result {

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
    public void apply(Request request, Response response) {
        if (null == excel) {
            Logger.debug("use sync excel rendering");
            long start = System.currentTimeMillis();
            try {
                InputStream is = file.inputstream();
                Workbook workbook = new XLSTransformer()
                        .transformXLS(is, beans);
                workbook.write(response.out);
                is.close();
                Logger.debug("Excel sync render takes %sms", System.currentTimeMillis() - start);
            } catch (Exception e) {
                throw new UnexpectedException(e);
            }
        } else {
            Logger.debug("use async excel rendering...");
            try {
                response.out.write(excel);
            } catch (IOException e) {
                throw new UnexpectedException(e);
            }
        }
    }

    private byte[] excel;
}
