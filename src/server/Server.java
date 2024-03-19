package server;

import data.Database;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Objects;
import java.io.File;

public class Server extends Thread {
    private ServerSocket serverSocket;
    private static final ArrayList<Database> databases = new ArrayList<>();
    @Override
    public void run() {

        recreate();

        for (Database db : databases){
            System.out.println(db.getDataBaseName());
        }


        try {
            serverSocket = new ServerSocket(12345);
            System.out.println("Server is running...");
            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Client connected: " + clientSocket.getInetAddress().getHostAddress());
                new Thread(() -> handleClient(clientSocket)).start();
                clientSocket.setSoTimeout(5000);
                //if(){ // ide kell valahogy megkapni a servergui stopServer-et
                 //   System.out.println("Server is shutting down...");
                //}

            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void recreate(){
        File folder = new File("src/databases");
        File[] listOfFolders = folder.listFiles();

        assert listOfFolders != null;
        for (File f : listOfFolders) {
            if (f.isDirectory()) {
                databases.add(new Database(f.getName()));
            }
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

                if (Objects.equals(parts[0].toLowerCase(), "create") && Objects.equals(parts[1].toLowerCase(), "database") && parts[2] != null){

                    File folder = new File("src/databases/" + parts[2]);

                    if (folder.mkdirs()) {
                        System.out.println("Database succesfully created: " + folder.getAbsolutePath());
                        Database newDB = new Database(parts[2]);
                        databases.add(newDB);

                    } else {
                        System.out.println("Could not create database <" + parts[2] +">.");
                    }
                }
                else if (Objects.equals(parts[0].toLowerCase(), "drop") && Objects.equals(parts[1].toLowerCase(), "database") && parts[2] != null){
                    File folder = new File("src/databases/" + parts[2]);

                    if(folder.delete()){
                        System.out.println("Database <" + parts[2] + "> dropped.");
                        drop(parts[2]);
                    }
                    else {
                        System.out.println("Could not drop database <" + parts[2] + ">." );
                    }
                }


                //

            }

            clientSocket.close();
            System.out.println("Client disconnected: " + clientSocket.getInetAddress().getHostAddress());
        } catch (IOException e) {
            //e.printStackTrace();
        }
    }
}
