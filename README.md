Java Swing TCP Chat Application

Overview
--------
This is a simple client-server chat application written in plain Java (no external frameworks). It uses TCP sockets and a multi-threaded server. The client GUI uses Java Swing.

Components
----------
- Server.java         - TCP server that accepts multiple clients and broadcasts messages.
- ClientHandler.java  - Per-client handler running on its own thread.
- ClientGUI.java      - Java Swing GUI for chat clients.
- ChatClient.java     - Small launcher that starts the GUI.
- DBHelper.java       - Optional MySQL helper to save chat history (disabled if config is empty).
- sql/create_table.sql - SQL to create the chat_history table.

Requirements
------------
- Java 8 or newer
- Optional: MySQL and MySQL JDBC driver on classpath (for DB logging)

How it works
------------
1. Start the server first:
   - java Server [port]
   - Default port is 12345
2. Start one or more clients:
   - java ChatClient
   - Fill username, server host (default localhost) and port (default 12345). Click Connect.
3. Type messages and click Send. Messages are broadcast to all connected clients.

Compilation (simple)
--------------------
From the project folder run:

    javac *.java

Then start the server:

    java Server

And start clients (each in its own terminal / run):

    java ChatClient

MySQL (optional)
----------------
If you want server to save chat history to MySQL:
1. Install MySQL and create a database (e.g., chatdb).
2. Run the SQL in `sql/create_table.sql` to create the `chat_history` table.
3. Edit `DBHelper.java` and set DB_URL, DB_USER, DB_PASSWORD appropriately.
4. Ensure MySQL JDBC driver (mysql-connector-java) is on the classpath when running the server:

    java -cp ".;path/to/mysql-connector-java.jar" Server

Notes
-----
- The protocol is simple: client sends the username as the first line after connecting. After that every line is a message.
- The server broadcasts messages as: [username]: message
- Console logs include tags like [Server] and [Client].

Enjoy!
