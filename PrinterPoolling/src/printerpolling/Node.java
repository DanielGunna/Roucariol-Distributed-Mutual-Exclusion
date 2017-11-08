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
    private HashMap<String,ObjectOutputStream> streams;
    private ArrayList<Message> messages;
    private static final String defaultIp = "127.0.0.1";
    private NodeStatus status;
    private long HSN, OSN;
    private String name;
    private Message lastMessage;
    private ObjectOutputStream ois;

    public Node(String name, int port, Client server) {
        this.name = name;
        numClients = 1;
        HSN = System.currentTimeMillis();
        OSN = 0;
        status = NodeStatus.FREE;
        currentTable = new HashMap<>();
        messages = new ArrayList<>();
        clientsTable = new HashMap<>();
        streams = new HashMap<>();
        defaultServerPort = port;
        initServerSocket(server);
        verifyCanAccess();
        waitForClients();
    }

    private void initServerSocket(Client server) {
        try {
            serverSocket = new Socket(server.getIpAddress(), server.getPort());
            handleNewConnection(serverSocket);
        } catch (Exception ex) {
            System.out.println(name + " :Error connecting to server. Message: " + ex.getMessage());
            sleep(2);
        }
    }

    //Carvalho Roucariol Algorithm Logic
    private void verifyCanAccess() {
        new Thread(
                () -> {
                    while (true) {
                        System.out.println("Node Status = " + status.toString());
                        sleep(2);
                        if (status == NodeStatus.FREE) {
                            System.out.println("Node Status = " + status.toString());
                            if (getRamdomNumber() > 0.5) {

                                new Thread(() -> {
                                    tryEntryCriticalSection();
                                }).start();

                            }
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
        System.out.println("Updated OSN = " + OSN);
        sleep(2);
        sendMessageToEntryCriticalSection();
    }

    private void sendMessageToEntryCriticalSection() {
        sleep(10);
        boolean coming = true;
        
        // If I am the Only One...
        if (currentTable.size() == 0) {
            entryCriticalSection();
            return;
        }
        
        // Does anyone else ?
        for (Map.Entry<String, Message> i : currentTable.entrySet()) {
            if (i.getValue() == null || i.getValue().getMessageType() == MessageType.REQUEST) {
                coming = false;
                System.out.println("Sending Request to " + i.getKey());
                sendMessage(clientsTable.get(i.getKey()),
                        Message.getRequestMessage(
                                name,
                                clientsTable.get(i.getKey()).getInetAddress().toString(),
                                OSN)
                );
            }
        }
        if(coming){
            //currentTable = (HashMap<String, Message>) proxTable.clone();
            entryCriticalSection();
        }
    }

    private void handleMessage(Socket client, Message message) {
        System.out.println("Received: " + message.toString() + " from " + client.getInetAddress().toString());
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
        messages.add(message);
        sleep(2);
        handleCommunicationMessage(message);
    }

    private void onReplyReceived(Message message) {
        currentTable.put(message.getNodeId(), message);
        sleep(2);
        verifyAfterReply();
    }

    private void onFinisheReceivied() {
        System.out.println("Server has Finished");
        sleep(2);
        notifyQueue();
    }
    

    private void verifyAfterReply() {
        if(status == NodeStatus.WAITING){
            System.out.println("Verifying if I can enter Critical Section");
            sleep(2);
            for (Map.Entry<String, Message> i : currentTable.entrySet()) {
                if (i.getValue() == null || i.getValue().getMessageType() == MessageType.REQUEST) {
                    System.out.println("Still cannot enter, missing reply from " + i.getKey());
                    sleep(2);
                    return;
                }
            }
            System.out.println("I have received every reply already. Entering critical section...");
            sleep(2);
            entryCriticalSection();
        }
    }

    private void entryCriticalSection() {
        status = NodeStatus.BUSY;
        sleep(2);
        sendMessage(serverSocket, Message.getStartMessage(
                name,
                serverSocket.getInetAddress().toString()
        )
        );
    }

    private void notifyQueue() {
        System.out.println("Time to reply pending requests...");
        sleep(2);
        messages.forEach((m) -> {
            currentTable.put(m.getNodeId(),Message.getRequestMessage(m.getNodeId(), m.getNodeName(), m.getOSN()));
            sendMessage(
                    clientsTable.get(m.getNodeId()),
                    Message.getReplyMessage(name,
                            clientsTable.get(m.getNodeId()).getInetAddress().toString()
                    )
            );
        });
        for(Map.Entry<String,Message> i : currentTable.entrySet()){
            boolean resp = true;
            for(Message m : messages){
                if(m.getNodeId().equals(i.getKey()))
                   resp = false; 
            }
            if(resp && i.getValue().getMessageType() == MessageType.REQUEST){
              sendMessage(
                    clientsTable.get(i.getValue().getNodeId()),
                    Message.getReplyMessage(name,
                            clientsTable.get(i.getValue().getNodeId()).getInetAddress().toString()
                    )
            );
            }
        }
        messages.clear();
        status = NodeStatus.FREE;
        System.out.println("Finished replying to pending requests.");
    }

    private void handleCommunicationMessage(Message message) {
        HSN = HSN > message.getOSN() ? HSN : message.getOSN();
        System.out.println("Node Status = " + status.toString());
        System.out.println("Updated HSN = " + HSN);
        sleep(2);
        switch (status) {
            case FREE:
                sleep(2);
                sendReply(clientsTable.get(message.getNodeId()));
                break;
            case BUSY:
                sleep(2); //Already in Queue
                break;
            case WAITING:
                sleep(2);
                if (OSN < message.getOSN()) {
                    System.out.println("My OSN is smaller = " + OSN + " " + message.getOSN());
                    sleep(2);
                } else {
                    System.out.println("My OSN is bigger = " + OSN + " " + message.getOSN());
                    sleep(2);
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
                System.out.println(name + " : Error when connecting to  " + ct.getIpAddress() + ":" + ct.getPort() + ". Message = " + ex.getMessage());
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
                        while (true) {
                            handleNewConnection(listenerSocket.accept());
                        }
                    } catch (Exception ex) {
                        System.out.println(name + " :Error when connecting to port:"
                                + defaultServerPort + ". Message: " + ex.getMessage());
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
                        System.out.println(name + " :Error when connecting to "
                                + client.getInetAddress()
                                + ". Message: " + ex.getMessage());
                        sleep(2);
                    }
                }
        ).start();
    }

    private void handleConnectMessage(Socket client, Message message) {
        System.out.println("New connection from " + client.getInetAddress().toString());
        clientsTable.put(message.getNodeId(), client);
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
            
            if(!streams.containsKey(client.getInetAddress().toString())) 
                streams.put(client.getInetAddress().toString(),new ObjectOutputStream(client.getOutputStream()));
            streams.get(client.getInetAddress().toString()).writeObject(message);
           // streams.get(client.getInetAddress().toString()).reset();
        } catch (Exception ex) {
            System.out.println(name + " :Error sending message to "
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
            System.out.println("Timer Error when Sleeping");
        }
    }
;

}
