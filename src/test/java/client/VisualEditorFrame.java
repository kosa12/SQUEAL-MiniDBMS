package client;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

public class VisualEditorFrame extends JFrame {
    private JPanel panel;
    private JTable table;
    private String clickedTableName;
    private String currentDatabase;

    public VisualEditorFrame(String tableName, String databaseName) {
        this.clickedTableName = tableName;
        this.currentDatabase = databaseName;

        panel = new JPanel();
        panel.setPreferredSize(new Dimension(300, 300));
        panel.setBackground(new Color(100, 0, 0));
        this.setLocationRelativeTo(null);

        table = new JTable();

        DefaultTableModel model = new DefaultTableModel();

        // valahogy bekell tenni a tabla oszlopait + sorait ( tippre egy for )
        // Spoiler: sok for volt XDDDDDDD

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

        table.setModel(model);

        JScrollPane tscp = new JScrollPane(table);
        tscp.setPreferredSize(new Dimension(400, 450));
        panel.add(tscp);

        this.add(panel);

        this.setPreferredSize(new Dimension(400, 475));
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

        try {
            MongoClient mongoClient = MongoClients.create("mongodb://localhost:27017");
            MongoDatabase database = mongoClient.getDatabase(databaseName);
            MongoCollection<Document> collection = database.getCollection(tableName);

            for (Document document : collection.find()) {
                String[] rowData = new String[attributeNames.length];

                Object idObj = document.get("_id");
                if (idObj != null) {
                    rowData[0] = idObj.toString();
                } else {
                    System.err.println("Primary key (_id) not found in the document.");
                    continue;
                }

                String ertek = document.getString("ertek");
                if (ertek != null && !ertek.isEmpty()) {
                    String[] attributeValues = ertek.split(";");
                    if (attributeValues.length == attributeNames.length - 1) {
                        for (int i = 0; i < attributeNames.length - 1; i++) {
                            rowData[i + 1] = attributeValues[i];
                        }
                    } else {

                        System.err.println("Number of attribute values does not match the number of attribute names.");
                        continue;
                    }
                } else {
                    System.err.println("The 'érték' field is empty or null.");
                    continue;
                }
                rows.add(rowData);
            }

            mongoClient.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return rows;
    }






}
