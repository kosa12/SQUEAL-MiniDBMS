package data;
public class Attribute {
    private String attributeName;
    private String type;
    private boolean isnull;

    private boolean ispk;

    public Attribute(String attributeName, String type, boolean isnull, boolean ispk) {
        this.attributeName = attributeName;
        this.type = type;
        this.isnull = isnull;
        this.ispk = ispk;
    }

    public boolean isIspk() {
        return ispk;
    }

    public void setIspk(boolean ispk) {
        this.ispk = ispk;
    }

    public String getAttributeName() {
        return attributeName;
    }


    public void setAttributeName(String attributeName) {
        this.attributeName = attributeName;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public boolean isIsnull() {
        return isnull;
    }

    public void setIsnull(boolean isnull) {
        this.isnull = isnull;
    }

}