package client;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import javax.swing.JMenuItem;

public class Client {
    private Socket socket;

    public Client(String serverAddress, int serverPort) {
        try {
            socket = new Socket(serverAddress, serverPort);
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
        }
    }

    public void close() {
        try {
            socket.close();
        } catch (IOException ex) {
            System.out.println("Error closing socket: " + ex.getMessage());
        }
    }

    public static void main(String[] args) {
        String serverAddress = "localhost";
        int serverPort = 10000;

        Client client = new Client(serverAddress, serverPort);
        if (client.socket == null) {
            return;
        }

        Client_GUI gui = new Client_GUI(client);

        JMenuItem executeButton = gui.getExecuteButton();
        executeButton.addActionListener(e -> {
            String message = gui.getjTextField();
            client.sendMessage(message);
        });

        JMenuItem exitButton = gui.getExitButton();
        exitButton.addActionListener(e -> {
            client.close();
            gui.dispose();
        });
    }
}
