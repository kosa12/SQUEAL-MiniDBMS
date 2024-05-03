package data;

import org.bson.Document;
import org.json.simple.JSONObject;

import java.util.ArrayList;
import java.util.List;

import com.mongodb.client.MongoCollection;

public class Table {
    private String tableName;
    private final ArrayList<Attribute> attributes;
    private String pKAttrName;
    private final List<String> pkList;
    private String primaryKeyAttributeName;

    private MongoCollection<Document> collection;
    private JSONObject referencingInfo;

    public Table(String tableName, String pKAttrName, MongoCollection<Document> collection) {
        this.tableName = tableName;
        this.pKAttrName = pKAttrName;
        attributes = new ArrayList<>();
        this.collection = collection;
        this.pkList = new ArrayList<>();
    }

    public boolean hasPrimaryKeyValue(Object primaryKeyValue) {
        for (String row : pkList) {
            if (row.equals(primaryKeyValue)) {
                return true;
            }
        }
        return false;
    }

    public boolean removePrimaryKeyValue(Object primaryKeyValue) {
        return pkList.remove(primaryKeyValue.toString());
    }

    public void addPKtoList(String pkname){
        pkList.add(pkname);
    }

    public String getTableName() {
        return tableName;
    }

    public void setpKAttrName(String pKAttrName) {
        this.pKAttrName = pKAttrName;
    }

    public ArrayList<Attribute> getAttributes() {
        return attributes;
    }

    public void addAttribute(Attribute attribute) {
        attributes.add(attribute);
    }

    public boolean hasAttribute(String attributeName) {
        for (Attribute attribute : attributes) {
            if (attribute.getAttributeName().equals(attributeName)) {
                return true;
            }
        }
        return false;
    }

    public void setReferencingInfo(JSONObject referencingInfo) {
        this.referencingInfo = referencingInfo;
    }


    @Override
    public String toString() {
        return tableName;
    }
}