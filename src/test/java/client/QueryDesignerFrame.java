package client;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

public class QueryDesignerFrame extends JFrame {
    private JPanel panel, outputPanel;
    private JTextArea jTextArea;
    private List<String> checkBoxes;
    private String currentdb;
    private JButton copy;
    private Map<String, List<String>> selectedItems;

    public QueryDesignerFrame(List<String> jCheckBoxes, String currentdatabase) {
        this.checkBoxes = jCheckBoxes;
        this.currentdb = currentdatabase;
        panel = new JPanel();
        panel.setBackground(new Color(239, 240, 243));
        this.setLayout(new BorderLayout());

        int padding = 10;
        Insets insets = new Insets(padding, padding, padding, padding);

        projectTables(checkBoxes, panel);

        jTextArea = new JTextArea();
        jTextArea.setFont(new Font("Cfont", Font.PLAIN, 20));
        jTextArea.setPreferredSize(new Dimension(900, 200));
        jTextArea.setEditable(false);
        jTextArea.setBorder(new EmptyBorder(insets));
        jTextArea.setBackground(new Color(239, 240, 243));

        copy = new JButton("Copy");
        copy.setPreferredSize(new Dimension(70, 200));
        copy.setBackground(new Color(239, 240, 243));

        copy.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                copy(jTextArea.getText());
            }
        });

        JPanel textPanel = new JPanel();
        textPanel.setBackground(new Color(75, 104, 178));
        textPanel.add(jTextArea);
        textPanel.add(copy);

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBorder(new EmptyBorder(insets));
        mainPanel.add(panel, BorderLayout.CENTER);
        mainPanel.add(textPanel, BorderLayout.SOUTH);
        mainPanel.setBackground(new Color(75, 104, 178));

        this.add(mainPanel);

        this.add(textPanel, BorderLayout.SOUTH);

        this.setSize(1000, 800);
        this.setLocationRelativeTo(null);
        Image icon = Toolkit.getDefaultToolkit().getImage("src/main/resources/qdic.png");
        setIconImage(icon);
        this.setVisible(true);
        this.setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        this.selectedItems = new HashMap<>();
    }

    public static void copy(String text) {
        Clipboard clipboard = getSystemClipboard();
        clipboard.setContents(new StringSelection(text), null);
    }

    private static Clipboard getSystemClipboard() {
        Toolkit defaultToolkit = Toolkit.getDefaultToolkit();
        return defaultToolkit.getSystemClipboard();
    }

    private void projectTables(List<String> checkBoxes, JPanel panel) {
        JSONObject jsontabla = null;
        int aux;
        for (int i = 0; i < checkBoxes.size(); i++) {
            jsontabla = readTableFormat(currentdb, checkBoxes.get(i));
            if (jsontabla == null) {
                System.out.println("> Could not load table: " + checkBoxes.get(i));
                continue;
            }
            JSONArray jsonArrayTable = (JSONArray) jsontabla.get("attributes");

            DefaultTableModel model = new DefaultTableModel(null, new Object[]{checkBoxes.get(i), "Select"}) {
                @Override
                public boolean isCellEditable(int row, int column) {
                    return column == 1;
                }

                @Override
                public Class<?> getColumnClass(int columnIndex) {
                    if (columnIndex == 1) {
                        return Boolean.class;
                    }
                    return super.getColumnClass(columnIndex);
                }
            };

            aux = 0;
            for (Object o : jsonArrayTable) {
                JSONObject attribut = (JSONObject) o;
                String attributName = (String) attribut.get("name");
                model.addRow(new Object[]{attributName, false});
                aux++;
            }

            JTable table = new JTable(model);
            JScrollPane scrollPane = new JScrollPane(table);
            table.setFillsViewportHeight(true);
            table.setPreferredSize(new Dimension(100, aux * 20));
            table.setLocation(i * 50, i * 50);
            table.setPreferredScrollableViewportSize(table.getPreferredSize());
            panel.add(scrollPane);

            int finalI = i;
            model.addTableModelListener(new TableModelListener() {
                @Override
                public void tableChanged(TableModelEvent e) {
                    if (e.getType() == TableModelEvent.UPDATE && e.getColumn() == 1) {
                        for (int row = 0; row < model.getRowCount(); row++) {
                            boolean selected = (Boolean) model.getValueAt(row, 1);
                            String itemName = (String) model.getValueAt(row, 0);
                            String key = checkBoxes.get(finalI);
                            if (selected) {
                                selectedItems.computeIfAbsent(key, _ -> new ArrayList<>());
                                if (!selectedItems.get(key).contains(itemName)) {
                                    selectedItems.get(key).add(itemName);
                                }
                            } else {
                                selectedItems.computeIfPresent(key, (_, v) -> {
                                    v.remove(itemName);
                                    return v.isEmpty() ? null : v;
                                });
                            }
                        }
                        updateTextArea();
                    }
                }
            });
        }
    }

    private void updateTextArea() {
        List<String> selectedColumns = selectedItems
                .values()
                .stream()
                .flatMap(List::stream)
                .collect(Collectors.toList());

        List<String> selectedTables = new ArrayList<>(selectedItems.keySet());

        String selectedColumnsString = String.join(", ", selectedColumns);
        String fromTableName = "";
        if(!selectedTables.isEmpty()){
            fromTableName = selectedTables.getFirst();
        }

        List<String> connections = getForeignKeyConnections("src/test/java/databases/" + currentdb + ".json", currentdb, selectedTables, fromTableName);

        StringBuilder queryBuilder = new StringBuilder();

        queryBuilder.append("SELECT ").append(selectedColumnsString).append(" FROM ").append(fromTableName);

        for (String connection : connections) {
            queryBuilder.append("\n").append(connection);
        }

        queryBuilder.append(';');

        jTextArea.setText(queryBuilder.toString());
    }

    public static JSONObject readTableFormat(String databaseName, String tableName) {
        JSONParser parser = new JSONParser();
        try {
            String filePath = "src/test/java/databases/" + databaseName + ".json";
            FileReader reader = new FileReader(filePath);
            Object obj = parser.parse(reader);

            JSONArray databaseArray = (JSONArray) obj;

            for (Object databaseObj : databaseArray) {
                JSONObject database = (JSONObject) databaseObj;
                String dbName = (String) database.get("database_name");
                if (dbName.equals(databaseName)) {
                    JSONArray tables = (JSONArray) database.get("tables");
                    for (Object tableObj : tables) {
                        JSONObject table = (JSONObject) tableObj;
                        String tableNameInJson = (String) table.get("table_name");
                        if (tableNameInJson.equals(tableName)) {
                            return table;
                        }
                    }
                }
            }
            return null;
        } catch (IOException | ParseException e) {
            return null;
        }
    }

    public static List<String> getForeignKeyConnections(String filePath, String databaseName, List<String> selectedTables, String fromTableName) {
        JSONParser parser = new JSONParser();
        List<String> connections = new ArrayList<>();

        try {
            FileReader reader = new FileReader(filePath);
            JSONArray databasesArray = (JSONArray) parser.parse(reader);

            for (Object dbObj : databasesArray) {
                JSONObject database = (JSONObject) dbObj;
                String dbName = (String) database.get("database_name");

                if (dbName.equals(databaseName)) {
                    JSONArray tables = (JSONArray) database.get("tables");

                    for (Object tableObj : tables) {
                        JSONObject table = (JSONObject) tableObj;
                        String tableName = (String) table.get("table_name");

                        if (selectedTables.contains(tableName)) {
                            JSONArray attributes = (JSONArray) table.get("attributes");

                            for (Object attributeObj : attributes) {
                                JSONObject attribute = (JSONObject) attributeObj;
                                String attributeName = (String) attribute.get("name");
                                JSONArray isReferencedByFk = (JSONArray) attribute.get("is_referenced_by_fk");

                                for (Object refObj : isReferencedByFk) {
                                    JSONObject ref = (JSONObject) refObj;
                                    String referencedTableName = (String) ref.get("fkTableName");
                                    String referencedAttributeName = (String) ref.get("fkName");

                                    if (selectedTables.contains(referencedTableName)) {
                                        Iterator<String> tableIterator = selectedTables.iterator();
                                        while (tableIterator.hasNext()) {
                                            String nextTableName = tableIterator.next();
                                            if (!nextTableName.equals(fromTableName)) {
                                                String joinClause = "INNER JOIN " + tableName + " ON " + tableName + "." + attributeName + " = " + referencedTableName + "." + referencedAttributeName;
                                                connections.add(joinClause);
                                                break;
                                            }
                                        }
                                    }

                                }
                            }
                        }
                    }
                    return connections;
                }
            }

            connections.add("Database '" + databaseName + "' not found.");

        } catch (IOException | ParseException e) {
            e.printStackTrace();
        }

        return connections;
    }
}
