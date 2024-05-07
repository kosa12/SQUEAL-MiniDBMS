package client;

import com.mongodb.client.*;
import data.Database;
import data.Table;
import org.bson.Document;

import javax.swing.*;
import java.awt.*;

public class QueryDesignerFrame extends JFrame {
    private JPanel panel;

    public QueryDesignerFrame() {
        panel = new JPanel();
        panel.setPreferredSize(new Dimension(300, 500));
        panel.setBackground(Color.RED);

        panel.setLayout(new FlowLayout());

        this.add(panel);
        this.setSize(1000, 800);
        this.setLocationRelativeTo(null);
        Image icon = Toolkit.getDefaultToolkit().getImage("src/main/resources/qdic.png");
        setIconImage(icon);
        this.setVisible(true);
        this.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
    }

    private void projectTables(String databaseName) {

        MongoClient mongoClient = MongoClients.create("mongodb://localhost:27017");
        MongoDatabase mongoDatabase = mongoClient.getDatabase(databaseName);

        for (String tableName : mongoDatabase.listCollectionNames()) {
            System.out.println(tableName);
        }

        mongoClient.close();
    }

}