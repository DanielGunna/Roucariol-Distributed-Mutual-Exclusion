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
    private ArrayList<Message> alreadyReplied;

    public Node(String name, int port, Client server) {
        this.name = name;
        numClients = 1;
        HSN = System.currentTimeMillis();
        OSN = 0;
        status = NodeStatus.FREE;
        alreadyReplied = new ArrayList<>();
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
            System.out.println(name + " :Erro ao conectar com servidor " + ex.getMessage());
            sleep(2);
        }
    }

    //Carvalho Roucariol Algorithm Logic
    private void verifyCanAccess() {
        new Thread(
                () -> {
                    while (true) {
                        System.out.println("Rodando rodand");
                        sleep(2);
                        if (status == NodeStatus.FREE) {
                            System.out.println("Estou livre");
                            if (getRamdomNumber() > 0.5) {

                                new Thread(() -> {
                                    System.out.println(name + " Vai tentar acessar o recurso");
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
        System.out.println("Atualizei OSN " + OSN);
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
                System.out.println("Enviando Request para " + i.getKey());
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
        System.out.println("Recebeu : " + message.toString() + " de " + client.getInetAddress().toString());
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
                System.out.println("Indo para o break");
                break;
        }
        System.out.println("Saindo de handle msg");
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
        System.out.println("Servidor terminou !!!");
        sleep(2);
        notifyQueue();
        System.out.println("Saindo de onFinished");
    }
    

    private void verifyAfterReply() {
        if(status == NodeStatus.WAITING){
            System.out.println("Verficando se pode entrar na regiao critica...");
            sleep(2);
            for (Map.Entry<String, Message> i : currentTable.entrySet()) {
                if (i.getValue() == null || i.getValue().getMessageType() == MessageType.REQUEST) {
                    System.out.println("Ainda nao pode falta resposta do no " + i.getKey());
                    sleep(2);
                    return;
                }
            }
            System.out.println("Pode entrar na regiao critica");
            sleep(2);
            entryCriticalSection();
        }
    }

    private void entryCriticalSection() {
        status = NodeStatus.BUSY;
        System.out.println("Enviando mensagem para server");
        sleep(2);
        sendMessage(serverSocket, Message.getStartMessage(
                name,
                serverSocket.getInetAddress().toString()
        )
        );
    }

    private void notifyQueue() {
        System.out.println("Notificando fila");
        sleep(2);
        messages.forEach((m) -> {
            currentTable.put(m.getNodeId(),Message.getRequestMessage(m.getNodeId(), m.getNodeName(), m.getOSN()));
            
            if ( !alreadyReplied.contains(m) ){
                System.out.println("Enviando reply para " + m.getNodeId());
                sendMessage(
                        clientsTable.get(m.getNodeId()),
                        Message.getReplyMessage(name,
                                clientsTable.get(m.getNodeId()).getInetAddress().toString()
                        )
                );
            }
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
        System.out.println("Saindo de notifyQueue");
    }

    private void handleCommunicationMessage(Message message) {
        HSN = HSN > message.getOSN() ? HSN : message.getOSN();
        System.out.println("Atualizei o HSN " + HSN);
        sleep(2);
        switch (status) {
            case FREE:
                System.out.println("Recebi um request, mas estou livre vou mandar reply");
                sleep(2);
                sendReply(clientsTable.get(message.getNodeId()));
                break;
            case BUSY:
                System.out.println("Recebi um request, vou add na fila");
                sleep(2);
                alreadyReplied.add(message);//Ja foi enfileirado
                break;
            case WAITING:
                System.out.println("Recebi um request,estou esperando");
                sleep(2);
                if (OSN < message.getOSN()) {
                    System.out.println("Meu OSN é menor " + OSN + " " + message.getOSN());
                    sleep(2);
                    //messages.add(message);
                } else {
                    System.out.println("Meu OSN é maior " + OSN + " " + message.getOSN());
                    sleep(2);
                    sendReply(clientsTable.get(message.getNodeId()));
                    alreadyReplied.add(message);
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
                        while (true) {
                            handleNewConnection(listenerSocket.accept());
                        }
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
                            System.out.println("Nova iteracao handleNewConnetion" + client.getInetAddress().toString());
                            message.setNodeId(client.getInetAddress().toString());
                            lastMessage = message;
                            handleMessage(client, message);
                            System.out.println("Terminando iteração handleNewConnection");
                        }

                        System.out.println("Sai do while");
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
        System.out.println("Nova conexao de " + client.getInetAddress().toString());
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
            System.out.println(name + " :Erro ao enviar msg para "
                    + client.getInetAddress());
            sleep(2);
        }
    }

    private Message getConnectMessage(Socket client) {
        return Message.getConnectMessage(name, listenerSocket.getInetAddress().toString());
    }

    private void sendReply(Socket get) {
        System.out.println("Enviandio reply para "+get.getInetAddress().toString());
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
            System.out.println("Erro timer ");
        }
    }
;

}
