package ch.exense.step.library.kw.system;

import ch.exense.step.library.commons.AbstractEnhancedKeyword;
import jakarta.json.Json;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObjectBuilder;
import step.core.accessors.Attribute;
import step.handlers.javahandler.AbstractKeyword;
import step.handlers.javahandler.Keyword;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Attribute(key = "category",value = "Database")
public class DatabaseKeywords extends AbstractEnhancedKeyword {

    private Connection getConnection() throws SQLException {
        DBConnectionWrapper dbConnectionWrapper = session.get(DBConnectionWrapper.class);
        //if there is no connection yet, the connections has been closed or became invalid
        if (dbConnectionWrapper == null || dbConnectionWrapper.getConnection().isClosed() ||
                !dbConnectionWrapper.getConnection().isValid(3)) {
            //close if previous is invalid
            if (dbConnectionWrapper != null && !dbConnectionWrapper.getConnection().isClosed()) {
                dbConnectionWrapper.getConnection().close();
            }
            String connectionString = input.getString("ConnectionString");
            String username = input.getString("Username");
            String password = input.getString("Password");

            Connection connection;
            if (username != null) {
                connection = DriverManager.getConnection(connectionString, username, password);
            } else {
                connection = DriverManager.getConnection(connectionString);
            }
            dbConnectionWrapper = new DBConnectionWrapper(connection);
            session.put(dbConnectionWrapper);
        }
        return dbConnectionWrapper.getConnection();
    }

    @Keyword(schema = "{\"properties\":{"
            + "\"ConnectionString\":{\"type\":\"string\"},"
            + "\"Username\":{\"type\":\"string\"},"
            + "\"Password\":{\"type\":\"string\"},"
            + "\"Query\":{\"type\":\"string\"},"
            + "\"ResultLimit\":{\"type\":\"string\"}"
            + "},\"required\":[\"ConnectionString\",\"Query\"]}", properties = {""},
            description = "Keyword used for executing a db query.")
    public void ExecuteQuery() throws SQLException {
        int resultLimit = Integer.decode(input.getString("ResultLimit", "10"));

        String query = input.getString("Query");
        Connection con = getConnection();
        try (Statement statement = con.createStatement()) {
            boolean isResultSet = statement.execute(query);
            if (isResultSet) {
                ResultSet rs = statement.getResultSet();
                ResultSetMetaData md = rs.getMetaData();
                output.add("ColumnCount", md.getColumnCount());

                int columns = md.getColumnCount();
                List<Map<String, ?>> results = new ArrayList<>();
                int count = 0;

                while (rs.next() && count < resultLimit) {
                    count++;
                    Map<String, Object> row = new HashMap<>();
                    for (int i = 1; i <= columns; i++) {
                        row.put(md.getColumnLabel(i).toUpperCase(), rs.getObject(i));
                    }
                    results.add(row);
                }

                if (input.getString("ResultFormat", "json").equals("json")) {
                    JsonArrayBuilder arrayBuilder = Json.createArrayBuilder();
                    results.forEach(row -> {
                        JsonObjectBuilder objectBuilder = Json.createObjectBuilder();
                        row.forEach((key, value) -> objectBuilder.add(key, value != null ? value.toString() : "null"));
                        arrayBuilder.add(objectBuilder.build());
                    });
                    output.add("ResultAsJson", arrayBuilder.build().toString());
                }

                rs.close();
            } else {
                output.add("UpdateCount", statement.getUpdateCount());
            }
        }
    }
}
