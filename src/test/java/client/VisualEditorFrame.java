package client;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.FileReader;
import java.io.IOException;

public class VisualEditorFrame extends JFrame {
    private JPanel panel;
    private JTable table;
    private String clickedTableName;

    public VisualEditorFrame( String tableName, String dbName) {
        this.clickedTableName = tableName;

        panel = new JPanel();
        panel.setPreferredSize(new Dimension(300, 300));
        panel.setBackground(new Color(100, 0, 0));

        table = new JTable();

        DefaultTableModel model = new DefaultTableModel();

        try {
            JSONParser parser = new JSONParser();
            Object obj = parser.parse(new FileReader("src/test/java/databases/" + dbName  + ".json"));
            JSONArray databaseArray = (JSONArray) obj;

            for (Object databaseObj : databaseArray) {
                JSONObject databaseJson = (JSONObject) databaseObj;
                String databaseName = (String) databaseJson.get("database_name");
                if (databaseName.equals(dbName)) {
                    JSONArray tablesArray = (JSONArray) databaseJson.get("tables");
                    for (Object tableObj : tablesArray) {
                        JSONObject tableJson = (JSONObject) tableObj;
                        String currentTableName = (String) tableJson.get("table_name");
                        if (currentTableName.equals(tableName)) {
                            JSONArray attributesArray = (JSONArray) tableJson.get("attributes");
                            for (Object attributeObj : attributesArray) {
                                JSONObject attributeJson = (JSONObject) attributeObj;
                                String attributeName = (String) attributeJson.get("name");
                                model.addColumn(attributeName);
                            }
                            break;
                        }
                    }
                    break;
                }
            }
        } catch (IOException | ParseException e) {
            e.printStackTrace();
        }

        JLabel label = new JLabel(tableName);
        label.setBackground(Color.LIGHT_GRAY);
        panel.add(label);

        table.setModel(model);

        JScrollPane tscp = new JScrollPane(table);
        tscp.setPreferredSize(new Dimension(200, 250));
        panel.add(tscp);

        this.add(panel);

        this.setPreferredSize(new Dimension(300, 375));
        this.pack();
        this.setVisible(true);
        this.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
    }
}
