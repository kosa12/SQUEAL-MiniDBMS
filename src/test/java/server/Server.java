package server;

import data.Database;


import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Objects;

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

        //recreate();
        recreateFromJson("src/test/java/databases/databases.json");

        for (Database db : databases){
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
                    clientSocket.setSoTimeout(5000);
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


    public void recreate(){
        File folder = new File("src/test/java/databases");
        File[] listOfFolders = folder.listFiles();

        assert listOfFolders != null;
        for (File f : listOfFolders) {
            if (f.isDirectory()) {
                databases.add(new Database(f.getName()));
            }
        }
    }

    public void recreateFromJson(String jsonFilePath) {
        JSONParser parser = new JSONParser();

        try (FileReader reader = new FileReader(jsonFilePath)) {
            Object obj = parser.parse(reader);

            JSONArray databaseArray = (JSONArray) obj;

            // Iterate through each database object in the array
            for (Object databaseObj : databaseArray) {
                JSONObject databaseJson = (JSONObject) databaseObj;

                String databaseName = (String) databaseJson.get("name");

                databases.add(new Database(databaseName));
            }
        } catch (IOException | ParseException e) {
            e.printStackTrace();
        }
    }

    public static void drop(String dbName){
        Database tmp = null;
        for (Database db : databases){
            tmp = db;
            if(Objects.equals(db.getDataBaseName(), dbName)){
                databases.remove(db);
                break;
            }
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

                //message feldolgozasa

                String[] parts = message.split(" ");

                if (parts.length >= 3) {
                    String operation = parts[0].toLowerCase();
                    String objectType = parts[1].toLowerCase();
                    String objectName = parts[2];

                    if (Objects.equals(operation, "create") && Objects.equals(objectType, "database")) {
                        handleDatabaseOperation("create", objectName);
                    } else if (Objects.equals(operation, "drop") && Objects.equals(objectType, "database")) {
                        handleDatabaseOperation("drop", objectName);
                    }
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

        try (FileReader reader = new FileReader("src/test/java/databases/databases.json")) {
            Object obj = parser.parse(reader);

            if (obj instanceof JSONArray) {
                JSONArray databases = (JSONArray) obj;

                if (operation.equalsIgnoreCase("create")) {
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
                    saveDatabaseJSON(databases);

                    System.out.println("Database created: " + databaseName);
                } else if (operation.equalsIgnoreCase("drop")) {
                    boolean found = false;
                    for (int i = 0; i < databases.size(); i++) {
                        JSONObject db = (JSONObject) databases.get(i);
                        if (db.get("name").equals(databaseName)) {
                            databases.remove(i);
                            saveDatabaseJSON(databases);
                            System.out.println("Database dropped: " + databaseName);
                            found = true;
                            break;
                        }
                    }
                    if (!found) {
                        System.out.println("Database not found: " + databaseName);
                    }
                } else {
                    System.out.println("Invalid operation: " + operation);
                }
            } else {
                System.out.println("Invalid JSON structure: Expecting JSON array.");
            }
        } catch (IOException | ParseException e) {
            e.printStackTrace();
        }
    }

    private static void saveDatabaseJSON(JSONArray databases) {
        try (FileWriter file = new FileWriter("src/test/java/databases/databases.json")) {
            file.write(databases.toJSONString());
            file.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        Server server = new Server();
        server.run();
    }
}
