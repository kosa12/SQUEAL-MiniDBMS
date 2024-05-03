package client;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class ClientMain {

    public static void main(String[] args) {
        String serverAddress = "localhost";
        int serverPort = 10000;

        Client client = new Client(serverAddress, serverPort);

        if (client.socket == null) {
            System.out.println("Failed to connect to the server.");
            return;
        }

        final boolean[] goGif = {false};


        GifPlayer gifPlayer = new GifPlayer();

        Timer timer = new Timer(1700, new ActionListener() {
            @Override

            public void actionPerformed(ActionEvent o) {
                Client_GUI gui = new Client_GUI(client);
                JMenuItem executeButton = gui.getExecuteButton();
                JTextArea outputTextArea = gui.getOutputTextArea();
                executeButton.addActionListener(e -> {
                    String message = gui.getjTextField();
                    client.sendMessage(message);
                    SwingUtilities.invokeLater(() -> outputTextArea.setText(""));
                });

                JMenuItem exitButton = gui.getExitButton();
                exitButton.addActionListener(e -> {
                    client.close();
                    gui.dispose();
                });

            }
        });
        timer.setRepeats(false); // Csak egyszer fusson le
        timer.start();

    }

}

