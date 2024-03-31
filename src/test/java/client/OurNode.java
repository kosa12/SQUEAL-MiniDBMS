package client;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class OurNode extends DefaultMutableTreeNode {
    public OurNode(Object userObject) {
        super(userObject);
    }

    public static class OurMouseListener extends MouseAdapter {
        @Override
        public void mouseClicked(MouseEvent e) {
            if (SwingUtilities.isRightMouseButton(e)) {
                JTree tree = (JTree) e.getComponent();
                int row = tree.getRowForLocation(e.getX(), e.getY());
                TreePath path = tree.getPathForRow(row);
                if (path != null) {
                    OurNode node = (OurNode) path.getLastPathComponent();
                    showPopupMenu(e.getComponent(), e.getX(), e.getY());
                }
            }
        }

        private void showPopupMenu(Component component, int x, int y) {
            JPopupMenu popupMenu = new JPopupMenu();
            JMenuItem menuItem = new JMenuItem("Visual Editor");
            menuItem.addActionListener(e -> {
                System.out.println("Visual Editor - kivalasztva");

            });
            popupMenu.add(menuItem);
            popupMenu.show(component, x, y);
        }
    }

}
