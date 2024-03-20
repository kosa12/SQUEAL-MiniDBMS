package client;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import java.io.File;

public class FileExplorer extends JFrame {
    private JTree tree;

    public FileExplorer() {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("Root");

        // Set the root directory you want to explore
        File rootDirectory = new File("D:\\Egyetem\\IV\\Adatbazisok2\\SQUEL\\SQUEL_maven\\src\\test\\java\\databases");
        addFiles(root, rootDirectory);

        tree = new JTree(root);
        add(new JScrollPane(tree));

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setTitle("File Explorer");
        pack();
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

    public static void main(String[] args) {
        SwingUtilities.invokeLater(FileExplorer::new);
    }
}
