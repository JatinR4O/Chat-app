Quickstart — Java Chat App (paste into VS Code README)
===============================================

Required extensions (recommended)
- Extension Pack for Java (by Microsoft)
- Language Support for Java(TM) by Red Hat
- Debugger for Java (by Microsoft)

Prerequisites
- JDK 11+ installed (JDK 17 or 21 recommended). Ensure JAVA_HOME and PATH are set.
- (Optional) MySQL server + MySQL JDBC driver if you want DB logging.

Quick run (PowerShell)
1) Compile sources:

```powershell
cd 'D:\App projects\Chat App\java-chat-app'
javac *.java
```

2) Start server (console):

```powershell
java Server
```

Or start the server GUI (prompts for server name):

```powershell
java ServerGUI
```

3) Start a client (each in its own window):

```powershell
java ChatClient
```

Optional: run the headless test client:

```powershell
java TestClient
```

Enable MySQL logging (optional)
- Edit `DBHelper.java` and set DB_URL, DB_USER, DB_PASSWORD.
- Ensure MySQL JDBC driver jar is on the server classpath when starting the server:

```powershell
java -cp ".;C:\path\to\mysql-connector-java.jar" Server
```

Troubleshooting
- If clients can't connect, confirm the server log shows "[Server]: Listening on port 12345".
- If you see duplicate messages, ensure each client has only one active connection.

Enjoy — open a client on two machines or two windows and start chatting.
