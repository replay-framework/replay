package play.db.helper;

import play.db.DB;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class JdbcHelper {

    private JdbcHelper() {
    }

    public static ResultSet execute(String sql, Object ... params) throws SQLException {
        PreparedStatement pst = DB.getConnection().prepareStatement(sql, ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
        int index = 0;
        for (Object param : params) {
            pst.setObject(++index, param);
        }
        return pst.executeQuery();
    }

    public static ResultSet executeList(String sql, List<Object> params) throws SQLException {
        PreparedStatement pst = DB.getConnection().prepareStatement(sql, ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
        int index = 0;
        for (Object param : params) {
            pst.setObject(++index, param);
        }
        return pst.executeQuery();
    }

    public static ResultSet execute(SqlQuery query) throws SQLException {
        return executeList(query.toString(), query.getParams());
    }

}
