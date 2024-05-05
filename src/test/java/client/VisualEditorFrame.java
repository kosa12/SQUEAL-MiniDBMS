package client;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import server.MongoDBHandler;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class VisualEditorFrame extends JFrame {
    private final JPanel panel;
    private final JPanel bpanel;
    private final JTable table;
    private final String clickedTableName;
    private final String currentDatabase;
    private final JButton delSelRow;
    private final JButton insertNewRow;
    private final Client client;

    private final JButton exit;

    public VisualEditorFrame(String tableName, String databaseName, Client client) {
        this.clickedTableName = tableName;
        this.currentDatabase = databaseName;
        this.client = client;

        panel = new JPanel();
        panel.setBackground(new Color(75, 104, 178));

        delSelRow = new JButton("Delete Selected Row");
        insertNewRow = new JButton("Insert Row");

        ImageIcon exiticon = new ImageIcon("src/main/resources/exitXD.png");

        Image ximage = exiticon.getImage();
        Image xnewimg = ximage.getScaledInstance(50, 50,  java.awt.Image.SCALE_SMOOTH);
        exiticon = new ImageIcon(xnewimg);

        exit = new JButton(exiticon);
        exit.setBorderPainted(false);
        exit.setBackground(new Color(75, 104, 178));
        exit.setFocusPainted(false);
        exit.setFocusable(false);

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
        assert attributeNames != null;
        tscp.setPreferredSize(new Dimension(attributeNames.length * 200, 450));

        panel.add(tscp);
        panel.setBounds(180, 0, attributeNames.length * 200, 470);

        bpanel = new JPanel();
        bpanel.setLayout(new FlowLayout());

        insertNewRow.setPreferredSize(new Dimension(160, 100));
        delSelRow.setPreferredSize(new Dimension(160, 100));
        exit.setPreferredSize(new Dimension(160, 100));

        delSelRow.addActionListener(_ -> deleteSelectedRow());
        insertNewRow.addActionListener(_ -> insertRow());
        exit.addActionListener(_ -> exitNOW());

        bpanel.add(exit);
        bpanel.add(insertNewRow);
        bpanel.add(delSelRow);


        bpanel.setBackground(new Color(75, 104, 178));
        bpanel.setBounds(0, 0, 180, 500);

        this.setLayout(null);
        this.add(panel);
        this.add(bpanel);
        Image icon = Toolkit.getDefaultToolkit().getImage("src/main/resources/dbicon.png");
        setIconImage(icon);
        this.setPreferredSize(new Dimension(attributeNames.length * 200 + 200, 500));
        this.pack();
        this.setVisible(true);
        this.setResizable(false);
        this.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
    }

    private void exitNOW() {
        this.dispose();
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
            if (rowData[i] == null || rowData[i].toString().trim().isEmpty()) {
                JOptionPane.showMessageDialog(null, "This should not be null or empty, add a value next time");
                return;
            }
        }


        Object primaryKeyValue = rowData[0];
        for (int i = 0; i < model.getRowCount(); i++) {
            if (model.getValueAt(i, 0).equals(primaryKeyValue)) {
                JOptionPane.showMessageDialog(null, "Primary key value already exists: " + primaryKeyValue);
                return;
            }
        }

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
        model.addRow(rowData);

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
