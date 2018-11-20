package bftsmart.demo.coin;

import bftsmart.tom.MessageContext;
import bftsmart.tom.ServiceReplica;
import bftsmart.tom.server.defaultservices.DefaultSingleRecoverable;
//import sun.nio.cs.*;
import java.io.*;
import java.util.*;
import java.security.*;
import javax.crypto.*;

/**
 * BFT replicated service
 * 
 * @author Weimeng Pu
 */
public final class CoinServer extends DefaultSingleRecoverable {

    private final int keySize = 512;
    private PublicKey public_key;
    private PrivateKey private_key;
    private PublicKey bank_public_key;
    private HashMap<Key, Integer> ledger;
    private HashMap<Key, Integer> nonces;
    private HashMap<String, Key> mapping;

    // Initialization of server that has public key, private key, bank's public key, ledger record, nonces record, and clientId-publicKey mapping
    public CoinServer(int id) throws NoSuchAlgorithmException {
        new ServiceReplica(id, this, this);
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(keySize);
        KeyPair kp = kpg.genKeyPair();
        this.public_key = kp.getPublic();
        this.private_key = kp.getPrivate();
        this.bank_public_key = null;
        this.ledger = new HashMap<>();
        this.nonces = new HashMap<>();
        this.mapping = new HashMap<>();
    }
            
    // Method which handles all the requests from the bank
    @Override
    public byte[] appExecuteUnordered(byte[] command, MessageContext msgCtx) {         
        try {
            // Deserialize input and initialization
            ByteArrayOutputStream out = new ByteArrayOutputStream(4);
            Object deser = new ObjectInputStream(new ByteArrayInputStream(command)).readObject();
            ArrayList<Object> deser_l =  (ArrayList<Object>) deser;
            String operation = (String) deser_l.get(0);
            // Process bank create request
            if (this.bank_public_key == null && operation.equals("bank create")) {
                this.bank_public_key = (PublicKey) deser_l.get(1);
                System.out.println("Bank registered at server");
            }
            // Process bank deposit request
            else if (operation.equals("bank deposit")) {
                String message = (String) deser_l.get(2);
                byte[] signed = (byte[]) deser_l.get(3);    
                // Verify the signature                   
                if (Utils.verify(message, signed, this.bank_public_key) == false) {
                    System.out.println("Signature of deposit not verified");
                    return out.toByteArray();
                }
                String[] split = message.split(" ");
                String id = split[0];
                int amount = Integer.parseInt(split[1]);
                PublicKey pbk = (PublicKey) this.mapping.get(id);
                this.ledger.put(pbk, this.ledger.get(pbk) + amount);
                System.out.println("Bank deposit completed at server");
            }
            return out.toByteArray();
        } catch (Exception ex) {
            System.err.println("Invalid request received!");
            return new byte[0];
        }
    }
  
    // Method which handles all the requests from the coin client
    @Override
    public byte[] appExecuteOrdered(byte[] command, MessageContext msgCtx) {
        try {
            // Deserialize input and initialization
            ByteArrayOutputStream out = new ByteArrayOutputStream(4);
            Object deser = new ObjectInputStream(new ByteArrayInputStream(command)).readObject();
            ArrayList<Object> deser_l =  (ArrayList<Object>) deser;
            String operation = (String) deser_l.get(0);
            PublicKey pbk = (PublicKey) deser_l.get(1);
            // Process client create request
            if (this.ledger.keySet().contains(pbk) == false && operation.equals("client create")) {
                this.ledger.put(pbk, 0);
                this.nonces.put(pbk, 0);
                String cid = (String) deser_l.get(2);
                this.mapping.put(cid, pbk);
                System.out.println("Client registered at server");
            }
            // Process client check balance request
            else if (operation.equals("check balance")) {
                String message = (String) deser_l.get(2);
                byte[] signed = (byte[]) deser_l.get(3);
                String bid = message;
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ObjectOutputStream dos = new ObjectOutputStream(baos);
                ArrayList<Object> wol = new ArrayList<>();
                // Verify the signature                   
                if (Utils.verify(message, signed, (PublicKey) this.mapping.get(bid)) == false) {
                    System.out.println("Signature not verified"); 
                    wol.add((Object) 1);
                    dos.writeObject(wol);
                    return baos.toByteArray();
                } 
                int balance = this.ledger.get(pbk);
                wol = new ArrayList<>();
                wol.add((Object) 0);
                wol.add((Object) balance);
                dos.writeObject(wol);
                System.out.println("Balance checked at server");
                return baos.toByteArray();
            }
            // Process client send money request
            else if (operation.equals("send money")) {
                String message = (String) deser_l.get(2);
                byte[] signed = (byte[]) deser_l.get(3); 
                String[] split = message.split("\\s+");
                String sen_id = split[0];                      
                PublicKey sen_pbk = (PublicKey) this.mapping.get(sen_id);
                // Verify the signature                   
                if (Utils.verify(message, signed, sen_pbk) == false) {
                    System.out.println("Signature not verified");
                    new ObjectOutputStream(out).writeObject(1);
                    return out.toByteArray();
                }
                int nonce = Integer.parseInt(split[1]);
                String rec_id = split[2];
                int amount = Integer.parseInt(split[3]);
                int sen_nonce = this.nonces.get(sen_pbk);
                int sen_balance = this.ledger.get(sen_pbk);
                PublicKey rec_pbk = (PublicKey) this.mapping.get(rec_id);
                // Examine the send money request conditions
                if (sen_pbk.equals(rec_pbk) == false && sen_nonce == nonce && amount > 0 && sen_balance >= amount) {
                    this.ledger.put(sen_pbk, this.ledger.get(sen_pbk) - amount);
                    this.ledger.put(rec_pbk, this.ledger.get(rec_pbk) + amount);
                    this.nonces.put(sen_pbk, this.nonces.get(sen_pbk) + 1);
                    new ObjectOutputStream(out).writeObject(0);
                    System.out.println("Transfer completed at server");
                }
                else {
                    System.out.println("Transfer failed at server"); 
                    new ObjectOutputStream(out).writeObject(1);
                    return out.toByteArray();
                }
            }
            return out.toByteArray();
        } catch (Exception ex) {
            System.err.println("Invalid request received!");
            return new byte[0];
        }
    }

    public static void main(String[] args) throws Exception{
        if(args.length < 1) {
            System.out.println("Use: java CoinServer <processId>");
            System.exit(-1);
        }      
        new CoinServer(Integer.parseInt(args[0]));
    }

    // Unused built in overriden method
    @SuppressWarnings("unchecked")
    @Override
    public void installSnapshot(byte[] state) {
        try {
            ByteArrayInputStream bis = new ByteArrayInputStream(state);
            ObjectInput in = new ObjectInputStream(bis);
            //counter = in.readInt();
            in.close();
            bis.close();
        } catch (IOException e) {
            System.err.println("[ERROR] Error deserializing state: "
                    + e.getMessage());
        }
    }

    // Unused built in overriden method
    @Override
    public byte[] getSnapshot() {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutput out = new ObjectOutputStream(bos);
            //out.writeInt(counter);
            out.flush();
            bos.flush();
            out.close();
            bos.close();
            return bos.toByteArray();
        } catch (IOException ioe) {
            System.err.println("[ERROR] Error serializing state: "
                    + ioe.getMessage());
            return "ERROR".getBytes();
        }
    }
}