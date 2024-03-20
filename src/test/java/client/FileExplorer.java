package client;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import java.awt.*;
import java.io.File;

public class FileExplorer extends JPanel {
    private JTree tree;
    private DefaultMutableTreeNode root;

    public FileExplorer() {
        root = new DefaultMutableTreeNode("databases");

        // Set the root directory you want to explore
        File rootDirectory = new File("src\\test\\java\\databases");
        addFiles(root, rootDirectory);

        tree = new JTree(root);
        tree.setPreferredSize(new Dimension(150,375));
        add(new JScrollPane(tree));

        setVisible(true);
    }

    public void refreshTree() {
        root.removeAllChildren();
        addFiles(root, new File("src\\test\\java\\databases")); // Rebuild the tree
        ((DefaultTreeModel) tree.getModel()).reload(); // Reload the tree model
    }

    private void addFiles(DefaultMutableTreeNode parentNode, File parentFile) {
        if (!parentFile.isDirectory()) return;

        File[] files = parentFile.listFiles();
        if (files != null) {
            for (File file : files) {
                DefaultMutableTreeNode node = new DefaultMutableTreeNode(file.getName());
                parentNode.add(node);
                if (file.isDirectory()) {
                    addFiles(node, file);
                }
            }
        }
    }

}
