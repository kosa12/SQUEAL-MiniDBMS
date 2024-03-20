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
    private JPanel spanel;
    private JButton start,stop;
    private JTextArea jTextField;
    private Server server;
    private boolean stopServer = false;

    public Server_GUI(){
        spanel = new JPanel();

        start = new JButton("START SERVER");
        start.setPreferredSize(new Dimension(150,300));

        stop = new JButton("STOP SERVER");
        stop.setPreferredSize(new Dimension(150,300));

        start.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                startServer();
            }
        });

        stop.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    server.getServerSocket().close();
                    System.out.println("Server is shutting down...");
                    System.exit(0);
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
                stopServer = true;
            }
        });


        jTextField = new JTextArea();
        jTextField.setPreferredSize(new Dimension(850,300));

        int padding = 15;
        Insets insets = new Insets(padding,padding,padding,padding);

        jTextField.setBorder(new EmptyBorder(insets));
        jTextField.setFont(new Font("Cfont", Font.PLAIN, 20));

        RedirectOutputStream redirectOutputStream = new RedirectOutputStream(jTextField);
        System.setOut(new PrintStream(redirectOutputStream));

        JScrollPane scrollPane = new JScrollPane(jTextField);
        scrollPane.setPreferredSize(new Dimension(850, 300));
        this.add(scrollPane);

        spanel.add(start);
        spanel.add(scrollPane);
        spanel.add(stop);

        this.add(spanel);

        this.setBounds(350,300,1200,350);
        this.setVisible(true);
        this.setResizable(false);
        this.setLocationRelativeTo(null);
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

    private void startServer() {
        server = new Server();
        server.start();
    }

    public static void main(String[] args) {
        new Server_GUI();
    }
}