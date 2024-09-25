package play.db;

import java.beans.PropertyVetoException;
import java.sql.SQLException;
import javax.sql.DataSource;

public interface DataSourceFactory {
  DataSource createDataSource(Configuration dbConfig) throws PropertyVetoException, SQLException;

  String getStatus() throws SQLException;

  String getDriverClass(DataSource ds);

  String getJdbcUrl(DataSource ds);

  String getUser(DataSource ds);
}
