package gui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

public class GUI extends JFrame {
    private final JPanel jPanel;
    private final JButton execute;
    private final JButton clear;
    private final JButton refresh;
    public final JTextField jTextField;
    private JLabel jLabel;
    private final JMenuBar jMenuBar;

    public GUI() {
        jPanel = new JPanel();

        this.setLayout(new BorderLayout());

        JButton exit = new JButton("EXIT");
        execute = new JButton("XECUTE");
        clear = new JButton("Clear");
        refresh = new JButton("Refresh");

        jMenuBar = new JMenuBar();
        jMenuBar.add(exit);
        jMenuBar.add(execute);
        jMenuBar.add(clear);
        jMenuBar.add(refresh);

        jTextField = new JTextField();
        jTextField.setFont(new Font("Cfont", Font.ITALIC, 20));

        jTextField.setPreferredSize(new Dimension(800, 600));


        jTextField.setBackground(new Color(159, 172, 194));
        jPanel.add(jTextField);

        exit.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });

        clear.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                jTextField.setText("");
            }
        });

        jTextField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    saveText();
                }
            }
        });

        this.add(jMenuBar, BorderLayout.NORTH);
        this.add(jPanel, BorderLayout.CENTER);

        this.setExtendedState(JFrame.MAXIMIZED_BOTH);
        this.setVisible(true);
        this.setDefaultCloseOperation(EXIT_ON_CLOSE);
        this.setResizable(false);
    }

    public void saveText() {
        String text = jTextField.getText();
        System.out.println("Mentve: " + text);
    }

    public String getjTextField() {
        return jTextField.getText();
    }

    public static void main(String[] args) {
        new GUI();
    }
}