import java.awt.*;
import java.io.*;
import java.net.*;
import javax.swing.*;
import javax.swing.text.*;

public class Client extends JFrame {
    private JTextField hostField, portField, messageField;
    private JButton connectButton, sendButton, fileButton;
    private JTextPane chatPane;
    private StyledDocument doc;
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private DataOutputStream dataOut;

    public Client() {
        setTitle("Chat + File Client");
        setSize(550, 400);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        JPanel topPanel = new JPanel(new FlowLayout());
        topPanel.add(new JLabel("Host:"));
        hostField = new JTextField("localhost", 10);
        topPanel.add(hostField);

        topPanel.add(new JLabel("Port:"));
        portField = new JTextField("12345", 5);
        topPanel.add(portField);

        connectButton = new JButton("Connect");
        topPanel.add(connectButton);
        add(topPanel, BorderLayout.NORTH);

        chatPane = new JTextPane();
        chatPane.setEditable(false);
        doc = chatPane.getStyledDocument();
        add(new JScrollPane(chatPane), BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel(new BorderLayout());
        messageField = new JTextField();
        bottomPanel.add(messageField, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout());
        sendButton = new JButton("Send");
        fileButton = new JButton("Send File");
        buttonPanel.add(fileButton);
        buttonPanel.add(sendButton);
        bottomPanel.add(buttonPanel, BorderLayout.EAST);

        add(bottomPanel, BorderLayout.SOUTH);

        connectButton.addActionListener(e -> connectToServer());
        sendButton.addActionListener(e -> sendMessage());
        fileButton.addActionListener(e -> sendFile());

        sendButton.setEnabled(false);
        fileButton.setEnabled(false);
    }

    private void connectToServer() {
        String host = hostField.getText();
        int port = Integer.parseInt(portField.getText());

        try {
            socket = new Socket(host, port);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            dataOut = new DataOutputStream(socket.getOutputStream());

            appendStyledMessage("Connected to server at " + host + ":" + port, false);
            sendButton.setEnabled(true);
            fileButton.setEnabled(true);
            connectButton.setEnabled(false);

            new Thread(() -> {
                try {
                    String msg;
                    while ((msg = in.readLine()) != null) {
                        appendStyledMessage(msg, false);
                    }
                } catch (IOException e) {
                    appendStyledMessage("Disconnected from server.", false);
                }
            }).start();

        } catch (IOException e) {
            appendStyledMessage("Connection failed: " + e.getMessage(), false);
        }
    }

    private void sendMessage() {
        String message = messageField.getText().trim();
        if (!message.isEmpty()) {
            out.println("Client: " + message);
            appendStyledMessage("You: " + message, true);
            messageField.setText("");
        }
    }

    private void sendFile() {
        JFileChooser fileChooser = new JFileChooser();
        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            try {
                FileInputStream fileIn = new FileInputStream(file);
                out.println("FILE:" + file.getName());

                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = fileIn.read(buffer)) != -1) {
                    dataOut.write(buffer, 0, bytesRead);
                }

                dataOut.flush();
                fileIn.close();

                appendStyledMessage("File sent: " + file.getName(), true);
            } catch (IOException e) {
                appendStyledMessage("File transfer failed: " + e.getMessage(), false);
            }
        }
    }

    private void appendStyledMessage(String message, boolean alignRight) {
        SimpleAttributeSet set = new SimpleAttributeSet();
        StyleConstants.setAlignment(set, alignRight ? StyleConstants.ALIGN_RIGHT : StyleConstants.ALIGN_LEFT);
        StyleConstants.setFontSize(set, 14);
        StyleConstants.setSpaceAbove(set, 5);
        StyleConstants.setSpaceBelow(set, 5);
        doc.setParagraphAttributes(doc.getLength(), 1, set, false);

        try {
            doc.insertString(doc.getLength(), message + "\n", set);
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new Client().setVisible(true));
    }
}
