import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.text.*;
import java.io.*;
import java.awt.*;
import java.awt.event.*;

/**
 * Simple server GUI to start server, show logs, and send server-originated messages.
 * The server GUI will prompt for a username (server name) and display logs in a text area.
 */
public class ServerGUI extends JFrame implements MessageListener {
    // replaced JTextArea with JTextPane (logPane)
    private JTextField portField;
    private JButton startButton;
    private JButton stopButton;
    private JTextField serverNameField;
    private Server server;
    private JTextField serverInputField;
    private JButton serverSendButton;
    private JTextPane logPane;
    private JButton viewHistoryButton;

    public ServerGUI() {
        setTitle("Chat Server");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(600, 400);
        setLocationRelativeTo(null);
        // Use Nimbus look-and-feel if available
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception ignored) {}

        // Prompt for server name at startup
        String serverName = JOptionPane.showInputDialog(this, "Enter server name:", "Server Name", JOptionPane.PLAIN_MESSAGE);
        if (serverName == null || serverName.trim().isEmpty()) {
            serverName = "Server" + (int)(Math.random() * 900 + 100);
        }
        initComponents();
        serverNameField.setText(serverName);
    }

    private void initComponents() {
    logPane = new JTextPane();
    logPane.setEditable(false);
    logPane.setBorder(new EmptyBorder(8,8,8,8));
    logPane.setFont(new Font("Segoe UI", Font.PLAIN, 13));
    JScrollPane scrollPane = new JScrollPane(logPane);

    portField = new JTextField("12345", 6);
    serverNameField = new JTextField("Server", 10);
    serverNameField.setFont(new Font("Segoe UI", Font.PLAIN, 13));

    startButton = new JButton("Start Server");
    startButton.setBackground(new Color(0x3AA655));
    startButton.setForeground(Color.WHITE);
    startButton.addActionListener(e -> startServer());

    viewHistoryButton = new JButton("View History");
    viewHistoryButton.addActionListener(e -> showServerHistory());

    stopButton = new JButton("Stop Server");
    stopButton.setBackground(new Color(0xD64545));
    stopButton.setForeground(Color.WHITE);
    stopButton.setEnabled(false);
    stopButton.addActionListener(e -> stopServer());

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
    top.add(new JLabel("Server Name:")); top.add(serverNameField);
    top.add(new JLabel("Port:")); top.add(portField);
    top.add(startButton); top.add(stopButton);
    top.add(viewHistoryButton);

        // Message input area for server operator
        JTextField serverInput = new JTextField();
        JButton serverSend = new JButton("Send");
        serverSend.setEnabled(false);
        serverSend.setBackground(new Color(0x2D9CDB));
        serverSend.setForeground(Color.WHITE);
        // action will be moved to a helper method below so Enter and button share behavior

    JPanel bottom = new JPanel(new BorderLayout(6,6));
    bottom.setBorder(new EmptyBorder(8,8,8,8));
    serverInput.setFont(new Font("Segoe UI", Font.PLAIN, 14));
    bottom.add(serverInput, BorderLayout.CENTER);
    bottom.add(serverSend, BorderLayout.EAST);

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(top, BorderLayout.NORTH);
        getContentPane().add(scrollPane, BorderLayout.CENTER);
        getContentPane().add(bottom, BorderLayout.SOUTH);

        // Store references for enabling/disabling on start/stop
        this.serverInputField = serverInput;
        this.serverSendButton = serverSend;

        // Send when pressing Enter in the input field
        serverInput.addActionListener(e -> sendServerMessage());
        // Send when pressing the button
        serverSend.addActionListener(e -> sendServerMessage());
    }

    private void showServerHistory() {
        if (server == null) {
            JOptionPane.showMessageDialog(this, "Server not started yet.", "History", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        File hf = server.getHistoryFile();
        if (hf == null || !hf.exists()) {
            JOptionPane.showMessageDialog(this, "No server history found.", "History", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        try {
            StringBuilder sb = new StringBuilder();
            try (BufferedReader br = new BufferedReader(new FileReader(hf))) {
                String l;
                while ((l = br.readLine()) != null) {
                    sb.append(l).append('\n');
                }
            }
            showTextDialog("Server History", sb.toString());
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Failed to read history: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void showTextDialog(String title, String text) {
        JTextArea area = new JTextArea(text);
        area.setEditable(false);
        area.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        JScrollPane sp = new JScrollPane(area);
        sp.setPreferredSize(new Dimension(600, 400));
        JOptionPane.showMessageDialog(this, sp, title, JOptionPane.PLAIN_MESSAGE);
    }

    private void startServer() {
        int port = Integer.parseInt(portField.getText().trim());
        server = new Server(port);
        server.addMessageListener(this);
        Thread t = new Thread(server::start);
        t.start();
    appendMessage("[Server GUI]: Started server on port " + port);
        // Broadcast a join message as the server name
        String name = serverNameField.getText().trim();
        if (name.isEmpty()) name = "Server";
        server.sendFromServer(name, "has started the server");
        // Update UI states
        serverNameField.setEnabled(false);
        serverSendButton.setEnabled(true);
        serverInputField.requestFocusInWindow();
        startButton.setEnabled(false);
        stopButton.setEnabled(true);
    }

    private void stopServer() {
        if (server != null) server.stop();
        appendMessage("[Server GUI]: Server stopped");
        startButton.setEnabled(true);
        stopButton.setEnabled(false);
        serverNameField.setEnabled(true);
        if (serverSendButton != null) serverSendButton.setEnabled(false);
    }

    private void sendServerMessage() {
        if (server == null) return;
        String name = serverNameField.getText().trim();
        if (name.isEmpty()) name = "Server";
        String text = serverInputField.getText().trim();
        if (text.isEmpty()) return;
        server.sendFromServer(name, text);
        serverInputField.setText("");
    }

    @Override
    public void onMessage(String message) {
        appendMessage(message);
    }

    private void appendMessage(String msg) {
        SwingUtilities.invokeLater(() -> {
            try {
                StyledDocument doc = logPane.getStyledDocument();
                Style defaultStyle = doc.getStyle(StyleContext.DEFAULT_STYLE);
                Style regular = doc.getStyle("regular");
                if (regular == null) {
                    regular = doc.addStyle("regular", defaultStyle);
                    StyleConstants.setFontFamily(regular, "Segoe UI");
                    StyleConstants.setFontSize(regular, 13);
                }

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
                if (msg.startsWith("[Server GUI]:") || msg.startsWith("[Server]:")) {
                    chosen = systemStyle; // internal server logs
                } else if (msg.startsWith("[")) {
                    int end = msg.indexOf("]");
                    if (end > 1) {
                        String name = msg.substring(1, end);
                        String serverName = serverNameField.getText().trim();
                        if (!serverName.isEmpty() && name.equals(serverName)) chosen = selfStyle;
                        else if (name.equalsIgnoreCase("Server")) chosen = serverStyle;
                        else chosen = otherStyle;
                    }
                }

                doc.insertString(doc.getLength(), msg + "\n", chosen);
                logPane.setCaretPosition(doc.getLength());
            } catch (BadLocationException e) {
                // fallback
                logPane.setText(logPane.getText() + msg + "\n");
            }
        });
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            ServerGUI gui = new ServerGUI();
            gui.setVisible(true);
        });
    }
}
