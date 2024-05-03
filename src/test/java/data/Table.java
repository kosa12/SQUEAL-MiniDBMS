package data;

import org.bson.Document;

import java.util.ArrayList;
import java.util.List;

import com.mongodb.client.MongoCollection;

public class Table {
    private final String tableName;
    private final ArrayList<Attribute> attributes;
    private final List<String> pkList;
    private String pkName;

    public Table(String tableName, String pKAttrName, MongoCollection<Document> collection) {
        this.tableName = tableName;
        attributes = new ArrayList<>();
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
        this.pkName = pKAttrName;
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


    @Override
    public String toString() {
        return tableName;
    }
}