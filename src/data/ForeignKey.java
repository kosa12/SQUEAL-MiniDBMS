package data;

public class ForeignKey {
    private String attrName;
    private String refTableName;
    private String refAttrName;

    public String getAttrName() {
        return attrName;
    }

    public void setAttrName(String attrName) {
        this.attrName = attrName;
    }

    public String getRefTableName() {
        return refTableName;
    }

    public void setRefTableName(String refTableName) {
        this.refTableName = refTableName;
    }

    public String getRefAttrName() {
        return refAttrName;
    }

    public void setRefAttrName(String refAttrName) {
        this.refAttrName = refAttrName;
    }
}