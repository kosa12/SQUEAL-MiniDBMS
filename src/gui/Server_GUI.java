package gui;

import server.Server;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

public class Server_GUI extends JFrame {
    private JPanel spanel;
    private JButton start,stop;
    private JTextField jTextField;
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
                    //throw new RuntimeException(ex);
                    System.out.println(" ");
                }
                stopServer = true;
            }
        });


        jTextField = new JTextField();
        jTextField.setPreferredSize(new Dimension(450,300));

        spanel.add(start);
        spanel.add(jTextField);
        spanel.add(stop);

        this.add(spanel);

        this.setBounds(350,300,800,350);
        this.setVisible(true);
        this.setResizable(false);

    }

    public boolean getstopServer()
    {
        return stopServer;
    }

    private void startServer() {
        server = new Server();
        server.start();
    }

    public static void main(String[] args) {
        new Server_GUI();
    }
}
