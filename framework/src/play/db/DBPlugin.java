package play.db;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.Play;
import play.PlayPlugin;
import play.db.DB.ExtendedDatasource;
import play.exceptions.DatabaseException;
import play.mvc.Http;

import javax.annotation.Nonnull;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.Properties;
import java.util.Set;

public class DBPlugin extends PlayPlugin {
    private static final Logger logger = LoggerFactory.getLogger(DBPlugin.class);

    public static String url = "";
   
    protected DataSourceFactory factory(Configuration dbConfig) {
        String dbFactory = dbConfig.getProperty("db.factory", "play.db.hikaricp.HikariDataSourceFactory");
        try {
            return (DataSourceFactory) Class.forName(dbFactory).getDeclaredConstructor().newInstance();
        }
        catch (Exception e) {
            throw new IllegalArgumentException("Expected implementation of " + DataSourceFactory.class.getName() + 
                ", but received: " + dbFactory);
        }
    }
    
    @Override
    public void onApplicationStart() {
        if (changed()) {
            String dbName = "";
            try {
                // Destroy all connections
                if (!DB.datasources.isEmpty()) {
                    DB.destroyAll();
                }

                Set<String> dbNames = Configuration.getDbNames();
                Iterator<String> it = dbNames.iterator();
                while (it.hasNext()) {
                    dbName = it.next();
                    Configuration dbConfig = new Configuration(dbName);
                    
                    boolean isJndiDatasource = false;
                    String datasourceName = dbConfig.getProperty("db", "");

                    // Identify datasource JNDI lookup name by 'jndi:' or 'java:' prefix 
                    if (datasourceName.startsWith("jndi:")) {
                        datasourceName = datasourceName.substring("jndi:".length());
                        isJndiDatasource = true;
                    }

                    if (isJndiDatasource || datasourceName.startsWith("java:")) {
                        Context ctx = new InitialContext();
                        DataSource ds =  (DataSource) ctx.lookup(datasourceName);
                        DB.datasource = ds;
                        DB.destroyMethod = "";
                        DB.ExtendedDatasource extDs = new DB.ExtendedDatasource(ds, "");
                        DB.datasources.put(dbName, extDs);  
                    } else {

                        // Try the driver
                        String driver = dbConfig.getProperty("db.driver");
                        try {
                            Driver d = (Driver) Class.forName(driver).getDeclaredConstructor().newInstance();
                            DriverManager.registerDriver(new ProxyDriver(d));
                        } catch (Exception e) {
                            throw new Exception("Database [" + dbName + "] Driver not found (" + driver + ")", e);
                        }

                        // Try the connection
                        Connection fake = null;
                        try {
                            if (dbConfig.getProperty("db.user") == null) {
                                fake = DriverManager.getConnection(dbConfig.getProperty("db.url"));
                            } else {
                                fake = DriverManager.getConnection(dbConfig.getProperty("db.url"), dbConfig.getProperty("db.user"), dbConfig.getProperty("db.pass"));
                            }
                        } finally {
                            if (fake != null) {
                                fake.close();
                            }
                        }

                        DataSource ds = factory(dbConfig).createDataSource(dbConfig);
                       
                        // Current datasource. This is actually deprecated. 
                        String destroyMethod = dbConfig.getProperty("db.destroyMethod", "");
                        DB.datasource = ds;
                        DB.destroyMethod = destroyMethod;

                        DB.ExtendedDatasource extDs = new DB.ExtendedDatasource(ds, destroyMethod);

                        url = testDataSource(ds);
                        logger.info("Connected to {} for {}", url, dbName);
                        DB.datasources.put(dbName, extDs);
                    }
                }
                
            } catch (Exception e) {
                DB.datasource = null;
                logger.error("Database [{}] Cannot connected to the database : {}", dbName, e.getMessage(), e);
                if (e.getCause() instanceof InterruptedException) {
                    throw new DatabaseException("Cannot connected to the database["+ dbName + "]. Check the configuration.", e);
                }
                throw new DatabaseException("Cannot connected to the database["+ dbName + "], " + e.getMessage(), e);
            }
        }
    }

    protected String testDataSource(DataSource ds) throws SQLException {
        try (Connection connection = ds.getConnection()) {
            return connection.getMetaData().getURL();
        }
    }
    
    @Override
    public void onApplicationStop() {
        if (Play.mode.isProd()) {
            DB.destroyAll();
        }
    }
    
    @Override
    public void onActionInvocationFinally(@Nonnull Http.Request request) {
        DB.closeAll();
    }

    @Override
    public void onJobInvocationFinally() {
        DB.closeAll();
    }

    private static void check(Configuration config, String mode, String property) {
        if (!StringUtils.isEmpty(config.getProperty(property))) {
            logger.warn("Ignoring {} because running the in {} db.", property, mode);
        }
    }

    private boolean changed() {
        Set<String> dbNames = Configuration.getDbNames();
        
        for (String dbName : dbNames) {
            Configuration dbConfig = new Configuration(dbName);
            String datasourceName = dbConfig.getProperty("db", "");
            DataSource ds = DB.getDataSource(dbName);
                     
            if ((datasourceName.startsWith("java:") || datasourceName.startsWith("jndi:")) && dbConfig.getProperty("db.url") == null) {
                if (ds == null) {
                    return true;
                }
            } else {
                // Internal pool is c3p0, we should call the close() method to destroy it.
                check(dbConfig, "internal pool", "db.destroyMethod");

                dbConfig.put("db.destroyMethod", "close");
            }

            if ((dbConfig.getProperty("db.driver") == null) || (dbConfig.getProperty("db.url") == null)) {
                return false;
            }
            
            if (ds == null) {
                return true;
            } else {
                DataSourceFactory factory = factory(dbConfig);
                
                if (!dbConfig.getProperty("db.driver").equals(factory.getDriverClass(ds))) {
                    return true;
                }
                if (!dbConfig.getProperty("db.url").equals(factory.getJdbcUrl(ds))) {
                    return true;
                }
                if (!dbConfig.getProperty("db.user", "").equals(factory.getUser(ds))) {
                    return true;
                }
            }

            ExtendedDatasource extDataSource = DB.datasources.get(dbName);

            if (extDataSource != null && !dbConfig.getProperty("db.destroyMethod", "").equals(extDataSource.getDestroyMethod())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Needed because DriverManager will not load a driver ouside of the system classloader
     */
    public static class ProxyDriver implements Driver {

        private Driver driver;

        ProxyDriver(Driver d) {
            this.driver = d;
        }

        @Override
        public boolean acceptsURL(String u) throws SQLException {
            return this.driver.acceptsURL(u);
        }

        @Override
        public Connection connect(String u, Properties p) throws SQLException {
            return this.driver.connect(u, p);
        }

        @Override
        public int getMajorVersion() {
            return this.driver.getMajorVersion();
        }

        @Override
        public int getMinorVersion() {
            return this.driver.getMinorVersion();
        }

        @Override
        public DriverPropertyInfo[] getPropertyInfo(String u, Properties p) throws SQLException {
            return this.driver.getPropertyInfo(u, p);
        }

        @Override
        public boolean jdbcCompliant() {
            return this.driver.jdbcCompliant();
        }
      
        // Method not annotated with @Override since getParentLogger() is a new method
        // in the CommonDataSource interface starting with JDK7 and this annotation
        // would cause compilation errors with JDK6.
        @Override
        public java.util.logging.Logger getParentLogger() {
            try {
                return (java.util.logging.Logger) Driver.class.getDeclaredMethod("getParentLogger").invoke(this.driver);
            } catch (Throwable e) {
                return null;
            }
        }
    }
}
