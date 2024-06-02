package tester;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Random;

public class TestWritingScript {
    public static void main(String[] args) {
        int numInserts = 100000;
        String tableName = "books";
        String filePath = "src/test/java/tester/scripts/insert_script_asd.txt";

        generateInsertScript(numInserts, tableName, filePath);
    }

    public static void generateInsertScript(int numInserts, String tableName, String filePath) {
        Random random = new Random();
        File file = new File(filePath);
        File parentDir = file.getParentFile();

        if (!parentDir.exists()) {
            parentDir.mkdirs();
        }
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
            for (int i = 0; i < numInserts; i++) {
                int id = i + 1;
                String name = "Name_" + id;
                int authorId = random.nextInt(1000) + 1;
                String insertStatement = "INSERT INTO " + tableName + " (books_bookid, books_title, authors_authorid) VALUES (" + id + ", '" + name + "', " + authorId +");";
                writer.write(insertStatement);
                writer.newLine();
            }
            System.out.println("Insert script generated successfully at: " + filePath);
        } catch (IOException e) {
            System.err.println("Error writing insert script: " + e.getMessage());
        }
    }
}
