package client;

import com.mongodb.client.*;
import data.Database;
import data.Table;
import org.bson.Document;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.ArrayList;

public class QueryDesignerFrame extends JFrame {
    private JPanel panel,outputPanel;
    private JTextArea jTextArea;
    private List<String> checkBoxes;
    private String currentdb;

    public QueryDesignerFrame(List<String> jCheckBoxes,String currentdatabase) {
        this.checkBoxes = jCheckBoxes;
        this.currentdb = currentdatabase;
        panel = new JPanel();
        panel.setBackground(Color.RED);
        this.setLayout(new BorderLayout());

        projectTables(checkBoxes,panel);
        jTextArea = new JTextArea();
        jTextArea.setPreferredSize(new Dimension(970,200));
        jTextArea.setEditable(false);

        this.add(jTextArea,BorderLayout.SOUTH);

        this.add(panel,BorderLayout.CENTER);

        this.setSize(1000, 800);
        this.setLocationRelativeTo(null);
        Image icon = Toolkit.getDefaultToolkit().getImage("src/main/resources/qdic.png");
        setIconImage(icon);
        this.setVisible(true);
        this.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
    }

    private void projectTables(List<String> checkBoxes, JPanel panel) {
        JSONObject jsontabla = null;
        int aux;
        for (int i = 0; i < checkBoxes.size(); i++) {
            jsontabla = readTableFormat(currentdb, checkBoxes.get(i));
            if (jsontabla == null) {
                System.out.println("Nem sikerült betölteni a táblázatot: " + checkBoxes.get(i));
                continue;
            }
            JSONArray jsonArrayTable = (JSONArray) jsontabla.get("attributes");

            DefaultTableModel model = new DefaultTableModel(null, new Object[]{checkBoxes.get(i),"Select"}){
                @Override
                public boolean isCellEditable(int row, int column) {
                    return column == 1;
                }

                @Override
                public Class<?> getColumnClass(int columnIndex) {
                    if (columnIndex == 1) {
                        return Boolean.class;
                    }
                    return super.getColumnClass(columnIndex);
                }
            };

            aux = 0;
            for (Object o : jsonArrayTable) {
                JSONObject attribut = (JSONObject) o;
                String attributName = (String) attribut.get("name");
                model.addRow(new Object[]{attributName,false});
                aux++;
            }

            JTable table = new JTable(model);
            JScrollPane scrollPane = new JScrollPane(table);
            table.setFillsViewportHeight(true);
            table.setPreferredSize(new Dimension(100,aux * 20));
            table.setLocation(i * 50, i * 50);
            table.setPreferredScrollableViewportSize(table.getPreferredSize());
            panel.add(scrollPane);
        }

    }



    public static JSONObject readTableFormat(String databaseName, String tableName) {
        JSONParser parser = new JSONParser();
        try {
            String filePath = "src/test/java/databases/" + databaseName + ".json";
            FileReader reader = new FileReader(filePath);
            Object obj = parser.parse(reader);

            JSONArray databaseArray = (JSONArray) obj;

            for (Object databaseObj : databaseArray) {
                JSONObject database = (JSONObject) databaseObj;
                String dbName = (String) database.get("database_name");
                if (dbName.equals(databaseName)) {
                    JSONArray tables = (JSONArray) database.get("tables");
                    for (Object tableObj : tables) {
                        JSONObject table = (JSONObject) tableObj;
                        String tableNameInJson = (String) table.get("table_name");
                        if (tableNameInJson.equals(tableName)) {
                            return table;
                        }
                    }
                }
            }
            return null;
        } catch (IOException | ParseException e) {
            return null;
        }
    }

}