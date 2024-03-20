package server;

import data.Database;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;

import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;



public class Server extends Thread {
    private ServerSocket serverSocket;
    private static final ArrayList<Database> databases = new ArrayList<>();

    private static String currentDatabase;

    private boolean isRunning = true;

    @Override
    public void run() {

        recreateFromJson("src/test/java/databases");

        for (Database db : databases) {
            System.out.println(db.getDataBaseName());
        }

        try {
            serverSocket = new ServerSocket(12345);
            System.out.println("Server is running...");
            while (isRunning) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    System.out.println("Client connected: " + clientSocket.getInetAddress().getHostAddress());
                    new Thread(() -> handleClient(clientSocket)).start();
                    clientSocket.setSoTimeout(50000);
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

    public void recreateFromJson(String jsonPath) {
        JSONParser parser = new JSONParser();

        try {
            File databasesDir = new File(jsonPath);
            if (!databasesDir.exists() || !databasesDir.isDirectory()) {
                System.out.println("Invalid directory: " + jsonPath);
                return;
            }

            File[] databaseFiles = databasesDir.listFiles((dir, name) -> name.endsWith(".json"));
            if (databaseFiles == null) {
                System.out.println("No database files found in directory: " + jsonPath);
                return;
            }

            for (File databaseFile : databaseFiles) {
                try (FileReader reader = new FileReader(databaseFile)) {
                    Object obj = parser.parse(reader);
                    JSONArray databaseArray = (JSONArray) obj;

                    for (Object databaseObj : databaseArray) {
                        JSONObject databaseJson = (JSONObject) databaseObj;

                        String databaseName = (String) databaseJson.get("db_name");

                        databases.add(new Database(databaseName));
                    }
                }
            }
        } catch (IOException | ParseException e) {
            e.printStackTrace();
        }
    }

    public ServerSocket getServerSocket(){
        return serverSocket;
    }

    private static void handleClient(Socket clientSocket) {
        try (
                BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        ) {
            String message;
            while ((message = in.readLine()) != null) {
                System.out.println("Received from client: " + message);

                String[] parts = message.trim().split("\\s+"); // Split the message by whitespace

                if (parts.length >= 3) {
                    String operation = parts[0].toLowerCase();
                    String objectType = parts[1].toLowerCase();
                    String objectName = parts[2];

                    if (operation.equals("create") || operation.equals("drop")) {
                        if (objectType.equals("database")) {
                            handleDatabaseOperation(operation, objectName, in);
                        } else if (objectType.equals("table")) {
                            handleTableOperation(operation, message, in);
                        } else {
                            System.out.println("Invalid object type: " + objectType);
                        }
                    } else if (operation.equals("use") && objectType.equals("database")) {
                        handleUseDatabase(objectName);
                    } else {
                        System.out.println("Invalid operation: " + operation);
                    }
                } else {
                    System.out.println("Invalid message format: " + message);
                }
            }

            clientSocket.close();
            System.out.println("Client disconnected: " + clientSocket.getInetAddress().getHostAddress());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private static void handleUseDatabase(String databaseName) {
        boolean found = false;
        for (Database db : databases) {
            if (db.getDataBaseName().equals(databaseName)) {
                found = true;
                break;
            }
        }

        if (found) {
            currentDatabase = databaseName;
            System.out.println("Switched to database: " + databaseName);

        } else {
            System.out.println("Database not found: " + databaseName);
        }
    }


    public static void handleDatabaseOperation(String operation, String objectType, String objectName, BufferedReader in) {
        if (operation == null || objectType == null || objectName == null) {
            System.out.println("Operation, object type, or object name cannot be null.");
            return;
        }

        if (objectType.equalsIgnoreCase("database")) {
            handleDatabaseOperation(operation, objectName, in);
        } else if (objectType.equalsIgnoreCase("table")) {
            handleTableOperation(operation, objectName, in);
        } else {
            System.out.println("Invalid object type: " + objectType);
        }
    }

    private static void handleTableOperation(String operation, String command, BufferedReader in) {
        if (currentDatabase == null) {
            System.out.println("No database selected.");
            return;
        }

        if (!operation.equalsIgnoreCase("create")) {
            System.out.println("Invalid table operation: " + operation);
            return;
        }


        String[] parts = command.split("\\s+");
        for (int i = 0; i < parts.length; i++) {
            parts[i] = parts[i].trim();
        }

        System.out.println(Arrays.toString(parts));
        if (parts.length < 4 || !parts[0].equalsIgnoreCase("CREATE") || !parts[1].equalsIgnoreCase("TABLE")) {
            System.out.println("Invalid CREATE TABLE command format.");
            return;
        }

        String tableName = parts[2];

        String columnDefinitions = command.substring(command.indexOf("(") + 1, command.lastIndexOf(")"));

        String[] columns = columnDefinitions.split(",");
        JSONArray tableColumns = new JSONArray();
        for (String column : columns) {
            String[] columnParts = column.trim().split("\\s+");
            if (columnParts.length != 2) {
                System.out.println("Invalid column definition: " + column);
                return;
            }
            JSONObject columnObj = new JSONObject();
            columnObj.put("name", columnParts[0]);
            columnObj.put("type", columnParts[1]);
            tableColumns.add(columnObj);
        }

        // Create table object
        JSONObject tableObj = new JSONObject();
        tableObj.put("name", tableName);
        tableObj.put("columns", tableColumns);

        updateDatabaseWithTable(tableName, tableObj);
        System.out.println("Table created: " + tableName);
    }


    private static void updateDatabaseWithTable(String tableName, JSONObject tableObj) {
        JSONParser parser = new JSONParser();
        FileReader fileReader = null;
        FileWriter fileWriter = null;

        JSONArray tablesArray = null;

        try {
            File databaseFile = new File("src/test/java/databases/" + currentDatabase + ".json");
            if (!databaseFile.exists()) {
                System.out.println("Database not found: " + currentDatabase);
                return;
            }

            fileReader = new FileReader(databaseFile);
            Object obj = parser.parse(fileReader);
            JSONArray databaseJson = (JSONArray) obj;


            JSONObject databaseObj = (JSONObject) databaseJson.get(0);
            if (databaseObj.containsKey("tables")) {
                tablesArray = (JSONArray) databaseObj.get("tables");
            } else {
                tablesArray = new JSONArray();
                databaseObj.put("tables", tablesArray);
            }

            for (Object table : tablesArray) {
                JSONObject tableJson = (JSONObject) table;
                if (tableJson.get("name").equals(tableName)) {
                    System.out.println("Table already exists: " + tableName);
                    return;
                }
            }

            tablesArray.add(tableObj);

            fileWriter = new FileWriter(databaseFile);
            fileWriter.write(databaseJson.toJSONString()+ "\n");
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


    public static void handleDatabaseOperation(String operation, String databaseName, BufferedReader in) {
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
                    tempWriter.write(jsonArray.toJSONString() + "\n");
                }
            }

            fileReader = new FileReader(databaseFile);
            Object obj = parser.parse(fileReader);
            JSONArray databases = (JSONArray) obj;

            if (operation.equals("create")) {
                for (Object dbObj : databases) {
                    JSONObject db = (JSONObject) dbObj;
                    if (db.get("db_name").equals(databaseName)) {
                        System.out.println("Database already exists: " + databaseName);
                        return;
                    }
                }

                JSONObject newDB = new JSONObject();
                newDB.put("db_name", databaseName);
                databases.add(newDB);
                saveDatabaseJSON(databases, databaseFile);

                System.out.println("Database created: " + databaseName);
            } else if (operation.equals("drop")) {
                boolean found = false;
                for (int i = 0; i < databases.size(); i++) {
                    JSONObject db = (JSONObject) databases.get(i);
                    if (db.get("db_name").equals(databaseName)) {
                        databases.remove(i);
                        found = true;
                        break;
                    }
                }

                if (!found) {
                    System.out.println("Database not found: " + databaseName);
                } else {
                    in.close();
                    fileReader.close();

                    try {
                        if (databaseFile.delete()) {
                            System.out.println("Database dropped: " + databaseName);
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
