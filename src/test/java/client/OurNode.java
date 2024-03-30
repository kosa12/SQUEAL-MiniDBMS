package client;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

public class OurNode extends DefaultMutableTreeNode {
    public OurNode(Object userObject) {
        super(userObject);

    }

    public static class OurMouseListener implements MouseListener {
        @Override
        public void mouseClicked(MouseEvent e) {

            if (SwingUtilities.isRightMouseButton(e)) {

                OurNode node = (OurNode) e.getSource();
                System.out.println("DSADSADSA");
                if (node != null) {
                    showPopupMenu(e.getComponent(), e.getX(), e.getY());
                }
            }
        }

        @Override
        public void mousePressed(MouseEvent e) {

        }

        @Override
        public void mouseReleased(MouseEvent e) {

        }

        @Override
        public void mouseEntered(MouseEvent e) {

        }

        @Override
        public void mouseExited(MouseEvent e) {

        }

        private void showPopupMenu(Component component, int x, int y) {
            JPopupMenu popupMenu = new JPopupMenu();
            JMenuItem menuItem = new JMenuItem("Menüpont 1");
            menuItem.addActionListener(e -> {
                System.out.println("DSADSADSA");
            });
            popupMenu.add(menuItem);
            popupMenu.show(component, x, y);
        }

}}
