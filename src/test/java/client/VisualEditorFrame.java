package client;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

public class VisualEditorFrame extends JFrame {
    private JPanel panel;
    private JTable table;
    private String clickedtbname;

    public VisualEditorFrame(String tablename){
        this.clickedtbname = tablename;

        panel = new JPanel();
        panel.setPreferredSize(new Dimension(300, 300));
        panel.setBackground(new Color(100,0,0));

        table = new JTable();

        // valahogy bekell tenni a tabla oszlopait + sorait ( tippre egy for )

        DefaultTableModel model = new DefaultTableModel();
        model.addColumn("zsido");
        model.addColumn("hitelr");

        JLabel label = new JLabel(tablename);
        label.setBackground(Color.LIGHT_GRAY);
        panel.add(label);



        table.setModel(model);

        JScrollPane tscp = new JScrollPane(table);
        tscp.setPreferredSize(new Dimension(200,250));
        panel.add(tscp);

        this.add(panel);

        this.setPreferredSize(new Dimension(300, 375));
        this.pack();
        this.setVisible(true);
        this.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
    }
}
