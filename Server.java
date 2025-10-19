import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.io.*;

public class Server {
    private int port;
    private ServerSocket serverSocket;
    private ExecutorService pool = Executors.newCachedThreadPool();
    private Set<ClientHandler> clients = ConcurrentHashMap.newKeySet();
    // Optional DB helper (can be null or disabled)
    private DBHelper dbHelper;
    private List<MessageListener> listeners = new CopyOnWriteArrayList<>();
    // keep an in-memory history (recent messages)
    private Deque<String> history = new ConcurrentLinkedDeque<>();
    private final int HISTORY_LIMIT = 1000;
    private File historyFile;

    public Server(int port) {
        this.port = port;
        this.dbHelper = new DBHelper(); // will be inert if not configured
        // prepare history file in the working directory
        try {
            historyFile = new File("server_chat_history.txt");
            if (!historyFile.exists()) historyFile.createNewFile();
        } catch (IOException e) {
            System.err.println("[Server]: Could not create history file - " + e.getMessage());
        }
    }

    public void start() {
        try {
            serverSocket = new ServerSocket(port);
            logToListeners("[Server]: Listening on port " + port);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                logToListeners("[Server]: Client connected from " + clientSocket.getRemoteSocketAddress());
                ClientHandler handler = new ClientHandler(clientSocket, this);
                clients.add(handler);
                pool.execute(handler);
            }
        } catch (IOException e) {
            logToListeners("[Server]: Error - " + e.getMessage());
        } finally {
            stop();
        }
    }

    public void stop() {
        try {
            if (serverSocket != null && !serverSocket.isClosed()) serverSocket.close();
            pool.shutdownNow();
            System.out.println("[Server]: Stopped");
        } catch (IOException e) {
            System.err.println("[Server]: Error closing server - " + e.getMessage());
        }
    }

    public void broadcast(String message, ClientHandler from) {
        logToListeners("[Server]: Broadcasting - " + message);
        addToHistory(message);
        // Save to DB (optional)
        if (dbHelper != null && dbHelper.isConfigured()) {
            String username = from != null ? from.getUsername() : "Server";
            dbHelper.insertMessage(username, message);
        }

        // Send to all connected clients (including the sender) so everyone sees the same stream
        for (ClientHandler client : clients) {
            client.sendMessage(message);
        }
    }

    private void addToHistory(String message) {
        if (message == null) return;
        history.addLast(message);
        while (history.size() > HISTORY_LIMIT) history.pollFirst();
        // append to disk
        if (historyFile != null) {
            try (FileWriter fw = new FileWriter(historyFile, true);
                 BufferedWriter bw = new BufferedWriter(fw)) {
                bw.write(message);
                bw.newLine();
            } catch (IOException e) {
                System.err.println("[Server]: Failed to write history - " + e.getMessage());
            }
        }
    }

    public void sendHistoryTo(ClientHandler client) {
        // send the current history snapshot to a client
        for (String m : history) {
            client.sendMessage(m);
        }
    }

    // expose history file for GUIs
    public File getHistoryFile() {
        return historyFile;
    }

    /**
     * Allows GUIs to send server-originated messages into the chat stream.
     */
    public void sendFromServer(String serverName, String message) {
        String formatted = "[" + serverName + "]: " + message;
        broadcast(formatted, null);
    }

    public void addMessageListener(MessageListener l) {
        listeners.add(l);
    }

    public void removeMessageListener(MessageListener l) {
        listeners.remove(l);
    }

    private void logToListeners(String message) {
        // Print to console as well
        System.out.println(message);
        for (MessageListener l : listeners) {
            l.onMessage(message);
        }
    }

    public void removeClient(ClientHandler client) {
        clients.remove(client);
        System.out.println("[Server]: Client removed: " + client.getUsername());
    }

    public static void main(String[] args) {
        int port = 12345;
        if (args.length > 0) {
            try { port = Integer.parseInt(args[0]); } catch (NumberFormatException ignored) {}
        }
        Server server = new Server(port);
        server.start();
    }
}
