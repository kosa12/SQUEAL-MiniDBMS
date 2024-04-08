package client;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import server.MongoDBHandler;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.io.FileReader;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class VisualEditorFrame extends JFrame {
    private JPanel panel, bpanel;
    private JTable table;
    private String clickedTableName;
    private String currentDatabase;
    private JButton delSelRow, insertNewRow;
    private Client client;

    public VisualEditorFrame(String tableName, String databaseName, Client client) {
        this.clickedTableName = tableName;
        this.currentDatabase = databaseName;
        this.client = client;

        panel = new JPanel();
        panel.setBackground(new Color(100, 0, 0));

        delSelRow = new JButton("Delete Selected Row");
        insertNewRow = new JButton("Insert Row");

        table = new JTable();
        DefaultTableModel model = new DefaultTableModel();
        table.setModel(model);

        String[] attributeNames = getAttributeNamesFromJSON(databaseName, tableName);

        if (attributeNames != null && attributeNames.length > 0) {
            for (String attributeName : attributeNames) {
                model.addColumn(attributeName);
            }

            List<String[]> rows = fetchRowsFromMongoDB(databaseName, tableName, attributeNames);
            if (rows != null && !rows.isEmpty()) {
                for (String[] rowData : rows) {
                    model.addRow(rowData);
                }
            }
        }

        JScrollPane tscp = new JScrollPane(table);
        tscp.setPreferredSize(new Dimension(attributeNames.length * 200, 300));

        panel.add(tscp);
        panel.setBounds(50, 0, attributeNames.length * 200, 300);

        bpanel = new JPanel();
        bpanel.setLayout(new FlowLayout());

        insertNewRow.setPreferredSize(new Dimension(300, 50));
        delSelRow.setPreferredSize(new Dimension(300, 50));

        delSelRow.addActionListener(e -> deleteSelectedRow());
        insertNewRow.addActionListener(e -> insertRow());

        bpanel.add(insertNewRow);
        bpanel.add(delSelRow);

        bpanel.setBackground(new Color(100, 0, 0));
        bpanel.setBounds((attributeNames.length * 200) / 4, 300, 620, 60);

        this.setLayout(null);
        this.add(panel);
        this.add(bpanel);

        this.setPreferredSize(new Dimension(attributeNames.length * 200 + 500, 500));
        this.pack();
        this.setVisible(true);
        this.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
    }

    private void deleteSelectedRow() {
        int row = table.getSelectedRow();
        if (row != -1) {
            DefaultTableModel model = (DefaultTableModel) table.getModel();
            String primaryKeyValue = (String) model.getValueAt(row, 0);
            model.removeRow(row);
            String useCommand = "USE " + currentDatabase + ";";
            client.sendMessage(useCommand);
            String command = "DELETE FROM " + clickedTableName + " WHERE _id=" + primaryKeyValue + ";";
            client.sendMessage(command);
        } else {
            JOptionPane.showMessageDialog(null, "Please select a row to delete.");
        }
    }

    private void insertRow() {
        DefaultTableModel model = (DefaultTableModel) table.getModel();
        int columnCount = model.getColumnCount();
        Object[] rowData = new Object[columnCount];
        String[] columnNames = new String[columnCount];

        for (int i = 0; i < columnCount; i++) {
            columnNames[i] = model.getColumnName(i);
        }

        for (int i = 0; i < columnCount; i++) {
            rowData[i] = JOptionPane.showInputDialog("Enter value for " + columnNames[i]);
        }

        model.addRow(rowData);

        StringBuilder command = new StringBuilder("INSERT INTO " + clickedTableName + " (");

        for (int i = 0; i < columnCount; i++) {
            command.append(columnNames[i]);
            if (i < columnCount - 1) {
                command.append(", ");
            }
        }
        command.append(") VALUES (");

        for (int i = 0; i < columnCount; i++) {
            command.append(rowData[i]);
            if (i < columnCount - 1) {
                command.append(",");
            }
        }
        command.append(");");
        client.sendMessage("USE " + currentDatabase + ";");
        client.sendMessage(command.toString());
    }


    private String[] getAttributeNamesFromJSON(String databaseName, String tableName) {
        try {
            String jsonFilePath = "src/test/java/databases/" + databaseName + ".json";
            File file = new File(jsonFilePath);
            if (!file.exists()) {
                System.err.println("JSON file not found: " + jsonFilePath);
                return null;
            }

            JSONParser parser = new JSONParser();
            Object obj = parser.parse(new FileReader(jsonFilePath));
            JSONArray databaseArray = (JSONArray) obj;

            for (Object databaseObj : databaseArray) {
                JSONObject databaseJson = (JSONObject) databaseObj;
                String jsonDatabaseName = (String) databaseJson.get("database_name");
                if (jsonDatabaseName.equals(databaseName)) {
                    JSONArray tablesArray = (JSONArray) databaseJson.get("tables");
                    for (Object tableObj : tablesArray) {
                        JSONObject tableJson = (JSONObject) tableObj;
                        String jsonTableName = (String) tableJson.get("table_name");
                        if (jsonTableName.equals(tableName)) {
                            JSONArray attributesArray = (JSONArray) tableJson.get("attributes");
                            String[] attributeNames = new String[attributesArray.size()];
                            for (int i = 0; i < attributesArray.size(); i++) {
                                JSONObject attributeJson = (JSONObject) attributesArray.get(i);
                                attributeNames[i] = (String) attributeJson.get("name");
                            }
                            return attributeNames;
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private List<String[]> fetchRowsFromMongoDB(String databaseName, String tableName, String[] attributeNames) {
        List<String[]> rows = new ArrayList<>();
        MongoDBHandler mongoDBHandler = new MongoDBHandler();

        try {
            rows = mongoDBHandler.fetchRows(databaseName, tableName, attributeNames);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            mongoDBHandler.close();
        }
        return rows;
    }
}
