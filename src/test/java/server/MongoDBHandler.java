package server;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;

public class MongoDBHandler {
    private MongoClient mongoClient;

    public MongoDBHandler() {
        mongoClient = MongoClients.create("mongodb://localhost:27017");
    }

    public void insertDocument(String databaseName, String collectionName, Document document) {
        MongoDatabase database = mongoClient.getDatabase(databaseName);
        MongoCollection<Document> collection = database.getCollection(collectionName);
        collection.insertOne(document);
    }

    public long deleteDocumentByPK(String databaseName, String collectionName, String primaryKeyValue) {
        MongoDatabase database = mongoClient.getDatabase(databaseName);
        MongoCollection<Document> collection = database.getCollection(collectionName);
        Document query = new Document("_id", primaryKeyValue);
        return collection.deleteOne(query).getDeletedCount();
    }

    public void close() {
        mongoClient.close();
    }
}

