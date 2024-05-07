package client;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import javax.swing.*;

public class Client {
    Socket socket;
    private static JTextArea outputTextArea;
    private BufferedReader serverInput;
    private volatile boolean running;

    public Client(String serverAddress, int serverPort) {
        try {
            socket = new Socket(serverAddress, serverPort);
            serverInput = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            outputTextArea = new JTextArea();
            running = true;

            Thread responseListener = new Thread(this::listenForServerResponses);
            responseListener.start();
        } catch (IOException ex) {
            System.out.println("The client is not connected to the server. Please start the server.");
        }
    }

    public void sendMessage(String message) {
        try {
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            out.println(message);
        } catch (IOException ex) {
            System.out.println("Error sending message: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    public void close() {
        try {
            running = false;
            if (socket != null) {
                socket.close();
            }
        } catch (IOException ex) {
            System.out.println("Error closing socket: " + ex.getMessage());
        }
    }

    private void listenForServerResponses() {
        try {
            String response;
            while (running && (response = serverInput.readLine()) != null) {
                if (outputTextArea != null) {
                    String finalResponse = response;
                    SwingUtilities.invokeLater(() -> outputTextArea.append(finalResponse + "\n"));
                }
            }
        } catch (IOException ex) {
            if (running) {
                System.out.println("Error receiving message: " + ex.getMessage());
            }
        } finally {
            try {
                if (serverInput != null) {
                    serverInput.close();
                }
            } catch (IOException ex) {
                System.out.println("Error closing input stream: " + ex.getMessage());
            }
        }
    }

    public void setOutputTextArea(JTextArea outputTextArea) {
        this.outputTextArea = outputTextArea;
    }
}
