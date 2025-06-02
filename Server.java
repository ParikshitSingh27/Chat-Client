import java.awt.*;
import java.io.*;
import java.net.*;
import java.util.*;
import javax.swing.*;
import javax.swing.text.*;

public class Server extends JFrame {
    private JTextField portField, messageField;
    private JButton startButton, broadcastButton;
    private JTextPane chatPane;
    private StyledDocument doc;
    private ServerSocket serverSocket;
    private Set<ClientHandler> clients = Collections.synchronizedSet(new HashSet<>());

    public Server() {
        setTitle("Chat + File Server");
        setSize(550, 400);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        JPanel topPanel = new JPanel(new FlowLayout());
        topPanel.add(new JLabel("Port:"));
        portField = new JTextField("12345", 8);
        startButton = new JButton("Start Server");
        topPanel.add(portField);
        topPanel.add(startButton);
        add(topPanel, BorderLayout.NORTH);

        chatPane = new JTextPane();
        chatPane.setEditable(false);
        doc = chatPane.getStyledDocument();
        add(new JScrollPane(chatPane), BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel(new BorderLayout());
        messageField = new JTextField();
        broadcastButton = new JButton("Broadcast");
        bottomPanel.add(messageField, BorderLayout.CENTER);
        bottomPanel.add(broadcastButton, BorderLayout.EAST);
        add(bottomPanel, BorderLayout.SOUTH);

        startButton.addActionListener(e -> startServer());
        broadcastButton.addActionListener(e -> {
            String msg = messageField.getText().trim();
            if (!msg.isEmpty()) {
                broadcastMessage("Server: " + msg, true);
                messageField.setText("");
            }
        });
    }

    private void startServer() {
        int port = Integer.parseInt(portField.getText());
        new Thread(() -> {
            try {
                serverSocket = new ServerSocket(port);
                appendStyledMessage("Server started on port " + port, true);

                while (true) {
                    Socket socket = serverSocket.accept();
                    appendStyledMessage("Client connected: " + socket.getInetAddress(), false);
                    ClientHandler handler = new ClientHandler(socket);
                    clients.add(handler);
                    new Thread(handler).start();
                }
            } catch (IOException e) {
                appendStyledMessage("Error: " + e.getMessage(), false);
            }
        }).start();
    }

    private void broadcastMessage(String message, boolean fromServer) {
        appendStyledMessage(message, fromServer);
        synchronized (clients) {
            for (ClientHandler c : clients) {
                c.sendMessage(message);
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

    class ClientHandler implements Runnable {
        private Socket socket;
        private BufferedReader in;
        private PrintWriter out;
        private DataInputStream dis;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        public void sendMessage(String msg) {
            if (out != null) out.println(msg);
        }

        @Override
        public void run() {
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                dis = new DataInputStream(socket.getInputStream());
                out = new PrintWriter(socket.getOutputStream(), true);

                String msg;
                while ((msg = in.readLine()) != null) {
                    if (msg.startsWith("FILE:")) {
                        receiveFile(msg.substring(5));
                    } else {
                        broadcastMessage(msg, false);
                    }
                }
            } catch (IOException e) {
                appendStyledMessage("A client disconnected.", false);
            } finally {
                try {
                    clients.remove(this);
                    socket.close();
                } catch (IOException ignored) {}
            }
        }

        private void receiveFile(String fileName) {
            try {
                FileOutputStream fos = new FileOutputStream("Received_" + fileName);
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = dis.read(buffer)) != -1) {
                    fos.write(buffer, 0, bytesRead);
                    if (bytesRead < 4096) break;
                }
                fos.close();
                appendStyledMessage("File received: " + fileName, false);
            } catch (IOException e) {
                appendStyledMessage("Error receiving file.", false);
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new Server().setVisible(true));
    }
}
