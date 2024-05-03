package data;

import org.json.simple.JSONArray;

public class Attribute {
    private String attributeName;
    private String type;
    private boolean isnull;

    private boolean ispk;

    public Attribute(String attributeName, String type, boolean isnull, boolean ispk, boolean isfk, JSONArray fkKeys) {
        this.attributeName = attributeName;
        this.type = type;
        this.isnull = isnull;
        this.ispk = ispk;
    }

    public String getAttributeName() {
        return attributeName;
    }

    public String getType() {
        return type;
    }

}