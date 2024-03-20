package client;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import java.awt.*;
import java.io.File;

public class FileExplorer extends JPanel {
    private JTree tree;

    public FileExplorer() {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("Root");

        // Set the root directory you want to explore
        File rootDirectory = new File("src\\test\\java\\databases");
        addFiles(root, rootDirectory);

        tree = new JTree(root);
        tree.setPreferredSize(new Dimension(150,375));
        add(new JScrollPane(tree));

        setVisible(true);
    }

    private void addFiles(DefaultMutableTreeNode parentNode, File parentFile) {
        if (!parentFile.isDirectory()) return; // Exit if it's not a directory

        File[] files = parentFile.listFiles();
        if (files != null) {
            for (File file : files) {
                DefaultMutableTreeNode node = new DefaultMutableTreeNode(file.getName());
                parentNode.add(node);
                if (file.isDirectory()) {
                    addFiles(node, file); // Recursively add files and directories
                }
            }
        }
    }

}
