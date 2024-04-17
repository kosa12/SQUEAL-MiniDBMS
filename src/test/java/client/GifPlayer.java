package client;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class GifPlayer extends JFrame{
    private JPanel panel;
    private JLabel giflabel;

    public GifPlayer(){
        panel = new JPanel();
        this.add(panel);

        ImageIcon gif = new ImageIcon("src/main/resources/introgif.gif");
        giflabel = new JLabel(gif);
        panel.add(giflabel);

        Timer timer = new Timer(2000, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });
        timer.setRepeats(false); // Csak egyszer fusson le

        timer.start();

        Image icon = Toolkit.getDefaultToolkit().getImage("src/main/resources/dbicon.png");
        setIconImage(icon);

        this.setUndecorated(true);
        this.setSize(865, 650);
        this.setResizable(false);

        this.setLocationRelativeTo(null);

        this.setVisible(true);
    }


    public static void main(String[] args) {
        new GifPlayer();
    }
}
