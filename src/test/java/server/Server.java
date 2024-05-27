package server;

import com.mongodb.client.*;

import com.mongodb.client.model.Filters;
import data.Attribute;
import data.Database;
import data.Table;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import org.bson.conversions.Bson;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import org.bson.Document;

import javax.print.Doc;

public class Server extends Thread {
    private ServerSocket serverSocket;
    private static HashMap<String, Database> databases = new HashMap<>();
    private static String currentDatabase;
    private static MongoClient mongoClient;
    private static Boolean isItEnd = false;

    @Override
    public void run() {
        mongoClient = MongoClients.create("mongodb://localhost:27017");
        recreateFromJson("src/test/java/databases/");

        for (String dbName : databases.keySet()) {
            System.out.println(dbName);
        }

        ServerSocket serverSocket = null;
        try {
            serverSocket = new ServerSocket(10000);
            System.out.println("Server is running...");
            boolean isRunning = true;
            while (isRunning) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    System.out.println("Client connected: " + clientSocket.getInetAddress().getHostAddress());
                    new Thread(() -> handleClient(clientSocket)).start();
                    clientSocket.setSoTimeout(50000000);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (serverSocket != null && !serverSocket.isClosed()) {
                try {
                    serverSocket.close();
                    System.out.println("Server socket closed.");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
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
                        assert table != null;
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

            File[] databaseFiles = databasesDir.listFiles((_, name) -> name.endsWith(".json"));
            if (databaseFiles == null) {
                System.out.println("No database files found in directory: " + jsonPath);
                return;
            }

            for (File databaseFile : databaseFiles) {

                if (databaseFile.length() == 0) {
                    databaseFile.delete();
                    continue;
                }

                try (FileReader reader = new FileReader(databaseFile)) {

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
                if (!line.trim().startsWith("/*")) {
                    if (line.trim().endsWith(";")) {
                        isItEnd = false;

                        commandBuilder.append(line.trim(), 0, line.lastIndexOf(';'));
                        String command = commandBuilder.toString().trim();
                        if (command.trim().isEmpty()) {
                            return;
                        }

                        out.println(command);

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
                            insertRow(command, out);
                        } else if (parts.length >= 4 && parts[0].equalsIgnoreCase("DELETE") && parts[1].equalsIgnoreCase("FROM")) {
                            deleteRow(command, out);
                        } else if (parts.length >= 4 && parts[0].equalsIgnoreCase("SELECT")) {
                            selectFromTable(command, out);
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
                    } else if (line.equals("end")) {
                        isItEnd = true;
                        MongoDBHandler mongoDBHandler = new MongoDBHandler();
                        mongoDBHandler.insertDocumentsIntoMongoDBALL();
                        mongoDBHandler.close();
                    } else {
                        isItEnd = false;
                        commandBuilder.append(line);
                    }
                }

            }
            clientSocket.close();
            out.println("Client disconnected: " + clientSocket.getInetAddress().getHostAddress());
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static Boolean getIsItEnd() {
        return isItEnd;
    }

    private static void selectFromTable(String command, PrintWriter out) throws InterruptedException {
        String[] parts = command.split("\\s+");

        String tableName = extractTableName(parts);
        if (tableName == null) {
            out.println("> Invalid SELECT command: Missing table name or FROM keyword.");
            return;
        }

        if (currentDatabase == null) {
            out.println("> Failed to find database.\n Note: USE <database_name> if you forgot it.");
            return;
        }

        JSONObject tableFormat = readTableFormat(currentDatabase, tableName, out);
        if (tableFormat == null) {
            out.println("> Table format not found for table '" + tableName + "'");
            return;
        }

        if (parts[1].equals("*")) {
            if (command.toLowerCase().contains("where")) {
                // SELECT * FROM ETC WHERE LOREMIPSUM = lofasz;
                List<String> conditions = extractConditions(command);
                if (conditions != null) {
                    List<String> attributeNamesList = extractAttributeNamesALL(parts);
                    if (attributeNamesList.isEmpty()) {
                        out.println("> No attributes selected.");
                        return;
                    }
                    List<List<String>> rows = fetchRowsWithFilter(tableName, conditions, out, attributeNamesList);
                    if (rows != null && !rows.isEmpty()) {
                        PrintAttributeHeaderOut(out, tableFormat, attributeNamesList.toArray(new String[0]));
                        List<String[]> ertekek = new ArrayList<>();
                        for (List<String> row : rows) {
                            for(String rowXD : row){
                                String[] primaryKeys = new String[]{row.get(row.indexOf(rowXD))};
                                for (String primaryKey : primaryKeys) {
                                    Document document = fetchDocumentByPrimaryKey(tableName, primaryKey);
                                    if (document != null) {
                                        String ertek = (String) document.get("ertek");
                                        String pk = (String) document.get("_id");
                                        String vegsoErtek = pk + ";" + ertek;
                                        ertekek.add(vegsoErtek.split(";"));
                                    } else {
                                        out.println("> Document not found for primary key: " + primaryKey);
                                    }
                                }
                            }

                        }
                        printSelectedRows(out, tableFormat, ertekek, attributeNamesList.toArray(new String[0]));
                    }
                } else {
                    out.println("> Invalid WHERE clause.");
                }
            } else {
                // SELECT * FROM ETC;
                List<String[]> rows = fetchAllRowsFromTable(tableName, out);
                if (rows != null) {
                    printSelectedRowsALL(out, tableFormat, rows);
                }
            }
        } else {
            List<String> attributeNamesList = extractAttributeNames(parts);
            if (attributeNamesList.isEmpty()) {
                out.println("> No attributes selected.");
                return;
            }

            if (command.toLowerCase().contains("where")) {
                // SELECT A, B, C FROM ETC WHERE LOREMIPSUM = lofasz;
                List<String> conditions = extractConditions(command);
                if (conditions != null) {
                    List<List<String>> rows = fetchRowsWithFilter(tableName, conditions, out, attributeNamesList);
                    if (rows != null && !rows.isEmpty()) {
                        PrintAttributeHeaderOut(out, tableFormat, attributeNamesList.toArray(new String[0]));
                        List<String[]> ertekek = new ArrayList<>();
                        for (List<String> row : rows) {
                            for(String rowXD : row){
                                String[] primaryKeys = new String[]{row.get(row.indexOf(rowXD))};
                                for (String primaryKey : primaryKeys) {
                                    Document document = fetchDocumentByPrimaryKey(tableName, primaryKey);
                                    if (document != null) {
                                        String ertek = (String) document.get("ertek");
                                        String pk = (String) document.get("_id");
                                        String vegsoErtek = pk + ";" + ertek;
                                        ertekek.add(vegsoErtek.split(";"));
                                    } else {
                                        out.println("> Document not found for primary key: " + primaryKey);
                                    }
                                }
                            }

                        }
                        printSelectedRows(out, tableFormat, ertekek, attributeNamesList.toArray(new String[0]));
                    }
                } else {
                    out.println("> Invalid WHERE clause.");
                }
            } else {
                // SELECT A, B, C FROM ETC;
                List<String[]> rows = fetchAllRowsFromTable(tableName, out);
                if (rows != null) {
                    PrintAttributeHeaderOut(out, tableFormat, attributeNamesList.toArray(new String[0]));
                    printSelectedRows(out, tableFormat, rows, attributeNamesList.toArray(new String[0]));
                }
            }
        }
    }

    private static Document fetchDocumentByPrimaryKey(String tableName, String primaryKey) {
        MongoDatabase database = mongoClient.getDatabase(currentDatabase);
        MongoCollection<Document> collection = database.getCollection(tableName);
        Document document = collection.find(Filters.eq("_id", primaryKey)).first();

        return document;
    }

    private static String extractTableName(String[] parts) {
        for (int i = 0; i < parts.length; i++) {
            if (parts[i].equalsIgnoreCase("FROM") && i < parts.length - 1) {
                return parts[i + 1];
            }
        }
        return null;
    }

    private static List<String> extractAttributeNames(String[] parts) {
        List<String> attributeNamesList = new ArrayList<>();
        boolean foundSelectKeyword = false;

        for (String part : parts) {
            if (part.equalsIgnoreCase("SELECT")) {
                foundSelectKeyword = true;
                continue;
            } else if (part.equalsIgnoreCase("FROM")) {
                break;
            }

            if (foundSelectKeyword) {
                String[] attributeNamesArray = part.split(",");
                for (String attributeName : attributeNamesArray) {
                    attributeNamesList.add(attributeName.trim());
                }
            }
        }
        return attributeNamesList;
    }

    private static List<String> extractAttributeNamesALL(String[] parts) {
        List<String> attributeNamesList = new ArrayList<>();
        String path = "src/test/java/databases/" + currentDatabase + ".json";
        JSONParser parser = new JSONParser();
        try {
            Object obj = parser.parse(new FileReader(path));
            JSONArray databaseArray = (JSONArray) obj;

            for (Object databaseObj : databaseArray) {
                JSONObject databaseJson = (JSONObject) databaseObj;

                JSONArray tablesArray = (JSONArray) databaseJson.get("tables");
                if (tablesArray != null) {
                    for (Object tableObj : tablesArray) {
                        JSONObject tableJson = (JSONObject) tableObj;
                        String tableName = (String) tableJson.get("table_name");

                        if (tableName.equals(parts[3])) {
                            JSONArray attributesArray = (JSONArray) tableJson.get("attributes");
                            for (Object attributeObj : attributesArray) {
                                JSONObject attributeJson = (JSONObject) attributeObj;
                                String attributeName = (String) attributeJson.get("name");
                                attributeNamesList.add(attributeName);
                            }
                        }
                    }
                }
            }
        } catch (IOException | ParseException e) {
            e.printStackTrace();
        }
        return attributeNamesList;
    }

    private static List<String> extractConditions(String command) {
        List<String> conditions = new ArrayList<>();
        int whereIndex = command.toLowerCase().indexOf("where");
        if (whereIndex == -1) {
            return conditions;
        }

        String conditionString = command.substring(whereIndex + 5).trim();
        String[] conditionsArray = conditionString.split("\\s+and\\s+");

        for (String condition : conditionsArray) {
            conditions.add(condition.trim());
        }

        return conditions;
    }

    private static List<String[]> fetchAllRowsFromTable(String tableName, PrintWriter out) {
        MongoDBHandler mongoDBHandler = new MongoDBHandler();
        List<String[]> rows = mongoDBHandler.fetchAllRows(currentDatabase, tableName);
        mongoDBHandler.close();

        if (rows == null) {
            out.println("> Failed to fetch rows from table: " + tableName);
        } else if (rows.isEmpty()) {
            out.println("> Table: " + tableName + " is empty.");
        }
        return rows;
    }

    private static List<List<String>> fetchRowsWithFilter(String tableName, List<String> conditions, PrintWriter out, List<String> attributeNamesList) throws InterruptedException {
        ArrayList<Set<Document>> sets = new ArrayList<>(conditions.size());
        Set<Document> set;
        for (String condition : conditions) {
            set = new HashSet<>();
            String[] parts = condition.split("\\s+");
            if (parts.length != 3) {
                out.println("> Invalid condition: " + condition);
                return null;
            }
            String attributeName = parts[0];

            String indexName = constructIndexName(attributeName, tableName);
            MongoDBHandler mongoDBHandler = new MongoDBHandler();
            List<String> indexes = getRelevantCollections(tableName, mongoDBHandler);

            boolean isThereAnIndex = false;
            String matchedIndexName = "";

            String[] desiredParts = indexName.split("-");
            int desiredLength = desiredParts.length;
            String[] desiredAttributes = Arrays.copyOfRange(desiredParts, 0, desiredLength - 2);

            for (String index : indexes) {
                String[] indexParts = index.split("-");
                int length = indexParts.length;
                if (length >= 3 && indexParts[length - 1].equalsIgnoreCase("index") && indexParts[length - 2].equalsIgnoreCase(tableName)) {
                    String[] indexAttributes = Arrays.copyOfRange(indexParts, 1, length - 2);
                    if (Arrays.equals(indexAttributes, desiredAttributes)) {
                        isThereAnIndex = true;
                        matchedIndexName = index;
                        break;
                    }
                }
            }

            if (isThereAnIndex) {
                FindIterable<Document> result = MongoDBHandler.fetchRowsWithFilterFromIndex(currentDatabase, matchedIndexName, condition);
                if (result != null) {
                    for (Document doc : result) {
                        set.add(doc);
                    }
                }
            } else {
                String createIndexName = tableName + attributeName.toUpperCase();

                String command = "CREATE INDEX " + createIndexName + " ON " + tableName + " ( " + attributeName + " );";
                createIndex(command, out);
                StringBuilder indexNameBuilder = new StringBuilder(createIndexName + "-" + attributeName + "-" + tableName + "-index");
                MongoDBHandler mongoDBHandler1 = new MongoDBHandler();
                FindIterable<Document> result = mongoDBHandler1.fetchRowsWithFilterFromIndex(currentDatabase, indexNameBuilder.toString(), condition);
                if (result != null) {
                    for (Document doc : result) {
                        set.add(doc);
                    }
                }
            }
            sets.add(set);
        }

        set = new HashSet<>(sets.getFirst());
        List<List<String>> resultList = new ArrayList<>();
        for (int j = 1; j < sets.size(); j++) {
            resultList.add(intersect(set, sets.get(j)));
        }

        if(sets.size()==1){
            resultList.add(intersect(set, set));
        }

        return resultList;
    }

    public static List<String> intersect(Collection<Document> set1, Collection<Document> set2) {
        Collection<Document> smallerSet;
        Collection<Document> largerSet;

        if (set1.size() <= set2.size()) {
            smallerSet = set1;
            largerSet = set2;
        } else {
            smallerSet = set2;
            largerSet = set1;
        }
        Set<String> finalIntersection = new HashSet<>();

        for (Document doc1 : smallerSet) {
            List<String> ertekList1 = splitErtek(doc1.getString("ertek"));

            Set<String> tempIntersection = new HashSet<>();
            for (Document doc2 : largerSet) {
                List<String> ertekList2 = splitErtek(doc2.getString("ertek"));
                Set<String> currentIntersection = new HashSet<>(ertekList1);
                currentIntersection.retainAll(ertekList2);
                tempIntersection.addAll(currentIntersection);
            }
            finalIntersection.addAll(tempIntersection);
        }

        return new ArrayList<>(finalIntersection);
    }

    private static List<String> splitErtek(String ertek) {
        if (ertek == null || ertek.isEmpty()) {
            return Collections.emptyList();
        }
        return Arrays.asList(ertek.split(";"));
    }


    private static String constructIndexName(String attributeName, String tableName) {
        StringBuilder indexNameBuilder = new StringBuilder();
        indexNameBuilder.append(attributeName).append("-");
        indexNameBuilder.append(tableName).append("-index");
        return indexNameBuilder.toString();
    }


    private static void printSelectedRows(PrintWriter out, JSONObject tableFormat, List<String[]> rows, String[] selectedAttributes) {

        JSONArray attributes = (JSONArray) tableFormat.get("attributes");
        Map<String, Integer> attributeIndices = new HashMap<>();

        for (int i = 0; i < attributes.size(); i++) {
            JSONObject attribute = (JSONObject) attributes.get(i);
            attributeIndices.put(attribute.get("name").toString(), i);
        }

        List<String[]> filteredRows = new ArrayList<>();
        for (String[] row : rows) {
            String[] filteredRow = new String[selectedAttributes.length];
            for (int i = 0; i < selectedAttributes.length; i++) {
                int index = attributeIndices.getOrDefault(selectedAttributes[i], -1);
                if (index != -1 && index < row.length) {
                    filteredRow[i] = row[index];
                } else {
                    filteredRow[i] = "";
                }
            }
            filteredRows.add(filteredRow);
        }

        StringBuilder delimiter = new StringBuilder();
        delimiter.append("-".repeat(Math.max(0, selectedAttributes.length * 40)));
        out.println(delimiter);
        for (String[] row : filteredRows) {
            out.println("|\t" + String.join("\t|\t", row) + "\t|");
        }
        out.println(delimiter);
    }

    private static void PrintAttributeHeaderOut(PrintWriter out, JSONObject tableFormat, String[] selectedAttributes) {
        out.println();
        for (String attribute : selectedAttributes) {
            out.print("\t  " + attribute + "\t  ");
        }
        out.println();
    }


    private static void printSelectedRowsALL(PrintWriter out, JSONObject tableFormat, List<String[]> rows) {
        out.println();
        JSONArray attributes = (JSONArray) tableFormat.get("attributes");
        for (Object attributeObj : attributes) {
            JSONObject attribute = (JSONObject) attributeObj;
            out.print("\t  " + attribute.get("name") + "\t  ");
        }

        out.println();

        StringBuilder delimiter = new StringBuilder();
        delimiter.append("-".repeat(Math.max(0, attributes.size() * 40)));
        out.println(delimiter);
        for (String[] row : rows) {
            out.println("|\t" + String.join("\t|\t", row) + "\t|");
        }
        out.println(delimiter);
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
            out.println("DBFORQUERY " + databaseName);
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

    private static void insertRow(String command, PrintWriter out) {
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

        if (currentDatabase == null) {
            out.println("> No database found.");
            out.println("NOTE: 'USE <database_name>;'");
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
                value = value.replaceAll("^'|'$", "");
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
        updateIndexes(tableName, values, out, primaryKeyValue);

        out.println("> Row inserted/updated into MongoDB collection: " + collectionName);
    }

    private static void updateIndexes(String tableName, String[] values, PrintWriter out, String primaryKeyValue) {
        MongoDBHandler mongoDBHandler = new MongoDBHandler();
        List<String> relevantCollections = getRelevantCollections(tableName, mongoDBHandler);

        List<List<String>> attributeNamesList = new ArrayList<>();

        for (String collectionName : relevantCollections) {
            String[] parts = collectionName.split("-");
            if (parts.length > 2 && parts[parts.length - 2].equalsIgnoreCase(tableName)) {
                List<String> attributes = new ArrayList<>();
                for (int i = 1; i < parts.length - 2; i++) {
                    attributes.add(parts[i]);
                }
                attributeNamesList.add(attributes);
            }
        }


        String[][] attributeNames = new String[attributeNamesList.size()][];
        for (int i = 0; i < attributeNamesList.size(); i++) {
            List<String> sublist = attributeNamesList.get(i);
            attributeNames[i] = sublist.toArray(new String[0]);
        }
        int nrIndexes = 0;
        for (String collectionName : relevantCollections) {
            List<Document> documents = mongoDBHandler.fetchDocuments(currentDatabase, collectionName);
            for (Document doc : documents) {
                JSONObject tableFormat = readTableFormat(currentDatabase, tableName, out);
                if (tableFormat == null) {
                    System.out.println("Table format not found for table '" + tableName + "'");
                    out.println("> Table format not found for table '" + tableName + "'");
                    return;
                }

                List<Integer> indexKeys = getIndexKeys(tableFormat, attributeNames[nrIndexes]);

                if (indexKeys.contains(-1)) {
                    System.out.println("One or more columns not found in table format for table " + tableName);
                    out.println("> One or more columns not found in table format for table '" + tableName + "'");
                    return;
                }

                StringBuilder valueBuilder = new StringBuilder();
                for (int i = 0; i < indexKeys.size(); i++) {
                    int indexKey = indexKeys.get(i);
                    String value = values[indexKey].trim();
                    if (!value.isEmpty()) {
                        value = value.replaceAll("^'|'$", "");
                        valueBuilder.append(value);
                        if (i < indexKeys.size() - 1) {
                            valueBuilder.append(";");
                        }
                    }
                }

                String valueAtIndex = valueBuilder.toString().trim();

                String docId = doc.get("_id").toString().trim();
                String existingErtek = doc.getString("ertek");
                String concatValue = existingErtek + ";" + primaryKeyValue;

                Document existingDocument = mongoDBHandler.getDocumentByIndex(currentDatabase, collectionName, "_id", concatValue);

                if (docId.equals(valueAtIndex)) {
                    doc.put("ertek", concatValue);
                    mongoDBHandler.updateDocument(currentDatabase, collectionName, "_id", docId, "ertek", concatValue);
                } else if (existingDocument == null) {
                    Document document = new Document();
                    document.append("_id", valueAtIndex);
                    document.append("ertek", primaryKeyValue);
                    mongoDBHandler.insertDocumentINDEX(currentDatabase, collectionName, document);
                }

            }
            nrIndexes++;
        }
        mongoDBHandler.close();
    }

    private static List<String> getRelevantCollections(String tableName, MongoDBHandler mongoDBHandler) {
        List<String> relevantCollections = new ArrayList<>();
        String indexSuffix = "-" + tableName + "-index";
        for (String collection : mongoDBHandler.getAllCollections(currentDatabase)) {
            if (collection.endsWith(indexSuffix)) {
                relevantCollections.add(collection);
            }
        }
        return relevantCollections;
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

        if (isRowReferenced(tableName, out)) {
            return;
        }

        MongoDBHandler mongoDBHandler = new MongoDBHandler();
        long deletedCount = mongoDBHandler.deleteDocumentByPK(currentDatabase, tableName, primaryKeyValue);
        List<String> relevantCollections = getRelevantCollections(tableName, mongoDBHandler);


        Table table = databases.get(currentDatabase).getTable(tableName);
        if (table != null) {

            if (deletedCount > 0) {
                out.println("> Deleted " + deletedCount + " document(s) from table: " + tableName);
                for (String collectionName : relevantCollections) {
                    List<Document> documents = mongoDBHandler.fetchDocuments(currentDatabase, collectionName);

                    for (Document doc : documents) {
                        String ertek = doc.getString("ertek");
                        if (ertek != null && ertek.contains(primaryKeyValue)) {
                            String updatedErtek = ertek.replace(primaryKeyValue, "");
                            if (updatedErtek.contains(";;")) {
                                updatedErtek = updatedErtek.replace(";;", ";");
                            } else if (updatedErtek.startsWith(";")) {
                                updatedErtek = updatedErtek.replaceFirst(";", "");
                            }
                            doc.put("ertek", updatedErtek);
                            mongoDBHandler.updateDocument(currentDatabase, collectionName, "_id", doc.get("_id").toString(), "ertek", updatedErtek);
                            out.println("> Deleted document(s) from indexes too: " + collectionName);
                        }
                    }


                }

            } else {
                out.println("> No document found with primary key value: " + primaryKeyValue);
            }

        } else {
            out.println("> Table '" + tableName + "' does not exist.\nNote: you can see the existing tables using command 'show tables'");
        }
        mongoDBHandler.close();
    }

    private static boolean isRowReferenced(String tableName, PrintWriter out) {
        String path = "src/test/java/databases/" + currentDatabase + ".json";
        JSONArray currentDatabaseJson = getCurrentDatabaseJson(path);
        if (currentDatabaseJson == null) {
            out.println("> Error: Unable to load current database JSON.");
            return true;
        }

        for (Object databaseObj : currentDatabaseJson) {
            JSONObject databaseJson = (JSONObject) databaseObj;
            JSONArray tablesArray = (JSONArray) databaseJson.get("tables");

            if (tablesArray != null) {
                for (Object tableObj : tablesArray) {
                    JSONObject tableJson = (JSONObject) tableObj;
                    if (tableJson.get("table_name").equals(tableName)) {
                        JSONArray attributesArray = (JSONArray) tableJson.get("attributes");

                        if (attributesArray != null) {
                            for (Object attributeObj : attributesArray) {
                                JSONObject attributeJson = (JSONObject) attributeObj;

                                if (attributeJson.containsKey("is_referenced_by_fk")) {
                                    JSONArray referencedByFkArray = (JSONArray) attributeJson.get("is_referenced_by_fk");
                                    if (!referencedByFkArray.isEmpty()) {
                                        out.println("> Error: Cannot delete row because it is referenced by another table.");
                                        return true;
                                    }
                                }
                            }
                        }
                        break;
                    }
                }
            }
        }
        return false;
    }


    private static Object convertValue(String value, String type) {
        return switch (type.toLowerCase()) {
            case "int" -> Integer.parseInt(value);
            case "float" -> Float.parseFloat(value);
            case "bit" -> Boolean.parseBoolean(value);
            case "date", "datetime", "varchar" -> value;
            default -> null;
        };
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

                    i += 5;
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

                JSONArray tablesArray = (JSONArray) ((JSONObject) currentDatabaseJson.getFirst()).get("tables");

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
            JSONArray fkKeys = (JSONArray) column.get("is_referenced_by_fk");
            table.addAttribute(new Attribute(attributeName, attributeType, notNull, isPK, isfk, fkKeys));
        }

        databases.get(currentDatabase).addTable(table);


        JSONObject tableObj = new JSONObject();
        tableObj.put("table_name", tableName);

        JSONArray indexes = new JSONArray();
        tableObj.put("indexes", indexes);

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
            if (obj instanceof JSONArray jsonArray) {
                if (!jsonArray.isEmpty()) {
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
            JSONArray updatedFKJson = new JSONArray();

            JSONObject databaseObj = (JSONObject) databaseJson.getFirst();
            JSONArray tablesArray = (JSONArray) databaseObj.get("tables");
            boolean tableFound = false;
            int tableIndex = -1;
            for (int i = 0; i < tablesArray.size(); i++) {
                JSONObject tableObj = (JSONObject) tablesArray.get(i);
                if (tableObj.get("table_name").equals(tableName)) {
                    tableFound = true;
                    tableIndex = i;
                    JSONArray attributesArray = (JSONArray) tableObj.get("attributes");
                    for (Object attributeObj : attributesArray) {
                        JSONObject attribute = (JSONObject) attributeObj;
                        if (attribute.containsKey("is_fk") && (boolean) attribute.get("is_fk")) {
                            updatedFKJson = removeReferencesFromOtherTables(tableName, (String) attribute.get("name"), out);
                        }
                        if (attribute.containsKey("is_referenced_by_fk")) {
                            JSONArray referencedArray = (JSONArray) attribute.get("is_referenced_by_fk");
                            if (!referencedArray.isEmpty()) {
                                System.out.println("Error: The attribute '" + attribute.get("name") + "' in table '" + tableName + "' has references in other tables.");
                                out.println("> Error: The attribute '" + attribute.get("name") + "' in table '" + tableName + "' has references in other tables.");
                                return;
                            }
                        }
                    }
                    break;
                }
            }

            if (tableFound) {
                try {
                    fileReader.close();
                    fileWriter = new FileWriter(databaseFile);

                    if (!updatedFKJson.isEmpty()) {
                        databaseJson = updatedFKJson;
                        databaseObj = (JSONObject) databaseJson.getFirst();
                        tablesArray = (JSONArray) databaseObj.get("tables");
                    }


                    tablesArray.remove(tableIndex);

                    fileWriter.write(databaseJson.toJSONString() + "\n");
                } catch (IOException e) {
                    e.printStackTrace();
                }

                MongoDatabase mongoDatabase = mongoClient.getDatabase(currentDatabase);
                mongoDatabase.getCollection(tableName).drop();

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

    private static JSONArray removeReferencesFromOtherTables(String tableName, String attributeName, PrintWriter out) {
        JSONParser parser = new JSONParser();

        String path = "src/test/java/databases/" + currentDatabase + ".json";
        JSONArray currentDatabaseJson = getCurrentDatabaseJson(path);
        if (currentDatabaseJson == null) {
            out.println("> Error: Unable to load current database JSON.");
            return null;
        }

        for (Object databaseObj : currentDatabaseJson) {
            JSONObject databaseJson = (JSONObject) databaseObj;
            JSONArray tablesArray = (JSONArray) databaseJson.get("tables");

            if (tablesArray != null) {
                for (Object tableObj : tablesArray) {
                    JSONObject tableJson = (JSONObject) tableObj;
                    JSONArray attributesArray = (JSONArray) tableJson.get("attributes");

                    if (attributesArray != null) {
                        for (Object attributeObj : attributesArray) {
                            JSONObject attributeJson = (JSONObject) attributeObj;

                            if (attributeJson.containsKey("is_fk") && (boolean) attributeJson.get("is_fk")) {
                                JSONArray tablesArray2 = (JSONArray) databaseJson.get("tables");

                                for (Object tableObj2 : tablesArray2) {
                                    JSONObject tableJson2 = (JSONObject) tableObj2;
                                    JSONArray attributesArray2 = (JSONArray) tableJson2.get("attributes");

                                    if (attributesArray2 != null) {
                                        for (Object attributeObj2 : attributesArray2) {
                                            JSONObject attributeJson2 = (JSONObject) attributeObj2;

                                            if (attributeJson2.containsKey("is_referenced_by_fk")) {
                                                JSONArray referencedByFkArray = (JSONArray) attributeJson2.get("is_referenced_by_fk");
                                                if (referencedByFkArray != null) {
                                                    for (int i = 0; i < referencedByFkArray.size(); i++) {
                                                        JSONObject referenceJson = (JSONObject) referencedByFkArray.get(i);
                                                        String fkTableName = (String) referenceJson.get("fkTableName");
                                                        String fkAttributeName = (String) referenceJson.get("fkName");

                                                        if (fkTableName.equals(tableName) && fkAttributeName.equals(attributeName)) {
                                                            referencedByFkArray.remove(i);
                                                            i--;
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        return currentDatabaseJson;
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

            String[] trimmedColumns = new String[columns.length];
            for (int i = 0; i < columns.length; i++) {
                trimmedColumns[i] = columns[i].trim();
            }
            String indexName1 = indexName + "-" + String.join("-", trimmedColumns) + "-" + tableName + "-index";
            if (mongoDBHandler.indexExists(currentDatabase, indexName1)) {
                System.out.println("Index name '" + indexName + "-" + String.join("-", columns) + "-" + tableName + "-index' already exists in MongoDB");
                out.println("> Index name '" + indexName + "-" + String.join("-", columns) + "-" + tableName + "-index' already exists in MongoDB");
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
                String everything = primaryKeyValue + ";" + valuesFromRecord;
                String[] values = everything.split(";");

                StringBuilder indexKeyBuilder = new StringBuilder();
                for (int i = 0; i < indexKeys.size(); i++) {
                    indexKeyBuilder.append(values[indexKeys.get(i)]);
                    if (i < indexKeys.size() - 1) {
                        indexKeyBuilder.append(";");
                    }
                }

                String compositeIndexKey = indexKeyBuilder.toString();

                Document existingDocument = mongoDBHandler.getDocumentByIndex(currentDatabase, indexName1, "_id", compositeIndexKey);

                if (existingDocument != null) {
                    String existingPrimaryKey = existingDocument.getString("ertek");
                    existingPrimaryKey += ";" + primaryKeyValue;
                    existingDocument.put("ertek", existingPrimaryKey);
                    mongoDBHandler.updateDocument(currentDatabase, indexName1, "_id", compositeIndexKey, "ertek", existingPrimaryKey);
                } else {
                    Document document = new Document();
                    document.append("_id", compositeIndexKey);
                    document.append("ertek", primaryKeyValue);
                    mongoDBHandler.insertDocumentINDEX(currentDatabase, indexName1, document);
                }
            }

            updateIndexInJson(indexName, tableName, trimmedColumns, out);

            System.out.println("Index created: " + indexName + " on columns " + columnsStr + " in table " + tableName);
            out.println("> Index '" + indexName + "' created on column(s) " + columnsStr + " in table '" + tableName + "'");
        } catch (Exception ex) {
            System.out.println("Failed to create index in MongoDB: " + ex.getMessage());
            out.println("> Failed to create index in MongoDB: " + ex.getMessage());
        } finally {
            mongoDBHandler.close();
        }


    }

    private static void updateIndexInJson(String indexName, String tableName, String[] columns, PrintWriter out) {
        String path = "src/test/java/databases/" + currentDatabase + ".json";
        JSONArray currentDatabaseJson = getCurrentDatabaseJson(path);
        if (currentDatabaseJson == null) {
            out.println("> Error: Unable to load current database JSON.");
            return;
        }

        JSONArray tablesArray = (JSONArray) ((JSONObject) currentDatabaseJson.getFirst()).get("tables");

        JSONObject targetTable = null;
        for (Object tableObj : tablesArray) {
            JSONObject tableJson = (JSONObject) tableObj;
            if (tableJson.get("table_name").equals(tableName)) {
                targetTable = tableJson;
                break;
            }
        }

        if (targetTable == null) {
            out.println("> Error: Table '" + tableName + "' not found in database JSON.");
            return;
        }

        JSONArray indexesArray = (JSONArray) targetTable.get("indexes");
        JSONObject newIndex = new JSONObject();
        String mongoDBname = indexName + "-" + String.join("-", columns) + "-" + tableName + "-index";
        newIndex.put("index_name_in_mongoDB", mongoDBname);
        newIndex.put("index_name", indexName);
        newIndex.put("attributes", String.join(", ", columns));
        indexesArray.add(newIndex);

        FileWriter writer = null;
        try {
            writer = new FileWriter(path);
            writer.write(currentDatabaseJson.toJSONString());
        } catch (IOException e) {
            out.println("> Error: Unable to write to current database JSON file.");
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

                    mongoClient.getDatabase(databaseName);
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

                            MongoDBHandler mongoDBHandler = new MongoDBHandler();
                            mongoDBHandler.dropCollection(databaseName);
                            mongoDBHandler.close();

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

}


