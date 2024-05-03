package data;

import java.util.ArrayList;
import java.util.Iterator;
import org.json.simple.JSONObject;

public class Database {
    private String dataBaseName;
    private final ArrayList<Table> tables;

    public Database(String dataBaseName) {
        this.dataBaseName = dataBaseName;
        this.tables = new ArrayList<>();
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

    public boolean hasTable(String tableName) {
        for (Table table : tables) {
            if (table.getTableName().equals(tableName)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String toString() {
        return dataBaseName;
    }

    public ArrayList<Table> getTables() {
        return tables;
    }

    public void setReferencingInfo(String tableName, JSONObject referencingInfo) {
        Table table = getTable(tableName);
        if (table != null) {
            table.setReferencingInfo(referencingInfo);
        }
    }
}
