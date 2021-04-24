package net.dodian.utilities;

import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.RSAPrivateKeySpec;
import java.security.spec.RSAPublicKeySpec;

public class GenKeys {
    public static BigInteger serverModulus;
    public static BigInteger serverExponent;
    public static BigInteger clientModulus;
    public static BigInteger clientExponent;

	public static void genRSA(){
        try {
            KeyFactory factory = KeyFactory.getInstance("RSA");
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
            keyGen.initialize(1024);
            KeyPair keypair = keyGen.genKeyPair();
            PrivateKey privateKey = keypair.getPrivate();
            PublicKey publicKey = keypair.getPublic();
           
            RSAPrivateKeySpec privSpec = factory.getKeySpec(privateKey, RSAPrivateKeySpec.class);
                       
            RSAPublicKeySpec pubSpec = factory.getKeySpec(publicKey, RSAPublicKeySpec.class);
           
            serverModulus=privSpec.getModulus();
            serverExponent = privSpec.getPrivateExponent();
            KeyServer.Modulus=pubSpec.getModulus();
            KeyServer.Exponent = pubSpec.getPublicExponent();
            System.out.println("SM: "+serverModulus);
            System.out.println("SE: "+serverExponent);
 
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

}
