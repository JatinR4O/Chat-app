import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.text.*;
import java.awt.*;
// ...existing code...
import java.io.*;
import java.net.*;

/**
 * Simple Swing client GUI. Connects to a server via TCP, sends username as first line,
 * then sends messages entered by the user. Receives broadcast messages and displays them.
 */
public class ClientGUI extends JFrame {
    private JTextArea messageArea;
    private JTextPane messagePane;
    private JTextField inputField;
    private JTextField usernameField;
    private JButton sendButton;
    private JButton connectButton;
    private JButton disconnectButton;
    private JButton viewHistoryButton;
    private JTextField hostField;
    private JTextField portField;

    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private Thread readerThread;
    private String clientName;

    public ClientGUI() {
        setTitle("Java Chat Client");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(600, 400);
        setLocationRelativeTo(null);
        // Use Nimbus look-and-feel if available for a modern UI
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception ignored) {}

        // Prompt for username at startup (force user to provide one)
        String username = JOptionPane.showInputDialog(this, "Enter username:", "Username", JOptionPane.PLAIN_MESSAGE);
        if (username == null || username.trim().isEmpty()) {
            // If user cancels or enters nothing, generate a small default
            username = "User" + (int)(Math.random() * 900 + 100);
        }
        initComponents();
        usernameField.setText(username);
        clientName = username;
    }

    private void initComponents() {
    messagePane = new JTextPane();
    messagePane.setEditable(false);
    messagePane.setBorder(new EmptyBorder(8,8,8,8));
    messagePane.setFont(new Font("Segoe UI", Font.PLAIN, 14));
    JScrollPane scrollPane = new JScrollPane(messagePane);

        inputField = new JTextField();
        inputField.addActionListener(e -> sendMessage());

    sendButton = new JButton("Send");
    sendButton.addActionListener(e -> sendMessage());
    sendButton.setEnabled(false); // enabled only when connected
    sendButton.setBackground(new Color(0x2D9CDB));
    sendButton.setForeground(Color.WHITE);

    usernameField = new JTextField(10);
    usernameField.setFont(new Font("Segoe UI", Font.PLAIN, 13));

    hostField = new JTextField("localhost", 10);
    portField = new JTextField("12345", 5);
    hostField.setFont(new Font("Segoe UI", Font.PLAIN, 13));
    portField.setFont(new Font("Segoe UI", Font.PLAIN, 13));

        connectButton = new JButton("Connect");
        connectButton.addActionListener(e -> connect());

    viewHistoryButton = new JButton("View History");
    viewHistoryButton.addActionListener(e -> showClientHistory());

        disconnectButton = new JButton("Disconnect");
        disconnectButton.addActionListener(e -> disconnect());
        disconnectButton.setEnabled(false);

    JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
    topPanel.setBorder(new EmptyBorder(8,8,8,8));
    JLabel userLabel = new JLabel("Username:"); userLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
    JLabel hostLabel = new JLabel("Host:"); hostLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
    JLabel portLabel = new JLabel("Port:"); portLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
    topPanel.add(userLabel);
    topPanel.add(usernameField);
    topPanel.add(hostLabel);
    topPanel.add(hostField);
    topPanel.add(portLabel);
    topPanel.add(portField);
    topPanel.add(connectButton);
    topPanel.add(disconnectButton);
    topPanel.add(viewHistoryButton);

    JPanel bottomPanel = new JPanel(new BorderLayout(6,6));
    bottomPanel.setBorder(new EmptyBorder(8,8,8,8));
    inputField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
    bottomPanel.add(inputField, BorderLayout.CENTER);
    bottomPanel.add(sendButton, BorderLayout.EAST);

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(topPanel, BorderLayout.NORTH);
        getContentPane().add(scrollPane, BorderLayout.CENTER);
        getContentPane().add(bottomPanel, BorderLayout.SOUTH);
    }

    private void connect() {
        String host = hostField.getText().trim();
        int port = Integer.parseInt(portField.getText().trim());
        String username = usernameField.getText().trim();
        if (username.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter a username");
            return;
        }

        try {
            socket = new Socket(host, port);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            // Send username first
            out.println(username);
            // prepare or load local history for this username
            ensureLocalHistory(username);
            appendMessage("[Client]: Connected to server as " + username);

            readerThread = new Thread(() -> {
                try {
                    String line;
                    while ((line = in.readLine()) != null) {
                        appendMessage(line);
                        // append to local history file
                        appendToLocalHistory(line);
                    }
                } catch (IOException e) {
                    appendMessage("[Client]: Connection lost - " + e.getMessage());
                } finally {
                    SwingUtilities.invokeLater(() -> {
                        disconnect();
                    });
                }
            });
            readerThread.start();
            connectButton.setEnabled(false);
            disconnectButton.setEnabled(true);
            sendButton.setEnabled(true);
            hostField.setEnabled(false);
            portField.setEnabled(false);
        } catch (IOException e) {
            appendMessage("[Client]: Could not connect - " + e.getMessage());
            closeSocket();
        }
    }

    private File localHistoryFile;

    private void ensureLocalHistory(String username) {
        try {
            localHistoryFile = new File(username + "_history.txt");
            if (!localHistoryFile.exists()) localHistoryFile.createNewFile();
            // load and display existing history
            try (BufferedReader br = new BufferedReader(new FileReader(localHistoryFile))) {
                String l;
                while ((l = br.readLine()) != null) {
                    appendMessage(l);
                }
            }
        } catch (IOException e) {
            appendMessage("[Client]: Could not prepare history file - " + e.getMessage());
        }
    }

    private void appendToLocalHistory(String line) {
        if (localHistoryFile == null) return;
        try (FileWriter fw = new FileWriter(localHistoryFile, true);
             BufferedWriter bw = new BufferedWriter(fw)) {
            bw.write(line);
            bw.newLine();
        } catch (IOException e) {
            // ignore, but log
            appendMessage("[Client]: Failed to write local history - " + e.getMessage());
        }
    }

    private void showClientHistory() {
        if (localHistoryFile == null) {
            JOptionPane.showMessageDialog(this, "No history file yet (connect first).", "History", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        if (!localHistoryFile.exists()) {
            JOptionPane.showMessageDialog(this, "No history found for this user.", "History", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        try {
            StringBuilder sb = new StringBuilder();
            try (BufferedReader br = new BufferedReader(new FileReader(localHistoryFile))) {
                String l;
                while ((l = br.readLine()) != null) sb.append(l).append('\n');
            }
            JTextArea area = new JTextArea(sb.toString());
            area.setEditable(false);
            area.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            JScrollPane sp = new JScrollPane(area);
            sp.setPreferredSize(new Dimension(600,400));
            JOptionPane.showMessageDialog(this, sp, "Chat History", JOptionPane.PLAIN_MESSAGE);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Failed to read history: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void disconnect() {
        appendMessage("[Client]: Disconnecting");
        closeSocket();
        connectButton.setEnabled(true);
        disconnectButton.setEnabled(false);
        sendButton.setEnabled(false);
        hostField.setEnabled(true);
        portField.setEnabled(true);
    }

    private void closeSocket() {
        try { if (out != null) out.close(); } catch (Exception ignored) {}
        try { if (in != null) in.close(); } catch (Exception ignored) {}
        try { if (socket != null && !socket.isClosed()) socket.close(); } catch (Exception ignored) {}
        out = null; in = null; socket = null;
        if (readerThread != null && readerThread.isAlive()) readerThread.interrupt();
    }

    private void sendMessage() {
        String text = inputField.getText().trim();
        if (text.isEmpty()) return;
        if (out == null) {
            appendMessage("[Client]: Not connected");
            return;
        }
        out.println(text);
        // do not locally append the sent message — server will broadcast it back
        inputField.setText("");
    }

    private void appendMessage(String msg) {
        SwingUtilities.invokeLater(() -> {
            try {
                StyledDocument doc = messagePane.getStyledDocument();
                Style defaultStyle = messagePane.getStyle(StyleContext.DEFAULT_STYLE);
                Style regular = doc.getStyle("regular");
                if (regular == null) {
                    regular = doc.addStyle("regular", defaultStyle);
                    StyleConstants.setFontFamily(regular, "Segoe UI");
                    StyleConstants.setFontSize(regular, 14);
                }

                // Create colored styles for server, self, and others
                Style serverStyle = doc.getStyle("server");
                if (serverStyle == null) {
                    serverStyle = doc.addStyle("server", regular);
                    StyleConstants.setForeground(serverStyle, UIStyles.COLOR_SERVER);
                    StyleConstants.setItalic(serverStyle, true);
                }
                Style selfStyle = doc.getStyle("self");
                if (selfStyle == null) {
                    selfStyle = doc.addStyle("self", regular);
                    StyleConstants.setForeground(selfStyle, UIStyles.COLOR_SELF);
                    StyleConstants.setBold(selfStyle, true);
                }
                Style otherStyle = doc.getStyle("other");
                if (otherStyle == null) {
                    otherStyle = doc.addStyle("other", regular);
                    StyleConstants.setForeground(otherStyle, UIStyles.COLOR_OTHER);
                }
                Style systemStyle = doc.getStyle("system");
                if (systemStyle == null) {
                    systemStyle = doc.addStyle("system", regular);
                    StyleConstants.setForeground(systemStyle, UIStyles.COLOR_SYSTEM);
                }

                // Decide style based on message prefix
                Style chosen = regular;
                if (msg.startsWith("[Client]:") || msg.startsWith("[Client]")) {
                    chosen = systemStyle;
                } else if (msg.startsWith("[")) {
                    int end = msg.indexOf("]");
                    if (end > 1) {
                        String name = msg.substring(1, end);
                        if (name.equals(clientName)) chosen = selfStyle;
                        else if (name.equalsIgnoreCase("Server")) chosen = serverStyle;
                        else chosen = otherStyle;
                    }
                }

                doc.insertString(doc.getLength(), msg + "\n", chosen);
                messagePane.setCaretPosition(doc.getLength());
            } catch (BadLocationException e) {
                // fallback
                messagePane.setText(messagePane.getText() + msg + "\n");
            }
        });
    }
}
