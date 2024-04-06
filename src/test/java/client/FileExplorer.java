package client;

import javax.swing.*;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultTreeModel;
import java.awt.*;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class FileExplorer extends JPanel {
    private final JTree tree;
    private final OurNode root;

    String currentDatabase;

    public FileExplorer() {
        this.setPreferredSize(new Dimension(175,1000));
        root = new OurNode("databases", currentDatabase);

        File rootDirectory = new File("src/test/java/databases/");
        addFiles(root, rootDirectory);

        tree = new JTree(root);
        tree.setPreferredSize(new Dimension(150, 2500));

        tree.addMouseListener(new OurNode.OurMouseListener());

        tree.addTreeSelectionListener(new TreeSelectionListener() {
            @Override
            public void valueChanged(TreeSelectionEvent e) {
                OurNode selectedNode = (OurNode) tree.getLastSelectedPathComponent();
                if (selectedNode != null && selectedNode.isLeaf()) {
                    showTables(selectedNode);
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(tree);

        scrollPane.setPreferredSize(new Dimension(175,700));
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

        this.add(scrollPane);

        setVisible(true);
    }

    public void refreshTree() {
        root.removeAllChildren();
        addFiles(root, new File("src/test/java/databases/"));
        ((DefaultTreeModel) tree.getModel()).reload();
    }

    private void addFiles(OurNode parentNode, File parentFile) {
        if (!parentFile.isDirectory()) return;

        File[] files = parentFile.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.length() == 0 || file.getName().contains("index")) {
                    continue;
                } else {
                    String nodeName = file.isDirectory() ? file.getName() : file.getName().replace(".json", "");
                    OurNode node = new OurNode(nodeName, currentDatabase);
                    parentNode.add(node);
                    if (file.isDirectory()) {
                        addFiles(node, file);
                    }
                }
            }
        }
    }

    private void showTables(OurNode node) {
        node.removeAllChildren();

        String dbName = node.getUserObject().toString();
        currentDatabase = dbName;
        File databaseFile = new File("src/test/java/databases/" + dbName + ".json");

        if(databaseFile.exists()){
            String[] tables = getTablesFromDatabase(databaseFile);
            if (tables != null) {
                for (String tableName : tables) {
                    node.add(new OurNode(tableName, currentDatabase));
                }
            }
            ((DefaultTreeModel) tree.getModel()).reload(node);
        }
    }

    private String[] getTablesFromDatabase(File databaseFile) {
        JSONParser parser = new JSONParser();
        try {
            FileReader reader = new FileReader(databaseFile);
            Object obj = parser.parse(reader);
            JSONArray databaseArray = (JSONArray) obj;
            JSONObject databaseJson = (JSONObject) databaseArray.getFirst();
            JSONArray tablesArray = (JSONArray) databaseJson.get("tables");
            if(tablesArray!=null){
                String[] tables = new String[tablesArray.size()];
                for (int i = 0; i < tablesArray.size(); i++) {
                    JSONObject tableObj = (JSONObject) tablesArray.get(i);
                    tables[i] = (String) tableObj.get("table_name");
                }
                return tables;
            }
            else return null;

        } catch (IOException | ParseException e) {
            e.printStackTrace();
            return null;
        }
    }
}
