import java.io.*;
import java.net.*;

public class TestClient {
    public static void main(String[] args) {
        String host = "localhost";
        int port = 12345;
        String username = "DiagClient";
        try (Socket s = new Socket(host, port);
             BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream()));
             PrintWriter out = new PrintWriter(s.getOutputStream(), true)) {
            System.out.println("[TestClient]: Connected to " + host + ":" + port);
            out.println(username);
            out.println("Hello from TestClient");
            // Read a few lines then exit
            long start = System.currentTimeMillis();
            while (System.currentTimeMillis() - start < 3000) {
                if (in.ready()) {
                    String line = in.readLine();
                    if (line == null) break;
                    System.out.println("[TestClient] RECV: " + line);
                }
                Thread.sleep(100);
            }
            System.out.println("[TestClient]: Done");
        } catch (Exception e) {
            System.err.println("[TestClient]: Error - " + e.getMessage());
            e.printStackTrace();
        }
    }
}
