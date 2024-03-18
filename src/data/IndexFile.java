package data;

import java.util.ArrayList;

public class IndexFile {
    private String indexName;
    private int keyLength;
    private boolean isUnique;
    private String indexType;
    private ArrayList<String> attributes;

    public String getIndexName() {
        return indexName;
    }

    public void setIndexName(String indexName) {
        this.indexName = indexName;
    }

    public int getKeyLength() {
        return keyLength;
    }

    public void setKeyLength(int keyLength) {
        this.keyLength = keyLength;
    }

    public boolean isUnique() {
        return isUnique;
    }

    public void setUnique(boolean unique) {
        isUnique = unique;
    }

    public String getIndexType() {
        return indexType;
    }

    public void setIndexType(String indexType) {
        this.indexType = indexType;
    }

    public ArrayList<String> getAttributes() {
        return attributes;
    }

    public void addAttributes(String attribute) {
        attributes.add(attribute);
    }
}