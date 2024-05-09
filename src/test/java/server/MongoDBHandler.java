package server;

import com.mongodb.client.*;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.ArrayList;
import java.util.List;

import static server.Server.getIsItEnd;

public class MongoDBHandler {
    private static MongoClient mongoClient = null;
    private static final List<Document> documentList = new ArrayList<>();
    private static final Object lock = new Object();
    private static String databaseName1;
    private static String collectionName1;


    public MongoDBHandler() {
        mongoClient = MongoClients.create("mongodb://localhost:27017");
    }

    public void insertDocument(String databaseName, String collectionName, Document document) {

        /*
         	Isten, áldd meg a magyart
            Jó kedvvel, bőséggel,
            Nyújts feléje védő kart,
            Ha küzd ellenséggel;
            Bal sors akit régen tép,
            Hozz rá víg esztendőt,
            Megbünhödte már e nép
            A multat s jövendőt!
        */
        MongoDatabase database = mongoClient.getDatabase(databaseName);
        MongoCollection<Document> collection = database.getCollection(collectionName);
        Document existingDocument = collection.find(new Document("_id", document.get("_id"))).first();
        if (existingDocument != null) {
            return;
        }

        synchronized (lock) {
            int BATCH_SIZE = 1000;
            if (documentList.size() < BATCH_SIZE) {
                documentList.add(document);
                databaseName1 = databaseName;
                collectionName1 = collectionName;
            } else if (documentList.size() == BATCH_SIZE || getIsItEnd()) {
                documentList.add(document);
                insertDocumentsIntoMongoDB(databaseName, collectionName, new ArrayList<>(documentList));
                documentList.clear();
            }
        }

    }

    public static void insertDocumentsIntoMongoDBALL() {
        if(!documentList.isEmpty()){
            MongoDatabase database = mongoClient.getDatabase(databaseName1);
            MongoCollection<Document> collection = database.getCollection(collectionName1);
            collection.insertMany(documentList);
            documentList.clear();
        }

    }


    private void insertDocumentsIntoMongoDB(String databaseName, String collectionName, List<Document> documents) {
        MongoDatabase database = mongoClient.getDatabase(databaseName);
        MongoCollection<Document> collection = database.getCollection(collectionName);
        collection.insertMany(documents);
    }

    public void insertDocumentINDEX(String databaseName, String collectionName, Document document) {



        MongoDatabase database = mongoClient.getDatabase(databaseName);
        MongoCollection<Document> collection = database.getCollection(collectionName);

        Document existingDocument = collection.find(new Document("_id", document.get("_id"))).first();
        if (existingDocument != null) {
            System.out.println("Cannot insert row: Document with the same primary key already exists.");
            return;
        }

        collection.insertOne(document);
    }

    public void dropCollection(String databaseName) {
        MongoDatabase database = mongoClient.getDatabase(databaseName);
        database.drop();
    }


    public List<String> getAllCollections(String databaseName) {
        List<String> collections = new ArrayList<>();
        MongoIterable<String> iterable = mongoClient.getDatabase(databaseName).listCollectionNames();
        for (String collection : iterable) {
            collections.add(collection);
        }
        return collections;
    }

    public boolean indexExists(String databaseName, String indexName) {
        MongoDatabase database = mongoClient.getDatabase(databaseName);
        for (String name : database.listCollectionNames()) {
            if (name.equals(indexName)) {
                return true;
            }
        }
        return false;
    }


    public Document getDocumentByIndex(String databaseName, String collectionName, String indexKey, String indexValue) {
        MongoDatabase database = mongoClient.getDatabase(databaseName);
        MongoCollection<Document> collection = database.getCollection(collectionName);

        Bson filter = Filters.eq(indexKey, indexValue);

        return collection.find(filter).first();
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

    public List<String[]> fetchRowsWithFilter(String databaseName, String collectionName, Bson filter) {
        List<String[]> rows = new ArrayList<>();
        MongoDatabase database = mongoClient.getDatabase(databaseName);
        MongoCollection<Document> collection = database.getCollection(collectionName);

        FindIterable<Document> documents = collection.find(filter);
        for (Document document : documents) {
            String[] rowData = convertDocumentToStringArray(document);
            rows.add(rowData);
        }

        return rows;
    }

    public List<String[]> fetchRowsWithFilterFromIndex(String databaseName, String collectionName, Bson filter) {
        List<String[]> rows = new ArrayList<>();
        MongoDatabase database = mongoClient.getDatabase(databaseName);
        MongoCollection<Document> collection = database.getCollection(collectionName);

        FindIterable<Document> documents = collection.find(filter);
        for (Document document : documents) {
            String[] rowData = convertDocumentToStringArray(document);
            rows.add(rowData);
        }

        return rows;
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

        try (MongoCursor<Document> cursor = collection.find().iterator()) {
            while (cursor.hasNext()) {
                documents.add(cursor.next());
            }
        }

        return documents;
    }

    public static List<String[]> fetchAllRows(String databaseName, String tableName) {
        MongoDatabase database = mongoClient.getDatabase(databaseName);
        if (database == null) {
            System.out.println("Database not found: " + databaseName);
            return null;
        }

        MongoCollection<Document> collection = database.getCollection(tableName);
        FindIterable<Document> documents = collection.find();
        List<String[]> rows = new ArrayList<>();

        try (MongoCursor<Document> cursor = documents.iterator()) {
            while (cursor.hasNext()) {
                Document doc = cursor.next();
                String[] row = convertDocumentToStringArray(doc);
                String[] secondElementParts = row[1].split(";");
                String[] combinedRow = new String[row.length + secondElementParts.length - 1];
                combinedRow[0] = row[0];
                System.arraycopy(secondElementParts, 0, combinedRow, 1, secondElementParts.length);
                rows.add(combinedRow);
            }
        }



        return rows;
    }

    private static String[] convertDocumentToStringArray(Document doc) {
        List<String> values = new ArrayList<>();
        for (String key : doc.keySet()) {
            values.add(doc.get(key).toString());
        }
        return values.toArray(new String[0]);
    }

    public void close() {
        mongoClient.close();
    }
}

