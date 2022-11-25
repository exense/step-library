package ch.exense.step.library.kw.system;

import java.io.Closeable;
import java.sql.Connection;
import java.sql.SQLException;

public class DBConnectionWrapper implements Closeable {

    private final Connection connection;

    public DBConnectionWrapper(Connection connection) {
        this.connection = connection;
    }

    public Connection getConnection() {
        return connection;
    }

    @Override
    public void close() {
        try {
            connection.close();
        } catch (SQLException exception) {
            throw new RuntimeException("Unable to close SQL connection", exception);
        }
    }
}
