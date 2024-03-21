package client;

import javax.swing.*;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
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
    private final DefaultMutableTreeNode root;

    public FileExplorer() {
        root = new DefaultMutableTreeNode("databases");

        File rootDirectory = new File("src\\test\\java\\databases");
        addFiles(root, rootDirectory);

        tree = new JTree(root);
        tree.setPreferredSize(new Dimension(150, 375));

        tree.addTreeSelectionListener(new TreeSelectionListener() {
            @Override
            public void valueChanged(TreeSelectionEvent e) {
                DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();
                if (selectedNode != null && selectedNode.isLeaf()) {
                    showTables(selectedNode);
                }
            }
        });

        add(new JScrollPane(tree));

        setVisible(true);
    }

    public void refreshTree() {
        root.removeAllChildren();
        addFiles(root, new File("src\\test\\java\\databases"));
        ((DefaultTreeModel) tree.getModel()).reload();
    }

    private void addFiles(DefaultMutableTreeNode parentNode, File parentFile) {
        if (!parentFile.isDirectory()) return;

        File[] files = parentFile.listFiles();
        if (files != null) {
            for (File file : files) {
                String nodeName = file.isDirectory() ? file.getName() : file.getName().replace(".json", "");
                DefaultMutableTreeNode node = new DefaultMutableTreeNode(nodeName);
                parentNode.add(node);
                if (file.isDirectory()) {
                    addFiles(node, file);
                }
            }
        }
    }

    private void showTables(DefaultMutableTreeNode node) {
        node.removeAllChildren();

        String dbName = node.getUserObject().toString();
        File databaseFile = new File("src\\test\\java\\databases\\" + dbName + ".json");

        String[] tables = getTablesFromDatabase(databaseFile);
        if (tables != null) {
            for (String tableName : tables) {
                node.add(new DefaultMutableTreeNode(tableName));
            }
        }

        ((DefaultTreeModel) tree.getModel()).reload(node);
    }

    private String[] getTablesFromDatabase(File databaseFile) {
        JSONParser parser = new JSONParser();
        try {
            FileReader reader = new FileReader(databaseFile);
            Object obj = parser.parse(reader);
            JSONArray databaseArray = (JSONArray) obj;
            JSONObject databaseJson = (JSONObject) databaseArray.getFirst();
            JSONArray tablesArray = (JSONArray) databaseJson.get("tables");

            String[] tables = new String[tablesArray.size()];
            for (int i = 0; i < tablesArray.size(); i++) {
                JSONObject tableObj = (JSONObject) tablesArray.get(i);
                tables[i] = (String) tableObj.get("table_name");
            }
            return tables;
        } catch (IOException | ParseException e) {
            e.printStackTrace();
            return null;
        }
    }
}
