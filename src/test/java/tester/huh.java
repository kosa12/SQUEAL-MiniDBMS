package tester;

import org.bson.Document;
import server.MongoDBHandler;

import java.util.ArrayList;
import java.util.List;

public class huh {
    public static void main(String[] args) {
        MongoDBHandler mongoDBHandler = new MongoDBHandler();
        List<Document> documents = new ArrayList<>();

        for (int i = 0; i < 100000; i++) {
            Document doc = new Document("_id", i)
                    .append("name", "Document " + i)
                    .append("value", i * 10);
            documents.add(doc);
        }

        //mongoDBHandler.insertDocument("yourDatabaseName", "yourCollectionName", documents);

        mongoDBHandler.close();
    }

}
