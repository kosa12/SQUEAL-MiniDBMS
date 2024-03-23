package data;

import java.util.ArrayList;

public class Table {
    private String tableName;
    private final ArrayList<Attribute> attributes;
    private String pKAttrName;
    private final ArrayList<ForeignKey> foreignKeys;
    private final ArrayList<IndexFile> indexFiles;

    public Table(String tableName, String pKAttrName) {
        this.tableName = tableName;
        this.pKAttrName = pKAttrName;
        attributes = new ArrayList<>();
        indexFiles = new ArrayList<>();
        foreignKeys = new ArrayList<>();
    }

    public Attribute getAttribute(String attributeName) {
        for (Attribute attribute : attributes) {
            if (attribute.getAttributeName().equals(attributeName)) {
                return attribute;
            }
        }
        return null;
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public String getpKAttrName() {
        return pKAttrName;
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

    public ArrayList<ForeignKey> getForeignKeys() {
        return foreignKeys;
    }

    public void addForeignKey(ForeignKey foreignKey) {
        foreignKeys.add(foreignKey);
    }

    public ArrayList<IndexFile> getIndexFiles() {
        return indexFiles;
    }

    public void addIndexFile(IndexFile indexFile) {
        indexFiles.add(indexFile);
    }


    @Override
    public String toString() {
        return tableName;
    }
}