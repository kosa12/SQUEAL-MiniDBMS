package server;

import com.mongodb.client.*;

import data.Attribute;
import data.Database;
import data.Table;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import org.bson.Document;

public class Server extends Thread {
    private ServerSocket serverSocket;
    private static final HashMap<String, Database> databases = new HashMap<>();
    private static String currentDatabase;
    private final boolean isRunning = true;
    private static MongoClient mongoClient;
    private Socket clientSocket;

    @Override
    public void run() {
        mongoClient = MongoClients.create("mongodb://localhost:27017");
        recreateFromJson("src/test/java/databases/");

        for (String dbName : databases.keySet()) {
            System.out.println(dbName);
        }

        try {
            serverSocket = new ServerSocket(10000);
            System.out.println("Server is running...");
            while (isRunning) {
                try {
                    clientSocket = serverSocket.accept();
                    System.out.println("Client connected: " + clientSocket.getInetAddress().getHostAddress());
                    new Thread(() -> handleClient(clientSocket)).start();
                    clientSocket.setSoTimeout(500000);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void recreateFromMongoDB(String databaseName) {
        MongoDatabase mongoDatabase = mongoClient.getDatabase(databaseName);
        for (String collectionName : mongoDatabase.listCollectionNames()) {
            MongoCollection<Document> collection = mongoDatabase.getCollection(collectionName);
            Table table = databases.getOrDefault(databaseName, new Database(databaseName)).getTable(collectionName);
            if (table == null && !collectionName.contains("index")) {
                System.out.println("Table not found in database: " + collectionName);
                continue;
            }
            if (!collectionName.contains("index")) {
                FindIterable<Document> documents = collection.find();
                for (Document document : documents) {
                    Object idValue = document.get("_id");
                    if (idValue != null) {
                        String primaryKeyAttribute = idValue.toString();
                        table.addPKtoList(primaryKeyAttribute);
                    } else {
                        System.out.println("No value found for _id field in document.");
                    }
                }
            }
        }
    }

    public void recreateFromJson(String jsonPath) {
        JSONParser parser = new JSONParser();

        try {
            File databasesDir = new File(jsonPath);
            if (!databasesDir.exists() || !databasesDir.isDirectory()) {
                System.out.println("Invalid directory: " + jsonPath);
                return;
            }

            File[] databaseFiles = databasesDir.listFiles((dir, name) -> name.endsWith(".json") && !name.contains("index"));
            if (databaseFiles == null) {
                System.out.println("No database files found in directory: " + jsonPath);
                return;
            }

            for (File databaseFile : databaseFiles) {



                try (FileReader reader = new FileReader(databaseFile);){

                    Object obj = parser.parse(reader);
                    JSONArray databaseArray = (JSONArray) obj;

                    for (Object databaseObj : databaseArray) {
                        JSONObject databaseJson = (JSONObject) databaseObj;

                        String databaseName = (String) databaseJson.get("database_name");

                        Database database = new Database(databaseName);

                        JSONArray tablesArray = (JSONArray) databaseJson.get("tables");
                        if (tablesArray != null) {
                            for (Object tableObj : tablesArray) {
                                JSONObject tableJson = (JSONObject) tableObj;
                                String tableName = (String) tableJson.get("table_name");

                                Table table = new Table(tableName, "", null);

                                JSONArray attributesArray = (JSONArray) tableJson.get("attributes");
                                for (Object attributeObj : attributesArray) {
                                    JSONObject attributeJson = (JSONObject) attributeObj;
                                    String attributeName = (String) attributeJson.get("name");
                                    String attributeType = (String) attributeJson.get("type");
                                    boolean attributeNotNull = (boolean) attributeJson.get("not_null");
                                    boolean attributeIsPK = (boolean) attributeJson.get("is_pk");
                                    boolean attributeIsFK = (boolean) attributeJson.get("is_fk");
                                    JSONArray fkKeys = (JSONArray) attributeJson.get("is_referenced_by_fk");

                                    if (attributeIsPK) {
                                        table.setpKAttrName(attributeName);
                                    }

                                    table.addAttribute(new Attribute(attributeName, attributeType, attributeNotNull, attributeIsPK, attributeIsFK, fkKeys));
                                }
                                database.addTable(table);

                            }
                        }
                        databases.put(databaseName, database);
                        recreateFromMongoDB(databaseName);

                    }
                } catch (IOException | ParseException e) {
                    throw new RuntimeException(e);
                }

            }
        } catch (RuntimeException e) {
            throw new RuntimeException(e);
        }

    }

    public ServerSocket getServerSocket() {
        return serverSocket;
    }

    private static void handleClient(Socket clientSocket) {
        try (
                BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)
        ) {
            StringBuilder commandBuilder = new StringBuilder();
            String line;
            while ((line = in.readLine()) != null) {
                if (!line.trim().startsWith("--")) {
                    if (line.trim().endsWith(";")) {
                        MongoDBHandler mongoDBHandler = new MongoDBHandler();
                        if (line.startsWith("FETCH")) {
                            String[] parts = line.split("\\s+");
                            if (parts.length >= 4) {
                                String databaseName = parts[1];
                                String tableName = parts[2];
                                String[] attributeNames = parts[3].split(",");

                                List<String[]> rows = mongoDBHandler.fetchRows(databaseName, tableName, attributeNames);
                                if (rows != null) {
                                    for (String[] row : rows) {
                                        out.println(String.join(",", row));
                                    }
                                }
                            }
                        }

                        out.println(line);
                        commandBuilder.append(line.trim(), 0, line.lastIndexOf(';'));
                        String command = commandBuilder.toString().trim();
                        if (command.trim().isEmpty()) {
                            return;
                        }

                        String[] parts = command.trim().split("\\s+");
                        if (parts.length == 2 && parts[0].equalsIgnoreCase("SHOW")) {
                            String objectType = parts[1].toUpperCase();
                            if (objectType.equals("DATABASES")) {
                                showDatabases(out);
                            } else if (objectType.equals("TABLES")) {
                                showTables(out);
                            } else {
                                out.println("Invalid SHOW command: " + command);
                            }
                        } else if (parts.length >= 4 && parts[0].equalsIgnoreCase("INSERT") && parts[1].equalsIgnoreCase("INTO")) {
                            insertRow(command, out, clientSocket);
                        } else if (parts.length >= 4 && parts[0].equalsIgnoreCase("DELETE") && parts[1].equalsIgnoreCase("FROM")) {
                            deleteRow(command, out);
                        } else if (parts.length >= 3) {
                            String operation = parts[0].toLowerCase();
                            String objectType = parts[1].toLowerCase();
                            String objectName = parts[2];

                            if (operation.equals("create") || operation.equals("drop")) {
                                if (objectType.equals("database") || objectType.equals("db")) {
                                    handleDatabaseOperation(operation, objectName, out);
                                } else if (objectType.equals("table") || objectType.equals("index")) {
                                    handleTableOperation(operation, command, out);
                                } else {
                                    out.println("Invalid object type: " + objectType);
                                }
                            } else {
                                out.println("Invalid operation: " + operation);
                            }
                        } else if (parts.length == 2) {
                            String operation = parts[0].toLowerCase();
                            String objectName = parts[1];
                            if (operation.equals("use")) {
                                handleUseDatabase(objectName, out);
                            } else {
                                out.println("Invalid operation: " + operation);
                            }
                        } else {
                            out.println("Invalid message format: " + command);
                        }
                        commandBuilder.setLength(0);
                    } else {
                        commandBuilder.append(line);
                    }
                }

            }
            clientSocket.close();
            out.println("Client disconnected: " + clientSocket.getInetAddress().getHostAddress());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void showDatabases(PrintWriter out) {
        for (String dbName : databases.keySet()) {
            out.println(dbName);
        }
    }

    private static void showTables(PrintWriter out) {
        if (currentDatabase == null) {
            out.println("No database selected.");
            return;
        }

        Database db = databases.get(currentDatabase);
        if (db != null) {
            for (Table table : db.getTables()) {
                out.println(table.getTableName());
            }
        }
    }

    private static void handleUseDatabase(String databaseName, PrintWriter out) {
        Database db = databases.get(databaseName);
        if (db != null) {
            currentDatabase = databaseName;
            System.out.println("Using database: " + databaseName);
            out.println("> Using database: " + databaseName);
        } else {
            System.out.println("Database not found: " + databaseName);
            out.println("> Database " + "'" + databaseName + "'" + " does not exist. Make sure you entered the name correctly.");
        }
    }

    private static void handleTableOperation(String operation, String command, PrintWriter out) {
        if (currentDatabase == null) {
            System.out.println("No database selected.");
            out.println("> No database selected. Please select one.\nNote: you can see the existing databases using command: 'show databases'");
            return;
        }

        if (operation.equalsIgnoreCase("create")) {
            if (command.toLowerCase().contains("index")) {
                createIndex(command, out);
            } else {
                createTable(command, out);
            }
        } else if (operation.equalsIgnoreCase("drop")) {
            dropTable(command, out);
        } else {
            System.out.println("Invalid table operation: " + operation);
            out.println("> Invalid table operation: " + operation);
        }
    }

    private static void insertRow(String command, PrintWriter out, Socket clientSocket) {
        if (!command.toLowerCase().contains("insert into")) {
            out.println("> Invalid INSERT command format: Missing 'INSERT INTO' keyword.");
            return;
        }

        if (!command.toLowerCase().contains("values")) {
            out.println("> Invalid INSERT command format: Missing 'VALUES' keyword.");
            return;
        }

        int tableNameIndex = command.toLowerCase().indexOf("insert into") + "insert into".length();
        int columnsStartIndex = command.indexOf("(", tableNameIndex);
        int columnsEndIndex = command.indexOf(")", columnsStartIndex);
        if (columnsStartIndex == -1 || columnsEndIndex == -1) {
            out.println("> Invalid INSERT command format: Missing '(' or ')' for column names.");
            return;
        }

        String tableName = command.substring(tableNameIndex, columnsStartIndex).trim();
        String columnsPart = command.substring(columnsStartIndex + 1, columnsEndIndex);
        String[] columns = columnsPart.split(",");

        int valuesStartIndex = command.indexOf("(", columnsEndIndex);
        int valuesEndIndex = command.lastIndexOf(")");
        if (valuesStartIndex == -1 || valuesEndIndex == -1) {
            out.println("> Invalid INSERT command format: Missing '(' or ')' for values.");
            return;
        }

        String valuesPart = command.substring(valuesStartIndex + 1, valuesEndIndex);
        String[] values = valuesPart.split(",");

        if (columns.length != values.length) {
            out.println("> Number of columns does not match number of values provided.");
            return;
        }

        Table table = databases.get(currentDatabase).getTable(tableName);
        if (table == null) {
            out.println("> Table '" + tableName + "' does not exist.\nNote: you can see the existing tables using command 'show tables'");
            return;
        }

        StringBuilder concatenatedValue = new StringBuilder();
        String primaryKeyValue = "";
        for (int i = 0; i < values.length; i++) {
            String value = values[i].trim();
            Attribute attribute = table.getAttributes().get(i);
            if (attribute == null) {
                out.println("> Column '" + columns[i].trim() + "' does not exist.");
                return;
            }
            String attributeName = attribute.getAttributeName();
            String attributeType = attribute.getType();
            if (attributeType.toLowerCase().contains("varchar")) {
                attributeType = "varchar";
            }

            if (convertValue(value, attributeType) == null) {
                out.println("> Invalid value type for attribute: " + attributeName);
                return;
            }

            if (i == 0) {
                primaryKeyValue = value;
            } else {
                concatenatedValue.append(value);
                if (i < values.length - 1) {
                    concatenatedValue.append(";");
                }
            }
        }

        if (table.hasPrimaryKeyValue(primaryKeyValue)) {
            out.println("> Primary key value " + primaryKeyValue + " already exists. It has to be unique.");
            return;
        }

        Document document = new Document();
        document.append("_id", primaryKeyValue);
        document.append("ertek", concatenatedValue.toString());

        String collectionName = tableName.toLowerCase();

        MongoDBHandler mongoDBHandler = new MongoDBHandler();
        mongoDBHandler.insertDocument(currentDatabase, collectionName, document);
        mongoDBHandler.close();
        out.println("> Row inserted into MongoDB collection: " + collectionName);
    }


    private static void deleteRow(String command, PrintWriter out) {
        if (!command.toLowerCase().contains("delete from")) {
            out.println("> Invalid DELETE command format: Missing 'DELETE FROM' keyword.");
            return;
        }

        if (!command.toLowerCase().contains("where")) {
            out.println("> Invalid DELETE command format: Missing 'WHERE' keyword.");
            return;
        }

        String[] parts = command.trim().split("\\s+");
        String tableName = parts[2];
        String condition = parts[parts.length - 1];

        if (!condition.startsWith("_id")) {
            out.println("> Invalid DELETE condition: Must be based on primary key (_id).");
            return;
        }

        String primaryKeyValue = condition.substring(4);

        MongoDBHandler mongoDBHandler = new MongoDBHandler();
        long deletedCount = mongoDBHandler.deleteDocumentByPK(currentDatabase, tableName, primaryKeyValue);
        mongoDBHandler.close();

        Table table = databases.get(currentDatabase).getTable(tableName);
        if (table != null) {
            if (table.removePrimaryKeyValue(primaryKeyValue)) {
                if (deletedCount > 0) {
                    out.println("> Deleted " + deletedCount + " document(s) from table: " + tableName);
                } else {
                    out.println("> No document found with primary key value: " + primaryKeyValue);
                }
            } else {
                out.println("> Primary key value not found in table: " + tableName);
            }
        } else {
            out.println("> Table '" + tableName + "' does not exist.\nNote: you can see the existing tables using command 'show tables'");
        }
    }

    private static Object convertValue(String value, String type) {
        switch (type.toLowerCase()) {
            case "int":
                return Integer.parseInt(value);
            case "float":
                return Float.parseFloat(value);
            case "bit":
                return Boolean.parseBoolean(value);
            case "date", "datetime", "varchar":
                return value;
            default:
                return null;
        }
    }


    private static void createTable(String command, PrintWriter out) {
        String[] parts = command.split("\\s+");
        for (int i = 0; i < parts.length; i++) {
            parts[i] = parts[i].trim();
        }

        if (parts.length < 4 || !parts[0].equalsIgnoreCase("CREATE") || !parts[1].equalsIgnoreCase("TABLE")) {
            out.println("> Invalid CREATE TABLE command format.\nCorrect format: create table _tablename_ (attr_1 type_1, ..., attr_n type_n);");
            return;
        }

        String tableName = parts[2];

        if (!command.contains("(") || !command.contains(")")) {
            out.println("> Invalid CREATE TABLE command format: Missing '(' or ')' for column definitions.\nCorrect format: create table _tablename_ (attr_1 type_1, ..., attr_n type_n);");
            return;
        }

        String columnDefinitions = command.substring(command.indexOf("(") + 1, command.lastIndexOf(")"));

        String[] columns = columnDefinitions.split(",(?![^(]*\\))");

        if (columns.length == 0) {
            out.println("> Invalid CREATE TABLE command format: No column definitions found.\nCorrect format: create table _tablename_ (attr_1 type_1, ..., attr_n type_n);");
            return;
        }

        if (databases.get(currentDatabase).hasTable(tableName)) {
            out.println("> Table '" + tableName + " already exists.");
            return;
        }

        String primaryKeyAttributeName = null;
        HashSet<String> foreignKeyAttributeNames = new HashSet<>();

        JSONArray tableColumns = new JSONArray();
        boolean hasPrimaryKey = false;
        Table table = new Table(tableName, "", null);
        for (String column : columns) {
            String[] columnParts = column.trim().split("\\s+");
            if (columnParts.length < 2) {
                out.println("> Invalid column definition: " + column);
                return;
            }

            boolean goodSyntax = false;

            if (isValidColumnType(columnParts[1])) {
                goodSyntax = true;
            } else if (columnParts[1].matches("(?i)varchar\\(\\d+\\)")) {
                String lengthStr = columnParts[1].substring(8, columnParts[1].length() - 1);
                try {
                    int length = Integer.parseInt(lengthStr);
                    if (length <= 0) {
                        out.println("> Invalid length for varchar: " + length);
                        return;
                    }
                    goodSyntax = true;
                } catch (NumberFormatException e) {
                    out.println("> Invalid length for varchar: " + lengthStr);
                    return;
                }
            } else {
                out.println("> Invalid column type: " + columnParts[1]);
            }

            if (!goodSyntax) {
                return;
            }
            JSONObject columnObj = new JSONObject();
            String attributeName = columnParts[0];
            String attributeType = columnParts[1];
            boolean notNull = false;
            boolean isPK = false;
            boolean isForeignKey = false;

            String referencedTableName = "";
            String referencedAttributeName = "";
            String fkName = "";
            String fkTableName = "";

            for (int i = 2; i < columnParts.length; i++) {
                String keyword = columnParts[i].toUpperCase();
                if (keyword.equalsIgnoreCase("NOT")) {
                    if (i + 1 < columnParts.length && columnParts[i + 1].equalsIgnoreCase("NULL")) {
                        notNull = true;
                        i++;
                    } else {
                        out.println("> Invalid keyword after NOT: " + columnParts[i + 1]);
                        return;
                    }
                } else if (keyword.equalsIgnoreCase("PRIMARY") && i + 1 < columnParts.length && columnParts[i + 1].equalsIgnoreCase("KEY")) {
                    if (hasPrimaryKey) {
                        out.println("> Only one primary key is allowed.");
                        return;
                    }
                    isPK = true;
                    hasPrimaryKey = true;
                    i++;
                } else if (keyword.equalsIgnoreCase("FOREIGN") && i + 1 < columnParts.length && columnParts[i + 1].equalsIgnoreCase("KEY")) {
                    if (columnParts.length < 6 || !columnParts[4].equalsIgnoreCase("REFERENCES")) {
                        out.println("> Invalid FOREIGN KEY syntax.");
                        return;
                    }

                    String referencedTableAndAttr = columnParts[5];
                    String[] referencedParts = referencedTableAndAttr.split("[()]");
                    if (referencedParts.length != 2) {
                        out.println("> Invalid syntax for referenced table and attribute: " + referencedTableAndAttr);
                        return;
                    }
                    referencedTableName = referencedParts[0];
                    referencedAttributeName = referencedParts[1];

                    if (!databases.get(currentDatabase).hasTable(referencedTableName)) {
                        out.println("> Referenced table '" + referencedTableName + "' does not exist.");
                        return;
                    }

                    if (!databases.get(currentDatabase).getTable(referencedTableName).hasAttribute(referencedAttributeName)) {
                        out.println("> Referenced attribute '" + referencedAttributeName + "' does not exist in table '" + referencedTableName + "'.");
                        return;
                    }

                    isForeignKey = true;
                    foreignKeyAttributeNames.add(attributeName);
                    fkName = attributeName;
                    fkTableName = tableName;

                    i+=5;
                } else {
                    out.println("> Invalid keyword in column definition: " + columnParts[i]);
                    return;
                }
            }

            columnObj.put("name", attributeName);
            columnObj.put("type", attributeType);
            columnObj.put("is_pk", isPK);
            columnObj.put("not_null", notNull);
            columnObj.put("is_fk", isForeignKey);
            columnObj.put("is_referenced_by_fk", new JSONArray());


            if (isForeignKey) {
                String path = "src/test/java/databases/" + currentDatabase + ".json";
                JSONArray currentDatabaseJson = getCurrentDatabaseJson(path);
                if (currentDatabaseJson == null) {
                    out.println("> Error: Unable to load current database JSON.");
                    return;
                }

                JSONArray tablesArray = (JSONArray) ((JSONObject) currentDatabaseJson.get(0)).get("tables");

                JSONObject referencedTableJson = null;

                for (Object tableObj : tablesArray) {
                    JSONObject tableJson = (JSONObject) tableObj;
                    String tableName2 = (String) tableJson.get("table_name");

                    if (tableName2.equals(referencedTableName)) {
                        referencedTableJson = tableJson;
                        break;
                    }
                }

                if (referencedTableJson == null) {
                    out.println("> Error: Referenced table '" + referencedTableName + "' not found.");
                    return;
                }

                JSONArray columnsArray = (JSONArray) referencedTableJson.get("attributes");
                int attributeIndex = -1;

                for (int i = 0; i < columnsArray.size(); i++) {
                    JSONObject columnJson = (JSONObject) columnsArray.get(i);
                    String columnName = (String) columnJson.get("name");

                    if (columnName.equals(referencedAttributeName)) {
                        attributeIndex = i;
                        break;
                    }
                }

                if (attributeIndex == -1) {
                    out.println("> Error: Referenced attribute '" + referencedAttributeName + "' not found in the referenced table.");
                    return;
                }

                JSONObject fkReferenceJson = new JSONObject();
                fkReferenceJson.put("fkName", fkName);
                fkReferenceJson.put("fkTableName", fkTableName);

                JSONArray isReferencedByFkArray = getJsonArray(columnsArray, attributeIndex);
                isReferencedByFkArray.add(fkReferenceJson);

                FileWriter writer = null;
                try {
                    writer = new FileWriter(path);

                    for (int i = 0; i < tablesArray.size(); i++) {
                        JSONObject tableObj = (JSONObject) tablesArray.get(i);
                        String tableName2 = (String) tableObj.get("table_name");
                        if (tableName2.equals(referencedTableName)) {
                            tablesArray.set(i, referencedTableJson);
                            break;
                        }
                    }

                    writer.write(currentDatabaseJson.toJSONString());
                } catch (IOException e) {
                    out.println("> Error: Unable to write to current database JSON file.");
                    return;
                } finally {
                    if (writer != null) {
                        try {
                            writer.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }

            tableColumns.add(columnObj);
        }


        HashSet<String> columnNames = new HashSet<>();
        for (Object obj : tableColumns) {
            JSONObject column = (JSONObject) obj;
            String attributeName = (String) column.get("name");
            if (!columnNames.add(attributeName)) {
                out.println("> Duplicate column name: " + attributeName);
                return;
            }
        }

        for (Object obj : tableColumns) {
            JSONObject column = (JSONObject) obj;
            String attributeName = (String) column.get("name");
            String attributeType = (String) column.get("type");
            boolean notNull = (boolean) column.get("not_null");
            boolean isPK = (boolean) column.get("is_pk");
            boolean isfk = (boolean) column.get("is_fk");
            JSONArray fkKeys = (JSONArray) column.get("is_referenced_by_fk") ;
            table.addAttribute(new Attribute(attributeName, attributeType, notNull, isPK, isfk, fkKeys));
        }

        databases.get(currentDatabase).addTable(table);
        JSONObject tableObj = new JSONObject();
        tableObj.put("table_name", tableName);
        tableObj.put("attributes", tableColumns);
        updateDatabaseWithTable(tableName, tableObj, out);
    }

    private static JSONArray getJsonArray(JSONArray columnsArray, int attributeIndex) {
        JSONArray isReferencedByFkArray;
        JSONObject attributeJson = (JSONObject) columnsArray.get(attributeIndex);
        if (attributeJson.containsKey("is_referenced_by_fk")) {
            Object isReferencedByFkObj = attributeJson.get("is_referenced_by_fk");
            isReferencedByFkArray = (JSONArray) isReferencedByFkObj;
        } else {
            isReferencedByFkArray = new JSONArray();
            attributeJson.put("is_referenced_by_fk", isReferencedByFkArray);
        }
        return isReferencedByFkArray;
    }

    private static JSONArray getCurrentDatabaseJson(String jsonPath) {
        JSONParser jsonParser = new JSONParser();

        try (FileReader reader = new FileReader(jsonPath)) {
            Object obj = jsonParser.parse(reader);
            if (obj instanceof JSONArray) {
                JSONArray jsonArray = (JSONArray) obj;
                if (jsonArray.size() > 0) {
                    return jsonArray;
                } else {
                    System.err.println("Error: The database JSON array is empty.");
                    return null;
                }
            } else {
                System.err.println("Error: The database JSON file does not contain a valid JSON array.");
                return null;
            }
        } catch (IOException | ParseException e) {
            System.err.println("Error reading database JSON file: " + e.getMessage());
            return null;
        }
    }




    private static boolean isValidColumnType(String type) {
        String[] validTypes = {"int", "float", "bit", "date", "datetime"};
        for (String validType : validTypes) {
            if (validType.equalsIgnoreCase(type)) {
                return true;
            }
        }
        return false;
    }

    private static void dropTable(String command, PrintWriter out) {
        String[] parts = command.split("\\s+");
        for (int i = 0; i < parts.length; i++) {
            parts[i] = parts[i].trim();
        }

        if (parts.length != 3 || !parts[0].equalsIgnoreCase("DROP") || !parts[1].equalsIgnoreCase("TABLE")) {
            System.out.println("Invalid DROP TABLE command format.");
            out.println("> Invalid DROP TABLE command format.\nCorrect format: drop table _tablename_;");
            return;
        }

        String tableName = parts[2];

        JSONParser parser = new JSONParser();
        FileReader fileReader = null;
        FileWriter fileWriter = null;

        try {
            File databaseFile = new File("src/test/java/databases/" + currentDatabase + ".json");
            if (!databaseFile.exists()) {
                System.out.println("Database not found: " + currentDatabase);
                out.println("> Database " + "'" + currentDatabase + "'" + " does not exist. Make sure you entered the name correctly.");
                return;
            }

            fileReader = new FileReader(databaseFile);
            Object obj = parser.parse(fileReader);
            JSONArray databaseJson = (JSONArray) obj;

            JSONObject databaseObj = (JSONObject) databaseJson.getFirst();
            JSONArray tablesArray = (JSONArray) databaseObj.get("tables");
            boolean tableFound = false;
            int tableIndex = -1;
            for (int i = 0; i < tablesArray.size(); i++) {
                JSONObject tableObj = (JSONObject) tablesArray.get(i);
                if (tableObj.get("table_name").equals(tableName)) {
                    tableFound = true;
                    tableIndex = i;
                    break;
                }
            }

            if (tableFound) {
                tablesArray.remove(tableIndex);
                try {
                    fileReader.close();
                    fileWriter = new FileWriter(databaseFile);
                    fileWriter.write(databaseJson.toJSONString() + "\n");
                } catch (IOException e) {
                    e.printStackTrace();
                }

                System.out.println("Table dropped: " + tableName);
                out.println("> Table '" + tableName + "' dropped.");
                databases.get(currentDatabase).dropTable(tableName);
            }
        } catch (IOException | ParseException e) {
            e.printStackTrace();
        } finally {
            try {
                if (fileReader != null) {
                    fileReader.close();
                }
                if (fileWriter != null) {
                    fileWriter.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static void createIndex(String command, PrintWriter out) {
        String[] parts = command.split("\\s+");
        if (parts.length < 5 || !parts[0].equalsIgnoreCase("CREATE") || !parts[1].equalsIgnoreCase("INDEX")) {
            System.out.println("Invalid CREATE INDEX command format.");
            out.println("> Invalid CREATE INDEX command format.\nCorrect format: create index _indexname_ on _tablename_ (attr_1, ..., attr_n)");
            return;
        }

        String indexName = parts[2];
        String tableName = parts[4];

        String columnsStr = command.substring(command.indexOf("(") + 1, command.lastIndexOf(")")).trim();
        String[] columns = columnsStr.split(",");


        MongoDBHandler mongoDBHandler = new MongoDBHandler();
        try {
            List<Document> records = mongoDBHandler.fetchDocuments(currentDatabase, tableName);

            if (mongoDBHandler.indexExists(currentDatabase, indexName + "-" + String.join("-", columns) + "-index")) {
                System.out.println("Index name '" + indexName + "-" + String.join("-", columns) + "-index' already exists in MongoDB");
                out.println("> Index name '" + indexName + "-" + String.join("-", columns) + "-index' already exists in MongoDB");
                return;
            }

            if (records.isEmpty()) {
                System.out.println("No records found in MongoDB for table '" + tableName + "'");
                out.println("> No records found in MongoDB for table '" + tableName + "'");
                return;
            }

            JSONObject tableFormat = readTableFormat(currentDatabase, tableName, out);
            if (tableFormat == null) {
                System.out.println("Table format not found for table '" + tableName + "'");
                out.println("> Table format not found for table '" + tableName + "'");
                return;
            }

            List<Integer> indexKeys = getIndexKeys(tableFormat, columns);
            if (indexKeys.contains(-1)) {
                System.out.println("One or more columns not found in table format for table " + tableName);
                out.println("> One or more columns not found in table format for table '" + tableName + "'");
                return;
            }

            for (Document record : records) {
                String primaryKeyValue = record.getString("_id");
                String valuesFromRecord = record.getString("ertek");
                String everything = primaryKeyValue + ";" +valuesFromRecord;
                String[] values = everything.split(";");

                StringBuilder indexKeyBuilder = new StringBuilder();
                for (int indexKey : indexKeys) {
                    indexKeyBuilder.append(values[indexKey]).append(";");
                }

                String compositeIndexKey = indexKeyBuilder.toString();

                Document existingDocument = mongoDBHandler.getDocumentByIndex(currentDatabase, indexName + "-" + String.join("-", columns) + "-index", "_id", compositeIndexKey);

                if (existingDocument != null) {
                    String existingPrimaryKey = existingDocument.getString("ertek");
                    existingPrimaryKey += ";" + primaryKeyValue;
                    existingDocument.put("ertek", existingPrimaryKey);
                    mongoDBHandler.updateDocument(currentDatabase, indexName + "-" + String.join("-", columns) + "-index", "_id", compositeIndexKey, "ertek", existingPrimaryKey);
                } else {
                    Document document = new Document();
                    document.append("_id", compositeIndexKey);
                    document.append("ertek", primaryKeyValue);
                    mongoDBHandler.insertDocument(currentDatabase, indexName + "-" + String.join("-", columns) + "-index", document);
                }
            }

            System.out.println("Index created: " + indexName + " on columns " + columnsStr + " in table " + tableName);
            out.println("> Index '" + indexName + "' created on column(s) " + columnsStr + " in table '" + tableName + "'");
        } catch (Exception ex) {
            System.out.println("Failed to create index in MongoDB: " + ex.getMessage());
            out.println("> Failed to create index in MongoDB: " + ex.getMessage());
        } finally {
            mongoDBHandler.close();
        }
    }

    private static List<Integer> getIndexKeys(JSONObject tableFormat, String[] columns) {
        List<Integer> indexKeys = new ArrayList<>();
        for (String column : columns) {
            column = column.trim();
            int indexKey = getIndexKey(tableFormat, column);
            if (indexKey == -1) {
                return Collections.singletonList(-1);
            }
            indexKeys.add(indexKey);
        }
        return indexKeys;
    }


    private static int getIndexKey(JSONObject tableFormat, String column) {
        JSONArray attributes = (JSONArray) tableFormat.get("attributes");
        for (int i = 0; i < attributes.size(); i++) {
            JSONObject attribute = (JSONObject) attributes.get(i);
            if (attribute.get("name").equals(column)) {
                return i;
            }
        }
        return -1;
    }

    public static JSONObject readTableFormat(String databaseName, String tableName, PrintWriter out) {
        JSONParser parser = new JSONParser();
        try {
            String filePath = "src/test/java/databases/" + databaseName + ".json";
            FileReader reader = new FileReader(filePath);
            Object obj = parser.parse(reader);

            JSONArray databaseArray = (JSONArray) obj;

            for (Object databaseObj : databaseArray) {
                JSONObject database = (JSONObject) databaseObj;
                String dbName = (String) database.get("database_name");
                if (dbName.equals(databaseName)) {
                    JSONArray tables = (JSONArray) database.get("tables");
                    for (Object tableObj : tables) {
                        JSONObject table = (JSONObject) tableObj;
                        String tableNameInJson = (String) table.get("table_name");
                        if (tableNameInJson.equals(tableName)) {
                            return table;
                        }
                    }
                }
            }
            return null;
        } catch (IOException | ParseException e) {
            System.out.println("Error reading table format JSON file: " + e.getMessage());
            out.println("> Error reading table format JSON file: " + e.getMessage());
            return null;
        }
    }

    private static void updateDatabaseWithTable(String tableName, JSONObject tableObj, PrintWriter out) {
        JSONParser parser = new JSONParser();
        FileReader fileReader = null;
        FileWriter fileWriter = null;

        try {
            File databaseFile = new File("src/test/java/databases/" + currentDatabase + ".json");
            if (!databaseFile.exists()) {
                System.out.println("Database not found: " + currentDatabase);
                out.println("> Database " + "'" + currentDatabase + "'" + " does not exist. Make sure you entered the database name correctly.");
                return;
            }

            fileReader = new FileReader(databaseFile);
            Object obj = parser.parse(fileReader);
            JSONArray databaseJson = (JSONArray) obj;

            JSONObject databaseObj = (JSONObject) databaseJson.getFirst();
            JSONArray tablesArray;

            if (databaseObj.containsKey("tables")) {
                tablesArray = (JSONArray) databaseObj.get("tables");
            } else {
                tablesArray = new JSONArray();
                databaseObj.put("tables", tablesArray);
            }

            for (Object table : tablesArray) {
                JSONObject tableJson = (JSONObject) table;
                if (tableJson.get("table_name").equals(tableName)) {
                    System.out.println("Table '" + tableName + " already exists.");
                    out.println("> Table '" + tableName + " already exists.");
                    return;
                }
            }

            tablesArray.add(tableObj);

            fileWriter = new FileWriter(databaseFile);
            fileWriter.write(databaseJson.toJSONString() + "\n");

            System.out.println("Table '" + tableName + "' created.");
            out.println("> Table '" + tableName + "' created.");
        } catch (IOException | ParseException e) {
            e.printStackTrace();
        } finally {
            try {
                if (fileReader != null) {
                    fileReader.close();
                }
                if (fileWriter != null) {
                    fileWriter.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static synchronized void handleDatabaseOperation(String operation, String databaseName, PrintWriter out) {
        JSONParser parser = new JSONParser();
        Path databasePath = Paths.get("src/test/java/databases", databaseName + ".json");

        try {
            // Ellenőrizzük, hogy létezik-e a mappa, ha nem, akkor létrehozzuk
            Path databasesDir = Paths.get("src/test/java/databases");
            if (!Files.exists(databasesDir)) {
                Files.createDirectories(databasesDir);
            }

            // Ellenőrizzük, hogy létezik-e a JSON fájl, ha nem, akkor létrehozzuk
            if (!Files.exists(databasePath)) {
                Files.createFile(databasePath);
                try (BufferedWriter writer = Files.newBufferedWriter(databasePath)) {
                    JSONArray jsonArray = new JSONArray();
                    writer.write(jsonArray.toJSONString());
                }
            }

            // A JSON fájl tartalmának beolvasása
            try (BufferedReader reader = Files.newBufferedReader(databasePath)) {
                Object obj = parser.parse(reader);
                JSONArray databasesCurr = (JSONArray) obj;

                if (operation.equals("create")) {
                    for (Object dbObj : databasesCurr) {
                        JSONObject db = (JSONObject) dbObj;
                        if (db.get("database_name").equals(databaseName)) {
                            System.out.println("Database already exists: " + databaseName);
                            out.println("> Database '" + databaseName + "' already exists.");
                            return;
                        }
                    }

                    JSONObject newDB = new JSONObject();
                    newDB.put("database_name", databaseName);
                    databasesCurr.add(newDB);

                    // Frissítjük a JSON fájlt a megváltozott adatbázislistával
                    saveDatabaseJSON(databasesCurr, databasePath);

                    // Új adatbázis hozzáadása a memóriabeli listához
                    databases.put(databaseName, new Database(databaseName));

                    System.out.println("Database created: " + databaseName);
                    out.println("> Database '" + databaseName + "' created.");
                } else if (operation.equals("drop")) {
                    boolean found = false;
                    for (int i = 0; i < databasesCurr.size(); i++) {
                        JSONObject db = (JSONObject) databasesCurr.get(i);
                        if (db.get("database_name").equals(databaseName)) {
                            databasesCurr.remove(i);
                            found = true;
                            break;
                        }
                    }

                    if (!found) {
                        System.out.println("Database '" + databaseName + "' not found.");
                        out.println("> Database '" + databaseName + "' not found.");
                    } else {
                        // Fájlolvasó bezárása
                        reader.close();

                        currentDatabase = null;
                        try {
                            // Fájl törlése
                            Files.deleteIfExists(databasePath);
                            System.out.println("Database dropped: " + databaseName);
                            out.println("> Database '" + databaseName + "' dropped.");

                            // Adatbázis eltávolítása a memóriabeli listából
                            databases.remove(databaseName);
                        } catch (NoSuchFileException e) {
                            System.out.println("Failed to drop database: " + databaseName);
                            out.println("> Failed to drop database: " + databaseName);
                        } catch (SecurityException e) {
                            System.out.println("Security exception occurred: " + e.getMessage());
                            out.println("> Security exception occurred: " + e.getMessage());
                        }
                    }
                } else {
                    System.out.println("Invalid operation: " + operation);
                    out.println("> Invalid operation: " + operation);
                }
            }
        } catch (IOException | ParseException e) {
            e.printStackTrace();
        }
    }

    private static void saveDatabaseJSON(JSONArray databasesCurr, Path databasePath) {
        try (BufferedWriter writer = Files.newBufferedWriter(databasePath)) {
            writer.write(databasesCurr.toJSONString());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private Socket getClientSocket() {
        return this.clientSocket;
    }

}

