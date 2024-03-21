package data;

import java.util.ArrayList;

public class Database {
    private String dataBaseName;
    private final ArrayList<Table> tables;

    public Database(String dataBaseName) {
        this.dataBaseName = dataBaseName;
        this.tables = new ArrayList<>();
    }

    public String getDataBaseName() {
        return dataBaseName;
    }

    public void setDataBaseName(String dataBaseName) {
        this.dataBaseName = dataBaseName;
    }

    public ArrayList<Table> getTables() {
        return tables;
    }

    public void addTable(Table table) {
        tables.add(table);
    }

    @Override
    public String toString() {
        return dataBaseName;
    }
}
