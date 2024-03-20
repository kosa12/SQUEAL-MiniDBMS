package server;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

public class Server_GUI extends JFrame {
    private JButton start, stop;
    private JTextArea logTextArea;
    private Server server;

    public Server_GUI() {
        setTitle("Server GUI");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        setPreferredSize(new Dimension(1200, 350));

        start = new JButton("START SERVER");
        stop = new JButton("STOP SERVER");

        start.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                startServer();
            }
        });

        stop.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                stopServer();
            }
        });

        JPanel buttonPanel = new JPanel();
        buttonPanel.add(start);
        buttonPanel.add(stop);

        logTextArea = new JTextArea();
        logTextArea.setFont(new Font("Cfont", Font.PLAIN, 20));
        logTextArea.setEditable(false);

        JScrollPane scrollPane = new JScrollPane(logTextArea);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

        add(buttonPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);

        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    private void startServer() {
        server = new Server();
        redirectOutput();
        server.start();
    }

    private void stopServer() {
        try {
            server.getServerSocket().close();
            logMessage("Server is shutting down...");
            System.exit(0);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    private void redirectOutput() {
        RedirectOutputStream redirectOutputStream = new RedirectOutputStream(logTextArea);
        System.setOut(new PrintStream(redirectOutputStream));
    }

    private void logMessage(String message) {
        logTextArea.append(message + "\n");
        logTextArea.setCaretPosition(logTextArea.getDocument().getLength());
    }

    class RedirectOutputStream extends OutputStream {
        private JTextArea textArea;

        public RedirectOutputStream(JTextArea textArea) {
            this.textArea = textArea;
        }

        @Override
        public void write(int b) {
            textArea.append(String.valueOf((char) b));
            textArea.setCaretPosition(textArea.getDocument().getLength());
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                new Server_GUI();
            }
        });
    }
}
