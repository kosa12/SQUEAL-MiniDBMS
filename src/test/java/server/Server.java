package server;

import data.Database;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class Server extends Thread {
    private ServerSocket serverSocket;
    private static final ArrayList<Database> databases = new ArrayList<>();

    private boolean isRunning = true;

    public void stopServer() {
        isRunning = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
            System.out.println("Server is shutting down...");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

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

                        String databaseName = (String) databaseJson.get("name");

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
                PrintWriter out = new PrintWriter(new OutputStreamWriter(clientSocket.getOutputStream()), true)
        ) {

            String message;
            while ((message = in.readLine()) != null) {
                System.out.println("Received from client: " + message);

                Pattern pattern = Pattern.compile("^\\s*(create|drop)\\s+(database)\\s+(\\S+)\\s*$", Pattern.CASE_INSENSITIVE);
                Matcher matcher = pattern.matcher(message);

                if (matcher.matches()) {
                    String operation = matcher.group(1).toLowerCase();
                    String objectType = matcher.group(2).toLowerCase();
                    String objectName = matcher.group(3);

                    handleDatabaseOperation(operation, objectName);
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


    public static void handleDatabaseOperation(String operation, String databaseName) {
        if (operation == null) {
            System.out.println("Operation cannot be null.");
            return;
        }

        JSONParser parser = new JSONParser();
        FileWriter writer = null;

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

            Object obj = parser.parse(new FileReader(databaseFile));
            JSONArray databases = (JSONArray) obj;

            if (operation.equals("create")) {
                for (Object dbObj : databases) {
                    JSONObject db = (JSONObject) dbObj;
                    if (db.get("name").equals(databaseName)) {
                        System.out.println("Database already exists: " + databaseName);
                        return;
                    }
                }

                JSONObject newDB = new JSONObject();
                newDB.put("name", databaseName);
                databases.add(newDB);
                writer = new FileWriter(databaseFile);
                writer.write(databases.toJSONString());

                System.out.println("Database created: " + databaseName);
            } else if (operation.equals("drop")) {
                boolean found = false;
                for (int i = 0; i < databases.size(); i++) {
                    JSONObject db = (JSONObject) databases.get(i);
                    if (db.get("name").equals(databaseName)) {
                        databases.remove(i);
                        writer = new FileWriter(databaseFile);
                        writer.write(databases.toJSONString());
                        System.out.println("Database dropped: " + databaseName);
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    System.out.println("Database not found: " + databaseName);
                }

                if (writer != null) {
                    writer.close();
                }

                if (!databaseFile.delete()) {
                    System.out.println("Failed to delete database file: " + databaseName);
                }
            } else {
                System.out.println("Invalid operation: " + operation);
            }
        } catch (IOException | ParseException e) {
            e.printStackTrace();
        }
    }





    private static void saveDatabaseJSON(JSONArray databases, File databaseFile) {
        try (FileWriter writer = new FileWriter(databaseFile)) {
            writer.write(databases.toJSONString());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
