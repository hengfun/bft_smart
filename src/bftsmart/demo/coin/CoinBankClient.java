package bftsmart.demo.coin;

import bftsmart.tom.ServiceProxy;
//import sun.nio.cs.*;
import java.io.*;
import java.util.*;
import java.security.*;
import javax.crypto.*;

/**
 * Example client that updates a BFT replicated service (bank)
 * 
 * @author Weimeng Pu
 */
public class CoinBankClient {

    private final int keySize = 512;
    private PublicKey public_key;
    private PrivateKey private_key;
    private int total_deposit;

    // Initialization of bank that has public key, private key, and total deposit
    public CoinBankClient() throws NoSuchAlgorithmException{
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(keySize);
        KeyPair kp = kpg.genKeyPair();
        this.public_key = kp.getPublic();
        this.private_key = kp.getPrivate();
        this.total_deposit = 0;
    }

    public static void main(String[] args) throws Exception {
        // Read input and initialization
        int continued = 1;
        Scanner in = new Scanner(System.in); 
        ServiceProxy coinProxy = new ServiceProxy(Integer.parseInt(args[0]));
        CoinBankClient cbc =  new CoinBankClient();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream dos = new ObjectOutputStream(baos);
        ArrayList<Object> wol = new ArrayList<>();
        // Send serialized bank create request to server
        wol.add("bank create");
        wol.add(cbc.public_key);
        dos.writeObject(wol);
        System.out.println("New bank created at client");
        coinProxy.invokeUnordered(baos.toByteArray());
        System.out.println("Start the mint");
        // Mint or deposit process
        do {
            System.out.println("Enter the account id and amount separated by space");
            // Read input and initialization
            String message = in.nextLine();
            cbc.total_deposit += Integer.parseInt(message.split(" ")[1]);
            byte[] signed = Utils.sign(message, cbc.private_key);
            baos = new ByteArrayOutputStream();
            dos = new ObjectOutputStream(baos);
            wol = new ArrayList<>();
            // Send serialized bank mint request to server
            wol.add("bank deposit");
            wol.add(cbc.public_key);
            wol.add(message);
            wol.add(signed);
            dos.writeObject(wol);
            coinProxy.invokeUnordered(baos.toByteArray());
            System.out.println("Mint done");
            // Decide if repeat the process
            System.out.println("More mint? (Enter 1 for Yes or 0 for No) "); 
            continued = Integer.parseInt(in.nextLine()); 
        } while(continued == 1);
        coinProxy.close();
    }
}