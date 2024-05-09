package client;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;

public class SelectorFrame extends JFrame {
    private JPanel jPanel;
    private JButton go;
    private String currentDatabase;
    private static JCheckBox[] jCheckBoxes;
    private String[] selectedTableNames;
    private List<String> stringList;
    public SelectorFrame(String databasename){
        currentDatabase = databasename;

        jPanel = new JPanel();

        go = new JButton("GO!");

        Image icon = Toolkit.getDefaultToolkit().getImage("src/main/resources/qdic.png");
        setIconImage(icon);

        readTableFormat(jPanel,currentDatabase);

        jPanel.add(go);

        go.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                stringList = new ArrayList<>();
                for(JCheckBox checkBox : jCheckBoxes){
                    if(checkBox.isSelected()){
                        stringList.add(checkBox.getText());
                    }
                }
                System.out.println(stringList);
                dispose();
            }
        });



        jPanel.setLayout(new BoxLayout(jPanel,BoxLayout.Y_AXIS));
        this.setSize(250,250);
        this.add(jPanel);
        this.setLocationRelativeTo(null);
        this.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        this.setVisible(true);
    }

    public static JSONObject readTableFormat(JPanel panel,String databaseName) {
        JSONParser parser = new JSONParser();
        try {
            String filePath = "src/test/java/databases/" + databaseName + ".json";
            FileReader reader = new FileReader(filePath);
            Object obj = parser.parse(reader);

            JSONArray databaseArray = (JSONArray) obj;

            int index = 0;

            for (Object databaseObj : databaseArray) {
                JSONObject database = (JSONObject) databaseObj;
                String dbName = (String) database.get("database_name");

                if (dbName.equals(databaseName)) {
                    JSONArray tables = (JSONArray) database.get("tables");

                    jCheckBoxes = new JCheckBox[tables.size()];

                    for (Object tableObj : tables) {
                        JSONObject table = (JSONObject) tableObj;
                        String tableNameInJson = (String) table.get("table_name");
                        System.out.println(tableNameInJson);

                        jCheckBoxes[index] = new JCheckBox(tableNameInJson);
                        panel.add(jCheckBoxes[index]);
                        index++;
                    }
                }
            }
            return null;
        } catch (IOException | ParseException e) {
            System.out.println("Error reading table format JSON file: " + e.getMessage());
            return null;
        }
    }

    public static JCheckBox[] getjCheckBoxes() {
        return jCheckBoxes;
    }
}