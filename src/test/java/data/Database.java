package data;

import java.util.ArrayList;
import java.util.Iterator;

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

    public void dropTable(String tableName) {
        Iterator<Table> iterator = tables.iterator();
        while (iterator.hasNext()) {
            Table table = iterator.next();
            if (table.getTableName().equals(tableName)) {
                iterator.remove();
                break;
            }
        }
    }

    public Table getTable(String tableName) {
        for (Table table : tables) {
            if (table.getTableName().equals(tableName)) {
                return table;
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return dataBaseName;
    }

}
