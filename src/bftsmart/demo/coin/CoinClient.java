package bftsmart.demo.coin;

import bftsmart.tom.ServiceProxy;
import java.io.*;
import java.util.*;
import java.security.*;
import javax.crypto.*;

/**
 * Example client that updates a BFT replicated service (client).
 * 
 * @author Weimeng Pu
 */
public class CoinClient {

    private final int keySize = 512;
    private PublicKey public_key;
    private PrivateKey private_key;
    private int nonce;

    // Initialization of coin client that has public key, private key, and nonce
    public CoinClient() throws NoSuchAlgorithmException{
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(keySize);
        KeyPair kp = kpg.genKeyPair();
        public_key = kp.getPublic();
        this.private_key = kp.getPrivate();
        this.nonce = 0;
    }

    public static void main(String[] args) throws Exception {
        // Read input and initialization
        int continued = 1;
        Scanner in = new Scanner(System.in); 
        String id = args[0];
        ServiceProxy coinProxy = new ServiceProxy(Integer.parseInt(id));
        CoinClient cc =  new CoinClient();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream dos = new ObjectOutputStream(baos);
        ArrayList<Object> wol = new ArrayList<>();
        // Send serialized client create request to server
        wol.add("client create");
        wol.add(cc.public_key);
        wol.add(id);
        dos.writeObject(wol);
        System.out.println("New client created at client");
        coinProxy.invokeOrdered(baos.toByteArray());
        System.out.println("Welcome to the client interface");
        // Interface to check balance or send money
        do {
            System.out.println("Enter 1 for checking balance and 2 for sending coin");
            int input = Integer.parseInt(in.nextLine());
            // Check balance
            if (input == 1) {
                System.out.println("Checking balance... Enter the id to proceed");
                // Read input and initialization
                String message = in.nextLine();
                byte[] signed = Utils.sign(message, cc.private_key);
                baos = new ByteArrayOutputStream();
                dos = new ObjectOutputStream(baos);
                wol = new ArrayList<>();
                // Send serialized check balance request to server
                wol.add("check balance");
                wol.add(cc.public_key);
                wol.add(message);
                wol.add(signed);
                dos.writeObject(wol);
                byte[] output = coinProxy.invokeOrdered(baos.toByteArray());
                Object deser = new ObjectInputStream(new ByteArrayInputStream(output)).readObject();
                ArrayList<Object> deser_l =  (ArrayList<Object>) deser;
                // Check if the request is verified in server
                int success = (Integer) deser_l.get(0);
                if (success == 0) {
                    System.out.println("Balance check verified and completed at server");
                    int balance = (Integer) deser_l.get(1);
                    System.out.println("Current balance is: " + balance);
                }
                else {System.out.println("Balance check failed. Check your input info");}
            }
            // Send money
            else if (input == 2) {
                // Read input and initialization
                System.out.println("Enter your id, your nonce, receiver's account id and amount separated by space");
                String message = in.nextLine();
                byte[] signed = Utils.sign(message, cc.private_key);
                baos = new ByteArrayOutputStream();
                dos = new ObjectOutputStream(baos);
                wol = new ArrayList<>();
                // Send serialized send money request to server
                wol.add("send money");
                wol.add(cc.public_key);
                wol.add(message);
                wol.add(signed);
                dos.writeObject(wol);
                System.out.println("Transfer done at client");
                byte[] output = coinProxy.invokeOrdered(baos.toByteArray());
                Object deser = new ObjectInputStream(new ByteArrayInputStream(output)).readObject();
                // Check if the request is verified in server
                int success = (Integer) deser;
                if (success == 0) {
                    cc.nonce += 1;
                    System.out.println("Transfer verified and completed at server");
                }
                else {System.out.println("Transfer failed. Check your input info");}
            }
            // Decide if repeat the process
            System.out.println("More transferring or checking balance? (Enter 1 for Yes or 0 for No) "); 
            continued = Integer.parseInt(in.nextLine()); 
        } while(continued == 1);
        coinProxy.close();
    }
}