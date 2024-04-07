package server;

import com.mongodb.client.*;
import data.Attribute;
import data.Database;
import data.Table;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

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
                    Socket clientSocket = serverSocket.accept();
                    if (isRunning) {
                        System.out.println("Client connected: " + clientSocket.getInetAddress().getHostAddress());
                    }
                    new Thread(() -> handleClient(clientSocket)).start();
                    clientSocket.setSoTimeout(500000);
                } catch (SocketException se) {
                    if (!isRunning) {
                        System.out.println("Server is shutting down...");
                        break;
                    }
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
            if (table == null) {
                System.out.println("Table not found in database: " + collectionName);
                continue;
            }
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

    public void recreateFromJson(String jsonPath) {
        JSONParser parser = new JSONParser();
        FileReader reader = null;
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
                if (databaseFile.length() == 0) {
                    databaseFile.delete();
                    continue;
                }

                reader = new FileReader(databaseFile);

                try {
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

                                    if (attributeIsPK) {
                                        table.setpKAttrName(attributeName);
                                    }

                                    table.addAttribute(new Attribute(attributeName, attributeType, attributeNotNull, attributeIsPK));
                                }
                                database.addTable(table);

                            }
                        }
                        databases.put(databaseName, database);
                        recreateFromMongoDB(databaseName);
                    }
                } catch (IOException | ParseException e) {
                    throw new RuntimeException(e);
                } finally {
                    try {
                        reader.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public ServerSocket getServerSocket(){
        return serverSocket;
    }

    private static void handleClient(Socket clientSocket) {
        try (
                BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
        ) {
            StringBuilder commandBuilder = new StringBuilder();
            String line;
            while ((line = in.readLine()) != null) {
                if (line.trim().endsWith(";")) {
                    commandBuilder.append(line.trim(), 0, line.lastIndexOf(';'));
                    String command = commandBuilder.toString().trim();
                    if (command.trim().isEmpty()) {
                        return;
                    }

                    out.println("Received from client: " + command);

                    String[] parts = command.trim().split("\\s+");
                    if (parts.length == 2 && parts[0].equalsIgnoreCase("SHOW")) {
                        String objectType = parts[1].toUpperCase();
                        if (objectType.equals("DATABASES")) {
                            showDatabases();
                        } else if (objectType.equals("TABLES")) {
                            showTables();
                        } else {
                            System.out.println("Invalid SHOW command: " + command);
                        }
                    } else if (parts.length >= 4 && parts[0].equalsIgnoreCase("INSERT") && parts[1].equalsIgnoreCase("INTO")) {
                        insertRow(command);
                    } else if (parts.length >= 4 && parts[0].equalsIgnoreCase("DELETE") && parts[1].equalsIgnoreCase("FROM")) {
                        deleteRow(command);
                    }
                    else if (parts.length >= 3) {
                        String operation = parts[0].toLowerCase();
                        String objectType = parts[1].toLowerCase();
                        String objectName = parts[2];

                        if (operation.equals("create") || operation.equals("drop")) {
                            if (objectType.equals("database") || objectType.equals("db")) {
                                handleDatabaseOperation(operation, objectName);
                            } else if (objectType.equals("table") || objectType.equals("index")) {
                                handleTableOperation(operation, command);
                            } else {
                                out.println("Invalid object type: " + objectType);
                            }
                        }  else {
                            out.println("Invalid operation: " + operation);
                        }
                    } else if(parts.length == 2){
                        String operation = parts[0].toLowerCase();
                        String objectName = parts[1];
                        if (operation.equals("use")) {
                            handleUseDatabase(objectName);
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

            clientSocket.close();
            out.println("Client disconnected: " + clientSocket.getInetAddress().getHostAddress());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void showDatabases() {
        for (String dbName : databases.keySet()) {
            System.out.println(dbName);
        }
    }

    private static void showTables() {
        if (currentDatabase == null) {
            System.out.println("No database selected.");
            return;
        }

        Database db = databases.get(currentDatabase);
        if (db != null) {
            for (Table table : db.getTables()) {
                System.out.println(table.getTableName());
            }
        }
    }


    private static void handleUseDatabase(String databaseName) {
        Database db = databases.get(databaseName);
        if (db != null) {
            currentDatabase = databaseName;
            System.out.println("Switched to database: " + databaseName);
        } else {
            System.out.println("Database not found: " + databaseName);
        }
    }

    private static void handleTableOperation(String operation, String command) {
        if (currentDatabase == null) {
            System.out.println("No database selected.");
            return;
        }

        if (operation.equalsIgnoreCase("create")) {
            if (command.toLowerCase().contains("index")) {
                createIndex(command);
            } else {
                createTable(command);
            }
        } else if (operation.equalsIgnoreCase("drop")) {
            dropTable(command);}
        else {
            System.out.println("Invalid table operation: " + operation);
        }
    }

    private static void insertRow(String command) {
        if (!command.toLowerCase().contains("insert into")) {
            System.out.println("Invalid INSERT command format: Missing 'INSERT INTO' keyword.");
            return;
        }

        if (!command.toLowerCase().contains("values")) {
            System.out.println("Invalid INSERT command format: Missing 'VALUES' keyword.");
            return;
        }

        int startIndex = command.indexOf("(");
        int endIndex = command.lastIndexOf(")");
        if (startIndex == -1 || endIndex == -1) {
            System.out.println("Invalid INSERT command format: Missing '(' or ')' for values.");
            return;
        }

        String valuesPart = command.substring(startIndex + 1, endIndex);

        String[] values = valuesPart.split(",");

        String[] parts = command.trim().split("\\s+");
        String tableName = parts[2];
        Table table = databases.get(currentDatabase).getTable(tableName);
        if (table == null) {
            System.out.println("Table not found: " + tableName);
            return;
        }

        int index = 0;
        for (Attribute attribute : table.getAttributes()) {
            if (index >= values.length) {
                break;
            }
            String value = values[index].trim();
            String attributeType = attribute.getType();
            if (attributeType.toLowerCase().startsWith("varchar")) {
                int maxLength = extractMaxLengthFromType(attributeType);
                if (value.length() > maxLength) {
                    System.out.println("Value length exceeds maximum length for attribute " + attribute.getAttributeName());
                    return;
                }
            }
            index++;
        }

        if (values.length != table.getAttributes().size()) {
            System.out.println("Invalid number of values provided.");
            return;
        }

        StringBuilder concatenatedValue = new StringBuilder();
        String primaryKeyValue = "";
        for (int i = 0; i < values.length; i++) {
            String value = values[i].trim();
            Attribute attribute = table.getAttributes().get(i);
            String attributeName = attribute.getAttributeName();
            String attributeType = attribute.getType();
            if (attributeType.toLowerCase().contains("varchar")) {
                attributeType = "varchar";
            }


            if (convertValue(value, attributeType)==null) {
                System.out.println("Invalid value type for attribute: " + attributeName);
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
            System.out.println("Primary key value already exists: " + primaryKeyValue);
            return;
        }

        Document document = new Document();
        document.append("_id", primaryKeyValue);
        document.append("ertek", concatenatedValue.toString());

        String collectionName = tableName.toLowerCase();

        MongoDBHandler mongoDBHandler = new MongoDBHandler();
        mongoDBHandler.insertDocument(currentDatabase, collectionName, document);
        mongoDBHandler.close();

        System.out.println("Row inserted into MongoDB collection: " + collectionName);
    }

    private static void deleteRow(String command) {
        if (!command.toLowerCase().contains("delete from")) {
            System.out.println("Invalid DELETE command format: Missing 'DELETE FROM' keyword.");
            return;
        }

        if (!command.toLowerCase().contains("where")) {
            System.out.println("Invalid DELETE command format: Missing 'WHERE' keyword.");
            return;
        }

        String[] parts = command.trim().split("\\s+");
        String tableName = parts[2];
        String condition = parts[parts.length - 1];

        if (!condition.startsWith("_id")) {
            System.out.println("Invalid DELETE condition: Must be based on primary key (_id).");
            return;
        }

        String primaryKeyValue = condition.substring(4);

        MongoDBHandler mongoDBHandler = new MongoDBHandler();
        long deletedCount = mongoDBHandler.deleteDocumentByPK(currentDatabase, tableName, primaryKeyValue);
        mongoDBHandler.close();

        if (deletedCount > 0) {
            System.out.println("Deleted " + deletedCount + " document(s) from table: " + tableName);
        } else {
            System.out.println("No document found with primary key value: " + primaryKeyValue);
        }
    }


    private static int extractMaxLengthFromType(String type) {
        if (type.toLowerCase().startsWith("varchar")) {
            String maxLengthStr = type.substring(type.indexOf("(") + 1, type.indexOf(")"));
            return Integer.parseInt(maxLengthStr);
        }
        return -1;
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


    private static void createTable(String command) {
        String[] parts = command.split("\\s+");
        for (int i = 0; i < parts.length; i++) {
            parts[i] = parts[i].trim();
        }

        if (parts.length < 4 || !parts[0].equalsIgnoreCase("CREATE") || !parts[1].equalsIgnoreCase("TABLE")) {
            System.out.println("Invalid CREATE TABLE command format.");
            return;
        }

        String tableName = parts[2];

        if (!command.contains("(") || !command.contains(")")) {
            System.out.println("Invalid CREATE TABLE command format: Missing '(' or ')' for column definitions.");
            return;
        }

        String columnDefinitions = command.substring(command.indexOf("(") + 1, command.lastIndexOf(")"));

        String[] columns = columnDefinitions.split(",(?![^(]*\\))");

        if (columns.length == 0) {
            System.out.println("Invalid CREATE TABLE command format: No column definitions found.");
            return;
        }

        if (databases.get(currentDatabase).hasTable(tableName)) {
            System.out.println("Table already exists: " + tableName);
            return;
        }

        String primaryKeyAttributeName = null;
        for (String column : columns) {
            String[] columnParts = column.trim().split("\\s+");
            if (columnParts.length > 2 && columnParts[2].equalsIgnoreCase("PRIMARY")) {
                primaryKeyAttributeName = columnParts[0];
                break;
            }
        }

        if (primaryKeyAttributeName != null) {
            for (Table table : databases.get(currentDatabase).getTables()) {
                if (table.hasAttribute(primaryKeyAttributeName)) {
                    System.out.println("Primary key attribute already exists in another table: " + databases.get(currentDatabase).getTable(tableName).getTableName());
                    return;
                }
            }
        }

        JSONArray tableColumns = new JSONArray();
        boolean hasPrimaryKey = false;
        Table table = new Table(tableName, "", null);
        for (String column : columns) {
            String[] columnParts = column.trim().split("\\s+");
            if (columnParts.length < 2) {
                System.out.println("Invalid column definition: " + column);
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
                        System.out.println("Invalid length for varchar: " + length);
                        return;
                    }
                    goodSyntax = true;
                } catch (NumberFormatException e) {
                    System.out.println("Invalid length for varchar: " + lengthStr);
                    return;
                }
            } else {
                System.out.println("Invalid column type: " + columnParts[1]);
            }

            if (!goodSyntax) {
                return;
            }

            JSONObject columnObj = new JSONObject();
            columnObj.put("name", columnParts[0]);
            columnObj.put("type", columnParts[1]);

            boolean notNull = false;
            boolean isPK = false;

            for (int i = 2; i < columnParts.length; i++) {
                String keyword = columnParts[i].toUpperCase();
                if (keyword.equalsIgnoreCase("NOT")) {
                    if (i + 1 < columnParts.length && columnParts[i+1].equalsIgnoreCase("NULL")) {
                        notNull = true;
                        i++;
                    } else {
                        System.out.println("Invalid keyword after NOT: " + columnParts[i+1]);
                        return;
                    }
                } else if (keyword.equalsIgnoreCase("PRIMARY") && i + 1 < columnParts.length && columnParts[i+1].equalsIgnoreCase("KEY")) {
                    if (hasPrimaryKey) {
                        System.out.println("Only one primary key is allowed.");
                        return;
                    }
                    table.setpKAttrName(columnParts[0]);
                    isPK = true;
                    hasPrimaryKey = true;
                    table.addPKtoList(primaryKeyAttributeName);
                    i++;
                } else {
                    System.out.println("Invalid keyword in column definition: " + columnParts[i]);
                    return;
                }
            }

            columnObj.put("is_pk", isPK);
            columnObj.put("not_null", notNull);

            tableColumns.add(columnObj);
        }

        HashSet<String> columnNames = new HashSet<>();
        for (Object obj : tableColumns) {
            JSONObject column = (JSONObject) obj;
            String attributeName = (String) column.get("name");
            if (!columnNames.add(attributeName)) {
                System.out.println("Duplicate column name: " + attributeName);
                return;
            }
        }

        for (Object obj : tableColumns) {
            JSONObject column = (JSONObject) obj;
            String attributeName = (String) column.get("name");
            String attributeType = (String) column.get("type");
            boolean notNull = (boolean) column.get("not_null");
            boolean isPK = (boolean) column.get("is_pk");
            table.addAttribute(new Attribute(attributeName, attributeType, notNull, isPK));
        }

        databases.get(currentDatabase).addTable(table);
        JSONObject tableObj = new JSONObject();
        tableObj.put("table_name", tableName);
        tableObj.put("attributes", tableColumns);
        updateDatabaseWithTable(tableName, tableObj);
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

    private static void dropTable(String command) {
        String[] parts = command.split("\\s+");
        for (int i = 0; i < parts.length; i++) {
            parts[i] = parts[i].trim();
        }

        if (parts.length != 3 || !parts[0].equalsIgnoreCase("DROP") || !parts[1].equalsIgnoreCase("TABLE")) {
            System.out.println("Invalid DROP TABLE command format.");
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

    private static void createIndex(String command) {
        String[] parts = command.split("\\s+");
        if (parts.length < 5 || !parts[0].equalsIgnoreCase("CREATE") || !parts[1].equalsIgnoreCase("INDEX")) {
            System.out.println("Invalid CREATE INDEX command format.");
            return;
        }

        String indexName = parts[2];
        String tableName = parts[4];
        String[] columns = command.substring(command.indexOf("(") + 1, command.lastIndexOf(")")).split(",");

        for (int i = 0; i < columns.length; i++) {
            columns[i] = columns[i].trim();
        }

        Database currentDB = databases.get(currentDatabase);
        if (currentDB == null || !currentDB.hasTable(tableName)) {
            System.out.println("Table not found: " + tableName);
            return;
        }

        Table table = currentDB.getTable(tableName);

        for (String column : columns) {
            if (!table.hasAttribute(column)) {
                System.out.println("Column not found in table " + tableName + ": " + column);
                return;
            }
        }

        JSONObject indexObj = new JSONObject();
        indexObj.put("index_name", indexName);
        indexObj.put("table_name", tableName);
        JSONArray columnsArray = new JSONArray();
        columnsArray.addAll(Arrays.asList(columns));

        indexObj.put("columns", columnsArray);

        String fileName = currentDatabase + "-" + tableName + "-" + indexName + "-index.json";
        try (FileWriter fileWriter = new FileWriter("src/test/java/databases/" + fileName)) {
            fileWriter.write(indexObj.toJSONString());
            System.out.println("Index created: " + indexName + " on table " + tableName);
        } catch (IOException e) {
            System.out.println("Failed to create index file: " + e.getMessage());
        }
    }

    private static void updateDatabaseWithTable(String tableName, JSONObject tableObj) {
        JSONParser parser = new JSONParser();
        FileReader fileReader = null;
        FileWriter fileWriter = null;

        try {
            File databaseFile = new File("src/test/java/databases/" + currentDatabase + ".json");
            if (!databaseFile.exists()) {
                System.out.println("Database not found: " + currentDatabase);
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
                    System.out.println("Table already exists: " + tableName);
                    return;
                }
            }

            tablesArray.add(tableObj);

            fileWriter = new FileWriter(databaseFile);
            fileWriter.write(databaseJson.toJSONString() + "\n");

            System.out.println("Table created: " + tableName);
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

    public static synchronized void handleDatabaseOperation(String operation, String databaseName) {
        JSONParser parser = new JSONParser();
        FileReader fileReader = null;

        try {
            File databasesDir = new File("src/test/java/databases");
            if (!databasesDir.exists()) {
                databasesDir.mkdirs();
            }

            File databaseFile = new File(databasesDir, databaseName + ".json");
            if (!databaseFile.exists()) {
                try (FileWriter tempWriter = new FileWriter(databaseFile)) {
                    JSONArray jsonArray = new JSONArray();
                    tempWriter.write(jsonArray.toJSONString());
                }
            }

            fileReader = new FileReader(databaseFile);
            Object obj = parser.parse(fileReader);
            JSONArray databasesCurr = (JSONArray) obj;

            if (operation.equals("create")) {
                for (Object dbObj : databasesCurr) {
                    JSONObject db = (JSONObject) dbObj;
                    if (db.get("database_name").equals(databaseName)) {
                        System.out.println("Database already exists: " + databaseName);
                        return;
                    }
                }

                JSONObject newDB = new JSONObject();
                newDB.put("database_name", databaseName);
                databasesCurr.add(newDB);
                saveDatabaseJSON(databasesCurr, databaseFile);
                databases.put(databaseName, new Database(databaseName));

                System.out.println("Database created: " + databaseName);
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
                    System.out.println("Database not found: " + databaseName);
                } else {
                    //
                    //EZ ITT KELL LEGYEN !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
                    //
                    fileReader.close();
                    currentDatabase = null;
                    try {
                        if (databaseFile.delete()) {
                            System.out.println("Database dropped: " + databaseName);
                            databases.remove(databaseName);
                        } else {
                            System.out.println("Failed to drop database: " + databaseName);
                        }
                    } catch (SecurityException e) {
                        System.out.println("Security exception occurred: " + e.getMessage());
                    }
                }
            } else {
                System.out.println("Invalid operation: " + operation);
            }
        } catch (IOException | ParseException e) {
            e.printStackTrace();
        } finally {
            try {
                if (fileReader != null) {
                    fileReader.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    private static void saveDatabaseJSON(JSONArray databases, File databaseFile) {
        try (FileWriter writer = new FileWriter(databaseFile)) {
            writer.write(databases.toJSONString() + "\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}

