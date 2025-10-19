import java.io.*;
import java.net.*;

public class ClientHandler implements Runnable {
    private Socket socket;
    private Server server;
    private BufferedReader in;
    private PrintWriter out;
    private String username = "Unknown";

    public ClientHandler(Socket socket, Server server) {
        this.socket = socket;
        this.server = server;
        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);
        } catch (IOException e) {
            System.err.println("[ClientHandler]: Error setting up streams - " + e.getMessage());
            close();
        }
    }

    public String getUsername() {
        return username;
    }

    public void sendMessage(String message) {
        out.println(message);
    }

    @Override
    public void run() {
        try {
            // Protocol: first line from client is the username
            String nameLine = in.readLine();
            if (nameLine != null && !nameLine.trim().isEmpty()) {
                username = nameLine.trim();
            }
            System.out.println("[Server]: " + username + " joined");
            // send recent history to this client before broadcasting join
            server.sendHistoryTo(this);
            server.broadcast("[Server]: " + username + " has joined the chat", this);

            String line;
            while ((line = in.readLine()) != null) {
                String formatted = "[" + username + "]: " + line;
                System.out.println("[Server]: Received - " + formatted);
                server.broadcast(formatted, this);
            }
        } catch (IOException e) {
            System.err.println("[ClientHandler]: Connection error for " + username + " - " + e.getMessage());
        } finally {
            close();
            server.removeClient(this);
            server.broadcast("[Server]: " + username + " has left the chat", this);
        }
    }

    private void close() {
        try { if (in != null) in.close(); } catch (IOException ignored) {}
        if (out != null) out.close();
        try { if (socket != null && !socket.isClosed()) socket.close(); } catch (IOException ignored) {}
    }
}
