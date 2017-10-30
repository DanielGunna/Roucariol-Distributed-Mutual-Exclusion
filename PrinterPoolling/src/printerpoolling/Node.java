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
    private HashMap<String,Message> criticalSectionTable;
    private static final String defaultIp = "127.0.0.1";

    public Node() {
        numClients = 1;
        criticalSectionTable = new HashMap<>();
        clientsTable = new HashMap<>();
    }

    public void initAddresses(String[] args) {
        numClients += Integer.parseInt(args[0]);
        for (int x = 1; x < args.length; x++) {
            connectToClient(args[x]);
        }
        waitForClients();
    }

    private void waitForClients() {
        new Thread(
                () -> {
                    try {
                        listenerSocket = new ServerSocket(defaultServerPort);
                        handleNewConnection(listenerSocket.accept());
                    } catch (Exception ex) {
                        System.out.println("Erro ao conectar   escutar porta" + defaultServerPort + "causa : " + ex.getMessage());
                    }
                }
        ).start();

    }

    private double getRamdomNumber() {
        return new Random().nextFloat();
    }

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

    private void handleMessage(Message message) {

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

    private void sendMessage(Socket client, Message message) {
        try {
            ObjectOutputStream ois = new ObjectOutputStream(client.getOutputStream());
            ois.writeObject(message);
        } catch (IOException ex) {
            System.out.println("Erro ao enviar msg para " + client.getInetAddress());
        }
    }

    private void handleNewConnection(Socket client) {
        new Thread(
                () -> {
                    try {
                        ObjectInputStream ois = new ObjectInputStream(client.getInputStream());
                        Message message = null;
                        while ((message = ((Message) ois.readObject())) != null) {
                            handleMessage(message);
                        }
                    } catch (Exception ex) {
                        System.out.println("Erro ao conectar ao  nó " + client.getInetAddress() + " causa : " + ex.getMessage());
                    }
                }
        ).start();
    }
    
    
    
    
    
    

    private Message getConnectMessage(Socket client) {
        return new Message();
    }

    private void tryEntryCriticalSection() {
    
    }
    
    private boolean isCriticalSectionFree(){
        for(Map.Entry<String,Message> i : criticalSectionTable.entrySet()){
            if(!i.getKey().equals(defaultIp) && i.getValue().isEntryCriticalSection()){
                
            }
        }
        return true;
    }

}
