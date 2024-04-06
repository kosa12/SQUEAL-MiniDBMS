package client;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import javax.swing.JMenuItem;

public class Client {
    public static void main(String[] args) {
        String serverAddress = "localhost";
        int serverPort = 10000;



        final Socket[] socketHolder = new Socket[1];

        try {
            Socket socket = new Socket(serverAddress, serverPort);
            socketHolder[0] = socket;
        } catch (IOException ex) {
            System.out.println("The client is not connected to the server, please start the server!");
            return;
        }



        Client_GUI gui = new Client_GUI();

        JMenuItem executeButton = gui.getExecuteButton();
        executeButton.addActionListener(e -> {
            Socket socket = socketHolder[0];
            if (socket != null && !socket.isClosed()) {
                try {
                    PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                    String message = gui.getjTextField();
                    out.println(message);
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            } else {
                System.err.println("Not connected to the server.");
                // Optionally, display an error message to the user
            }
        });

        JMenuItem exitButton = gui.getExitButton();
        exitButton.addActionListener(e -> {
            Socket socket = socketHolder[0];
            if (socket != null && !socket.isClosed()) {
                try {
                    socket.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
            gui.dispose();
        });


    }
}
