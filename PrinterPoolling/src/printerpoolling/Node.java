/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package printerpoolling;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author 940437
 */
public class Node {

    private int numClients;
    private ServerSocket listenerSocket;
    private HashMap<String, Socket> clientsTable;
    private int defaultClientPort = 8000;
    private final int defaultServerPort = 8001;
    private HashMap<String, Message> currentTable;
    private HashMap<String, Message> proxTable;
    private ArrayList<Message> messages;
    private static final String defaultIp = "127.0.0.1";
    private NodeStatus status;
    private long HSN, OSN;
    private Message lastMessage;

    public Node() {
        numClients = 1;
        HSN = 0;
        OSN = 0;
        status = NodeStatus.FREE;
        currentTable = new HashMap<>();
        proxTable = new HashMap<>();
        messages = new ArrayList<>();
        clientsTable = new HashMap<>();
    }

    //Carvalho Roucariol Algorithm Logic
    private void verifyCanAccess() {
        new Thread(
                () -> {
                    while (true) {
                        if (getRamdomNumber() > 0.5) {
                            tryEntryCriticalSection();
                        }
                    }
                }
        ).start();
    }

    private double getRamdomNumber() {
        return new Random().nextFloat();
    }

    private void tryEntryCriticalSection() {
        status = NodeStatus.WAITING;
        OSN = HSN + 1;
        sendMessageToEntryCriticalSection();
    }

    private void sendMessageToEntryCriticalSection() {
        currentTable = (HashMap<String, Message>) proxTable.clone();
        for (Map.Entry<String, Message> i : currentTable.entrySet()) {
            if (i.getValue() == null || i.getValue().getMessageType() == MessageType.REQUEST) {
                sendMessage(clientsTable.get(i.getKey()), Message.getRequestMessage(OSN));
            }
        }
    }

    private void handleMessage(Socket client, Message message) {
        switch (message.getMessageType()) {
            case REQUEST:
                proxTable.put(message.getNodeId(), message);
                handleCommunicationMessage(message);
                break;
            case CONNECT:
                handleConnectMessage(client, message);
                break;
            case REPLY:
                currentTable.put(message.getNodeId(), message);
                proxTable.put(message.getNodeId(), message);
                verifyAfterReply();
                break;
        }
    }

    private void verifyAfterReply() {
        for (Map.Entry<String, Message> i : currentTable.entrySet()) {
            if (i.getValue() == null || i.getValue().getMessageType() == MessageType.REQUEST) {
                return;
            }
        }
        entryCriticalSection();
    }

    private void entryCriticalSection() {
        status = NodeStatus.BUSY;
        for (int x = 0; x < 10; x++) {
            System.out.println(lastMessage.getTimestamp() + x);
            try {
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException ex) {
                System.out.println(ex.getMessage());
            }
        }
        status = NodeStatus.FREE;
        notifyQueue();
    }

    private void notifyQueue() {
        messages.forEach((m) -> sendMessage(clientsTable.get(m.getNodeId()), Message.getReplyMessage()));
    }

    private void handleCommunicationMessage(Message message) {
        HSN = HSN > message.getOSN() ? HSN : message.getOSN();
        switch (status) {
            case FREE:
                sendReply(clientsTable.get(message.getNodeId()));
                break;
            case BUSY:
                messages.add(message);
                break;
            case WAITING:
                if (OSN < message.getOSN()) {
                    messages.add(message);
                } else {
                    sendReply(clientsTable.get(message.getNodeId()));
                }
                break;
        }
    }

    //Sockets and Connections stuffs
    public void initAddresses(String[] args) {
        numClients += Integer.parseInt(args[0]);
        for (int x = 1; x < args.length; x++) {
            connectToClient(args[x]);
        }
        waitForClients();
    }

    private void connectToClient(String ipAddress) {
        try {
            Socket client = new Socket(ipAddress, ++defaultClientPort);
            clientsTable.put(ipAddress, client);
            sendMessage(client, getConnectMessage(client));
        } catch (IOException ex) {
            System.out.println("Erro ao criar conexao com " + ipAddress);
        }
    }

    private void waitForClients() {
        new Thread(
                () -> {
                    try {
                        listenerSocket = new ServerSocket(defaultServerPort);
                        handleNewConnection(listenerSocket.accept());
                    } catch (Exception ex) {
                        System.out.println("Erro ao conectar   escutar porta"
                                + defaultServerPort + "causa : " + ex.getMessage());
                    }
                }
        ).start();

    }

    private void handleNewConnection(Socket client) {
        new Thread(
                () -> {
                    try {
                        ObjectInputStream ois
                        = new ObjectInputStream(client.getInputStream());
                        Message message = null;
                        while ((message = ((Message) ois.readObject())) != null) {
                            message.setNodeId(client.getInetAddress().toString());
                            lastMessage = message;
                            handleMessage(client, message);
                        }
                    } catch (Exception ex) {
                        System.out.println("Erro ao conectar ao  nó "
                                + client.getInetAddress()
                                + " causa : " + ex.getMessage());
                    }
                }
        ).start();
    }

    private void handleConnectMessage(Socket client, Message message) {
        clientsTable.put(message.getNodeId(), client);
        proxTable.put(message.getNodeId(), null);
        currentTable.put(message.getNodeId(), null);
    }

    private void sendMessage(Socket client, Message message) {
        try {
            ObjectOutputStream ois
                    = new ObjectOutputStream(client.getOutputStream());
            ois.writeObject(message);
        } catch (IOException ex) {
            System.out.println("Erro ao enviar msg para "
                    + client.getInetAddress());
        }
    }

    private Message getConnectMessage(Socket client) {
        return Message.getConnectMessage();
    }

    private void sendReply(Socket get) {
        sendMessage(get, Message.getReplyMessage());
    }

}
