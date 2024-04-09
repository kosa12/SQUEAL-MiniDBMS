# Database Management System (DBMS)

The Database Management System (DBMS) project is a Java-based application that provides essential functionalities for managing databases and tables in MongoDB. It allows users to view, insert, update, and delete data in MongoDB collections using a user-friendly interface.


## Key Features

1. **Database Operations:**
   - **Create Database:** Users can create new databases.
   - **Drop Database:** Existing databases can be deleted.

2. **Table Operations:**
   - **Create Table:** Tables can be created within databases, with customizable attributes and primary keys.
   - **Drop Table:** Users can delete tables from databases.
   - **Insert Data:** Data can be inserted into tables, populating them with records.
   - **Delete Data:** Users can delete specific records or entire rows from tables.

3. **Indexing:**
   - **Create Index:** Indexes can be created on tables to improve query performance.
  
4.  **Visual editor:**
    - **Insert Data:** Data can be inserted with a visual editor into the MongoDB collection.
    - **Delete Data:** The user can delete the data from the MongoDB via selecting the row.
  
## How to Use

1. **Installation:** Clone the repository to your local machine.
   ```bash
   git clone https://github.com/kosa12/MiniDBMS
   ```
3. **Run the Application:** Start the application by running the main Java program. This will launch the server and the GUI.
4. **Connect to Database:** Connect to the MongoDB database using the provided interface in the GUI.
5. **Visual Editor:** Use the visual editor to insert, delete, or update data records in the database.
6. **Manual Input:** Alternatively, users can input specific commands to perform CRUD operations directly.
7. **Interact with GUI:** Explore the various features of the GUI, including data visualization, query execution, and database management.

  
## Technologies Used

1. **Java:** The core programming language used for developing the application logic and server-side functionality.
2. **MongoDB:** A NoSQL database used for storing and managing the structured data.
3. **JSON:** Used for data interchange between the Java application and the MongoDB database.
4. **Socket Programming:** Implemented for client-server communication, allowing multiple clients to interact with the server concurrently.
5. **Swing:** Java's GUI toolkit utilized for building the graphical user interface, providing components such as buttons, text fields, and tables.

## Contributors
- [Dacz Krisztian](https://github.com/dKriszti15) - FrontEnd
- [Kosa Matyas](https://github.com/kosa12) - BackEnd

## License

This project is licensed under the MIT License
