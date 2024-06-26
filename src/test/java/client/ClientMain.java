package client;

import javax.swing.*;

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

        Timer timer = new Timer(1700, _ -> {
            Client_GUI gui = new Client_GUI(client);
            JMenuItem executeButton = gui.getExecuteButton();
            JTextArea outputTextArea = gui.getOutputTextArea();
            executeButton.addActionListener(_ -> {
                String message = gui.getjTextField();
                message = appendEndAfterInserts(message);
                client.sendMessage(message);

                SwingUtilities.invokeLater(() -> outputTextArea.setText(""));
            });

            JMenuItem exitButton = gui.getExitButton();
            exitButton.addActionListener(_ -> {
                client.close();
                gui.dispose();
            });

        });
        timer.setRepeats(false); // Csak egyszer fusson le
        timer.start();

    }

    private static String appendEndAfterInserts(String message) {
        String[] lines = message.split("\\n");
        StringBuilder modifiedMessage = new StringBuilder();

        boolean insideInsertBlock = false;

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            modifiedMessage.append(line).append("\n");

            if (line.toLowerCase().startsWith("insert")) {
                insideInsertBlock = true;
            }

            boolean isLastLine = i == lines.length - 1;
            if (insideInsertBlock && (isLastLine || !lines[i + 1].trim().toLowerCase().startsWith("insert"))) {
                modifiedMessage.append("end\n");
                insideInsertBlock = false;
            }
        }

        return modifiedMessage.toString();
    }

}

