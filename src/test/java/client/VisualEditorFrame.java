package client;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

import org.bson.Document;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import server.MongoDBHandler;

public class VisualEditorFrame extends JFrame {
    private JPanel panel, bpanel;
    private JFrame insertFrame;
    private JTable table;
    private String clickedTableName;
    private String currentDatabase;
    private JButton delSelRow, insertNewRow, go;

    public VisualEditorFrame(String tableName, String databaseName) {
        this.clickedTableName = tableName;
        this.currentDatabase = databaseName;

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

        delSelRow.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row != -1) {
                DefaultTableModel model_new = (DefaultTableModel) table.getModel();
                String primaryKeyValue = (String) model_new.getValueAt(row, 0);

                model_new.removeRow(row);

                MongoDBHandler mongoDBHandler = new MongoDBHandler();
                try {
                    mongoDBHandler.deleteDocumentByPK(currentDatabase, clickedTableName, primaryKeyValue);
                } catch (Exception ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(null, "Error deleting row from MongoDB.");
                } finally {
                    mongoDBHandler.close();
                }
            } else {
                JOptionPane.showMessageDialog(null, "Please select a row to delete.");
            }
        });


        insertNewRow.addActionListener(e -> {
            insertFrame = new JFrame();
            JPanel insertPanel = new JPanel();
            DefaultTableModel newModel = (DefaultTableModel) table.getModel();
            JButton confirmButton = new JButton("Confirm");
            confirmButton.addActionListener(actionEvent -> {

                String[] columnNames = new String[newModel.getColumnCount()];
                for (int i = 0; i < newModel.getColumnCount(); i++) {
                    columnNames[i] = newModel.getColumnName(i);
                }

                Object primaryKeyValue = JOptionPane.showInputDialog("Enter value for primary key (_id)");

                StringBuilder concatenatedValue = new StringBuilder();
                for (int i = 1; i < newModel.getColumnCount(); i++) {
                    String columnName = newModel.getColumnName(i);
                    String columnValue = JOptionPane.showInputDialog("Enter value for " + columnName);
                    concatenatedValue.append(columnValue);
                    if (i < newModel.getColumnCount() - 1) {
                        concatenatedValue.append(";");
                    }
                }

                Document document = new Document();
                document.append("_id", primaryKeyValue);
                document.append("ertek", concatenatedValue.toString());

                String[] splitValues = concatenatedValue.toString().split(";");
                Object[] rowData = new Object[newModel.getColumnCount()];
                rowData[0] = primaryKeyValue;
                for (int i = 1; i < newModel.getColumnCount(); i++) {
                    rowData[i] = splitValues[i - 1];
                }
                newModel.addRow(rowData);

                MongoDBHandler mongoDBHandler = new MongoDBHandler();
                try {
                    mongoDBHandler.insertDocument(currentDatabase, clickedTableName, document);
                } catch (Exception ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(null, "Error inserting row into MongoDB.");
                } finally {
                    mongoDBHandler.close();
                }

                insertFrame.dispose();
            });

            insertPanel.add(confirmButton);

            insertFrame.add(insertPanel);
            insertFrame.setPreferredSize(new Dimension(300, newModel.getColumnCount() * 50));
            insertFrame.setLocationRelativeTo(null);
            insertFrame.pack();
            insertFrame.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
            insertFrame.setVisible(true);
        });







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
