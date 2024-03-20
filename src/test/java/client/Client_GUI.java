package client;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class Client_GUI extends JFrame {
    private final JPanel jPanel;
    private final JButton execute;
    private final JButton clear;
    private final JButton refresh;
    public final JTextField querry;

    public final JTextField output;
    private JLabel jLabel;
    private final JMenuBar jMenuBar;

    private final JButton exit;

    public Client_GUI() {
        jPanel = new JPanel();

        this.setLayout(new BorderLayout());

        exit = new JButton("EXIT");
        execute = new JButton("XECUTE");
        clear = new JButton("Clear");
        refresh = new JButton("Refresh");

        jMenuBar = new JMenuBar();
        jMenuBar.add(exit);
        jMenuBar.add(execute);
        jMenuBar.add(clear);
        jMenuBar.add(refresh);

        querry = new JTextField();
        querry.setFont(new Font("Cfont", Font.ITALIC, 20));

        querry.setPreferredSize(new Dimension(1000, 100));
        querry.setText("ez lesz a parancs sor");


        output = new JTextField();
        output.setFont(new Font("Cfont", Font.ITALIC, 20));

        output.setPreferredSize(new Dimension(1000, 600));
        output.setText("ez meg az output amit majd kiad");


        querry.setBackground(new Color(159, 172, 194));

        output.setBackground(new Color(159, 172, 194));


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

        this.setExtendedState(JFrame.MAXIMIZED_BOTH);
        this.setVisible(true);
        this.setDefaultCloseOperation(EXIT_ON_CLOSE);
        this.setResizable(false);
    }

    public JButton getExecuteButton() {
        return execute;
    }


    public String getjTextField() {
        return querry.getText();
    }

    public JButton getExitButton() {
        return exit;
    }


    public static void main(String[] args) {
        new Client_GUI();
    }
}
