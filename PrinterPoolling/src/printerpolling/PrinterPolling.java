/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package printerpolling;

import java.util.ArrayList;
import java.util.Arrays;

/**
 *
 * @author felipesilva
 */
public class PrinterPolling {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        if (args.length != 0) {
            init(args);
        } else {
            test();
        }
    }

    private static Client getClient(String arg) {
        String[] address = arg.split(":");
        return new Client(address[0], address[1]);
    }

    private static void createClient(String[] args) {
        ArrayList<Client> clients = new ArrayList<>();
        String serverAddress = args[1];
        for (int x = 2; x < args.length; x++) {
            clients.add(getClient(args[x]));
        }
        Node node = new Node("NodeA", 8, getClient(serverAddress));
        node.initAddresses(clients);
    }

    private static void createServer(String[] args) {
        new Server();
    }

    private static void init(String[] args) {
        if (args.length != 0) {
            String param = args[0];
            if (param.equals("-client")) {
                createClient(args);
            } else if (param.equals("-server")) {
                createServer(args);
            } else {
                System.out.println("Opcao invalida!!");
            }
        } else {
            System.out.println("Falta o parametro de execucao!!");
        }
    }

    private static void test() {
        int basePort = 10;
        new Thread(() -> {
            new Server(basePort + 8000);
        }).start();

//        new Thread(() -> {
//            Node node = new Node("node1", basePort + 8002, new Client("10.2.5.3", basePort + 8000));
//            node.initAddresses(new ArrayList<Client>(Arrays.asList(
//                    new Client("10.2.5.1", basePort + 8001)
//            ))
//            );
//        }).start();
//
//        new Thread(() -> {
//            Node node = new Node("node2", basePort + 8001, new Client("10.2.5.3", basePort + 8000));
//            node.initAddresses(new ArrayList<Client>(Arrays.asList(
//                    new Client("10.2.5.2", basePort + 8002)
//            ))
//            );
//        }).start();

    }

}
