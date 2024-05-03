package client;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class OurNode extends DefaultMutableTreeNode {
    private final String currentDatabase;
    private final Client client;

    public OurNode(Object userObject, String currentDatabase, Client client) {
        super(userObject);
        this.currentDatabase = currentDatabase;
        this.client = client;
    }

    public static class OurMouseListener extends MouseAdapter {
        private final Client client;

        public OurMouseListener(Client client) {
            this.client = client;
        }

        @Override
        public void mouseClicked(MouseEvent e) {
            if (SwingUtilities.isRightMouseButton(e)) {
                JTree tree = (JTree) e.getComponent();
                int row = tree.getRowForLocation(e.getX(), e.getY());
                TreePath path = tree.getPathForRow(row);
                if (path != null) {
                    OurNode node = (OurNode) path.getLastPathComponent();
                    if(node.isLeaf()) {
                        showPopupMenu(e.getComponent(), e.getX(), e.getY(), node.toString(), node.getCurrentDatabase(), client);
                    }
                }
            }
        }

        private void showPopupMenu(Component component, int x, int y, String name, String currentDatabase, Client client) {
            JPopupMenu popupMenu = new JPopupMenu();
            JMenuItem menuItem = new JMenuItem("Visual Editor");
            menuItem.addActionListener(_ -> {
                System.out.println("Visual Editor - selected: " + name);
                System.out.println("Current Database: " + currentDatabase);

                VisualEditorFrame visualEditorFrame = new VisualEditorFrame(name, currentDatabase, client);
            });
            popupMenu.add(menuItem);
            popupMenu.show(component, x, y);
        }
    }

    public String getCurrentDatabase() {
        return currentDatabase;
    }
}
