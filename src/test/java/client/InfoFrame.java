package client;

import javax.swing.*;
import java.awt.*;

public class InfoFrame extends JFrame {
    private JPanel panel;
    private JTextArea textArea;
    public InfoFrame() {
        panel = new JPanel();
        panel.setLayout(new FlowLayout());
        panel.setBounds(50, 50, 500, 500);
        this.add(panel);

        textArea = new JTextArea();

        textArea.setText("SYNTAXES\n\n\n" +
                "DATABASE" +
                "\n-select: use _name_;" +
                "\n-create: create database  _name_;" +
                "\n-delete: drop database  _name_;\n\n" +
                "\n-show: show databases;" +
                "\n\nTABLE" +
                "\n-create: create table  _name_ (attributes);" +
                "\n-delete: drop table _name_;" +
                "\n-show: show tables;" +
                "\n\nINDEX" +
                "\n-create: create index  _name_  on  _tablename_ (attributes);\n\n" +
                "INSERT NEW ROW" +
                "\ninsert into (attribute_names) _tablename_  values (attributes);\n\n" +
                "DELETE ROW" +
                "\ndelete from _tablename_ where _id=VALUE;");


        Image icon = Toolkit.getDefaultToolkit().getImage("src/main/resources/infoic.png");
        setIconImage(icon);

        panel.add(textArea);
        textArea.setEditable(false);
        textArea.setHighlighter(null);

        this.setLayout(new FlowLayout());
        this.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        this.setSize(500, 500);
        this.setVisible(true);
    }

}