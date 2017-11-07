/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package printerpolling;

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

    private Socket serverSocket;
    private int numClients;
    private ServerSocket listenerSocket;
    private HashMap<String, Socket> clientsTable;
    private final int defaultServerPort;
    private HashMap<String, Message> currentTable;
    private HashMap<String, Message> proxTable;
    private ArrayList<Message> messages;
    private static final String defaultIp = "127.0.0.1";
    private NodeStatus status;
    private long HSN, OSN;
    private String name;
    private Message lastMessage;

    public Node(String name, int port, Client server) {
        this.name = name;
        numClients = 1;
        HSN = 0;
        OSN = 0;
        status = NodeStatus.FREE;
        currentTable = new HashMap<>();
        proxTable = new HashMap<>();
        messages = new ArrayList<>();
        clientsTable = new HashMap<>();
        defaultServerPort = port;
        initServerSocket(server);
       // waitForClients();
       // verifyCanAccess();
    }

    private void initServerSocket(Client server) {
        try {
            serverSocket = new Socket(server.getIpAddress(), server.getPort());
            handleNewConnection(serverSocket);
        } catch (Exception ex) {
            System.out.println(name + " :Erro ao conectar com servidor " + ex.getMessage());
            sleep(2);
        }
    }

    //Carvalho Roucariol Algorithm Logic
    private void verifyCanAccess() {
        new Thread(
                () -> {
                    while (true) {
                        if (getRamdomNumber() > 0.5) {
                            System.out.println(name + " Vai tentar acessar o recurso");
                            sleep(2);
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
                sendMessage(clientsTable.get(i.getKey()),
                        Message.getRequestMessage(
                                name,
                                clientsTable.get(i.getKey()).getInetAddress().toString(),
                                OSN)
                );
            }
        }
    }

    private void handleMessage(Socket client, Message message) {
        logReceiveMsg(message);
        switch (message.getMessageType()) {
            case REQUEST:
                onRequestReceived(message);
                break;
            case CONNECT:
                handleConnectMessage(client, message);
                break;
            case REPLY:
                onReplyReceived(message);
                break;
            case FINISHED:
                onFinisheReceivied();
                break;
        }
    }

    private void onRequestReceived(Message message) {
        proxTable.put(message.getNodeId(), message);
        handleCommunicationMessage(message);
    }

    private void onReplyReceived(Message message) {
        currentTable.put(message.getNodeId(), message);
        proxTable.put(message.getNodeId(), message);
        verifyAfterReply();
    }

    private void onFinisheReceivied() {
        status = NodeStatus.FREE;
        notifyQueue();
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
        sendMessage(serverSocket, Message.getStartMessage(
                name,
                serverSocket.getInetAddress().toString()
        )
        );
    }

    private void notifyQueue() {
        messages.forEach((m) -> sendMessage(
                clientsTable.get(m.getNodeId()),
                Message.getReplyMessage(name,
                        clientsTable.get(m.getNodeId()).getInetAddress().toString()
                )
        ));
        messages.clear();
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
    public void initAddresses(ArrayList<Client> args) {
        numClients += args.size();
        for (Client c : args) {
            connectToClient(c);
        }
    }

    private void connectToClient(Client ct) {
        new Thread(() -> {
            try {
                Socket client = new Socket(ct.getIpAddress(), ct.getPort());
                clientsTable.put(ct.getIpAddress(), client);
                sendMessage(client, getConnectMessage(client));
            } catch (Exception ex) {
                System.out.println(name + " : Erro ao criar conexao com " + ct.getIpAddress() + ":" + ct.getPort() + " : " + ex.getMessage());
                sleep(5);
                connectToClient(ct);
            }
        }).start();

    }

    private void waitForClients() {
        new Thread(
                () -> {
                    try {
                        listenerSocket = new ServerSocket(defaultServerPort);
                        handleNewConnection(listenerSocket.accept());
                    } catch (Exception ex) {
                        System.out.println(name + " :Erro ao conectar   escutar porta"
                                + defaultServerPort + "causa : " + ex.getMessage());
                        sleep(2);
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
                        System.out.println(name + " :Erro ao conectar ao  no "
                                + client.getInetAddress()
                                + " causa : " + ex.getMessage());
                        sleep(2);
                    }
                }
        ).start();
    }

    private void handleConnectMessage(Socket client, Message message) {
        clientsTable.put(message.getNodeId(), client);
        proxTable.put(message.getNodeId(), null);
        currentTable.put(message.getNodeId(), null);
    }

    private void logSendMsg(Message message) {
        System.out.println(name + " " + message.getMessageTypeName()
                + "----> " + message.getNodeName()
                + " [" + message.getNodeId()
                + "] (" + message.getTimestamp() + ")");
        sleep(2);
    }

    private void sendMessage(Socket client, Message message) {
        try {
            logSendMsg(message);
            ObjectOutputStream ois
                    = new ObjectOutputStream(client.getOutputStream());
            ois.writeObject(message);
        } catch (Exception ex) {
            System.out.println(name + " :Erro ao enviar msg para "
                    + client.getInetAddress());
            sleep(2);
        }
    }

    private Message getConnectMessage(Socket client) {
        return Message.getConnectMessage(name, listenerSocket.getInetAddress().toString());
    }

    private void sendReply(Socket get) {
        sendMessage(get, Message.getReplyMessage(name, listenerSocket.getInetAddress().toString()));
    }

    private void logReceiveMsg(Message message) {
        System.out.println(name + " " + message.getMessageTypeName()
                + "<---- " + message.getNodeName() + " [" + message.getNodeId()
                + "] (" + message.getTimestamp() + ")");
        sleep(2);
    }

    private void sleep(int seconds) {
        try {
            TimeUnit.SECONDS.sleep(seconds);
        } catch (Exception e) {
        }
    }
;

}
