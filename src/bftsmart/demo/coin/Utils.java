package bftsmart.demo.coin;

import java.io.*;
import java.util.*;
import java.security.*;
import javax.crypto.*;

/**
 * Utils for signing and verifying security
 * 
 * @author Weimeng Pu
 */
public class Utils {

    // Sign the message with private key
    public static byte[] sign(String plainText, PrivateKey privateKey) throws Exception {
        
        Signature privateSignature = Signature.getInstance("SHA256withRSA");
        privateSignature.initSign(privateKey);
        privateSignature.update(plainText.getBytes());

        byte[] signature = privateSignature.sign();

        return signature;
    }

    // Verify the signature of the message with public key
    public static boolean verify(String plainText, byte[] signature, PublicKey publicKey) throws Exception {
        
        Signature publicSignature = Signature.getInstance("SHA256withRSA");
        publicSignature.initVerify(publicKey);
        publicSignature.update(plainText.getBytes());

        return publicSignature.verify(signature);
    }
}