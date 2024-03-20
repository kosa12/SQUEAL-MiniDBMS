package client;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.io.PrintStream;

public class Client_GUI extends JFrame {
    private final JPanel jPanel;
    private final JMenuItem execute;
    private final JMenuItem clear;
    private final JMenuItem refresh;
    private final JTextArea querry, output;
    private JLabel jLabel;
    private final JMenuBar jMenuBar;

    private final JMenuItem exit;

    private FileExplorer fileExplorer;

    public Client_GUI() {
        jPanel = new JPanel();

        this.setLayout(new BorderLayout());

        int padding = 15;
        Insets insets = new Insets(padding,padding,padding,padding);


        exit = new JMenuItem("EXIT");
        exit.setPreferredSize(new Dimension(60,40));
        exit.setBackground(new Color(75, 104, 178));
        //exit.setBorder(new EmptyBorder(insets));

        exit.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                exit.setBackground(Color.LIGHT_GRAY);
            }
            @Override
            public void mouseExited(MouseEvent e) {
                exit.setBackground(new Color(75, 104, 178));
            }
        });


        execute = new JMenuItem("EXECUTE");
        execute.setBackground(new Color(75, 104, 178));
        execute.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                execute.setBackground(Color.LIGHT_GRAY);
            }
            @Override
            public void mouseExited(MouseEvent e) {
                execute.setBackground(new Color(75, 104, 178));
            }
        });

        clear = new JMenuItem("CLEAR");
        clear.setBackground(new Color(75, 104, 178));
        clear.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                clear.setBackground(Color.LIGHT_GRAY);
            }
            @Override
            public void mouseExited(MouseEvent e) {
                clear.setBackground(new Color(75, 104, 178));
            }
        });

        refresh = new JMenuItem("REFRESH");
        refresh.setBackground(new Color(75, 104, 178));
        refresh.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                refresh.setBackground(Color.LIGHT_GRAY);
            }
            @Override
            public void mouseExited(MouseEvent e) {
                refresh.setBackground(new Color(75, 104, 178));
            }
        });

        jMenuBar = new JMenuBar();

        jMenuBar.add(exit);
        jMenuBar.add(execute);
        jMenuBar.add(clear);
        jMenuBar.add(refresh);

        fileExplorer = new FileExplorer();
        fileExplorer.setPreferredSize(new Dimension(175,300));
        //this.add(fileExplorer);


        querry = new JTextArea();
        querry.setFont(new Font("Cfont", Font.ITALIC, 20));

        querry.setPreferredSize(new Dimension(1350, 400));
        querry.setText("Write your command(s) here");

        querry.setBorder(new EmptyBorder(insets));

        querry.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                querry.setText("");
                querry.setFont(new Font("Cfont", Font.PLAIN, 20));

            }
        });

        output = new JTextArea();
        output.setFont(new Font("Cfont", Font.ITALIC, 20));

        output.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                output.setText("");
                output.setFont(new Font("Cfont", Font.PLAIN, 20));

            }
        });

        output.setBorder(new EmptyBorder(insets));

        output.setPreferredSize(new Dimension(1350, 400));
        output.setText("ez meg az output amit majd kiad");

        querry.setBackground(new Color(239, 240, 243));

        output.setBackground(new Color(239, 240, 243));

        output.setEditable(false);

        this.add(fileExplorer,BorderLayout.WEST);
        jPanel.add(querry);
        jPanel.add(output);

        exit.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });

        clear.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                querry.setText("");
            }
        });


        this.add(jMenuBar, BorderLayout.NORTH);
        this.add(jPanel, BorderLayout.CENTER);

        jPanel.setBackground(new Color(75, 104, 178));

        this.setExtendedState(JFrame.MAXIMIZED_BOTH);
        this.setVisible(true);
        this.setDefaultCloseOperation(EXIT_ON_CLOSE);
        this.setResizable(false);
    }

    public JMenuItem getExecuteButton() {
        return execute;
    }


    public String getjTextField() {
        return querry.getText();
    }

    public JMenuItem getExitButton() {
        return exit;
    }

}