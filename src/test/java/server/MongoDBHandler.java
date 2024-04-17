package server;

import com.mongodb.client.*;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Projections;
import com.mongodb.client.model.Updates;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.json.simple.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class MongoDBHandler {
    private MongoClient mongoClient;

    public MongoDBHandler() {
        mongoClient = MongoClients.create("mongodb://localhost:27017");
    }

    public void insertDocument(String databaseName, String collectionName, Document document) {
        MongoDatabase database = mongoClient.getDatabase(databaseName);
        MongoCollection<Document> collection = database.getCollection(collectionName);

        Document existingDocument = collection.find(new Document("_id", document.get("_id"))).first();
        if (existingDocument != null) {
            System.out.println("Cannot insert row: Document with the same primary key already exists.");
            return;
        }

        collection.insertOne(document);
    }

    public void insertDocument(String databaseName, String collectionName, JSONObject jsonObject) {
        Document document = Document.parse(jsonObject.toString());
        insertDocument(databaseName, collectionName, document);
    }

    public Document getDocumentByIndex(String databaseName, String collectionName, String indexKey, String indexValue) {
        MongoDatabase database = mongoClient.getDatabase(databaseName);
        MongoCollection<Document> collection = database.getCollection(collectionName);

        Bson filter = Filters.eq(indexKey, indexValue);

        Document document = collection.find(filter).first();

        return document;
    }



    public void updateDocument(String databaseName, String collectionName, String primaryKey, String primaryKeyValue, String fieldToUpdate, String newValue) {
        MongoDatabase database = mongoClient.getDatabase(databaseName);
        MongoCollection<Document> collection = database.getCollection(collectionName);

        Bson filter = Filters.eq(primaryKey, primaryKeyValue);

        Bson updateOperation = Updates.set(fieldToUpdate, newValue);

        collection.updateOne(filter, updateOperation);
    }
    public long deleteDocumentByPK(String databaseName, String collectionName, String primaryKeyValue) {
        MongoDatabase database = mongoClient.getDatabase(databaseName);
        MongoCollection<Document> collection = database.getCollection(collectionName);
        Document query = new Document("_id", primaryKeyValue);
        return collection.deleteOne(query).getDeletedCount();
    }

    public List<String[]> fetchRows(String databaseName, String collectionName, String[] attributeNames) {
        List<String[]> rows = new ArrayList<>();
        MongoDatabase database = mongoClient.getDatabase(databaseName);
        MongoCollection<Document> collection = database.getCollection(collectionName);

        FindIterable<Document> documents = collection.find();
        for (Document document : documents) {
            String[] rowData = new String[attributeNames.length];

            Object primaryKeyValue = document.get("_id");
            if (primaryKeyValue != null) {
                rowData[0] = primaryKeyValue.toString();
            } else {
                System.err.println("Primary key (_id) not found in the document.");
                continue;
            }

            String ertek = document.getString("ertek");
            if (ertek != null && !ertek.isEmpty()) {
                String[] attributeValues = ertek.split(";");
                if (attributeValues.length == attributeNames.length - 1) {
                    for (int i = 0; i < attributeValues.length; i++) {
                        rowData[i + 1] = attributeValues[i];
                    }
                } else {
                    System.err.println("Number of attribute values does not match the number of attribute names.");
                    continue;
                }
            } else {
                System.err.println("The 'ertek' field is empty or null.");
                continue;
            }
            rows.add(rowData);
        }

        return rows;
    }

    public List<Document> fetchDocuments(String databaseName, String collectionName) {
        List<Document> documents = new ArrayList<>();
        MongoDatabase database = mongoClient.getDatabase(databaseName);
        MongoCollection<Document> collection = database.getCollection(collectionName);

        MongoCursor<Document> cursor = collection.find().iterator();
        try {
            while (cursor.hasNext()) {
                documents.add(cursor.next());
            }
        } finally {
            cursor.close();
        }

        return documents;
    }

    public void close() {
        mongoClient.close();
    }
}

