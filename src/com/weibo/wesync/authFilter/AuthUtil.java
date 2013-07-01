package com.weibo.wesync.authFilter;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.ObjectInputStream;
import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Base64;


public class AuthUtil {

	private static RSAPrivateKey privateKey;
	private static SecretKey aesKey;
	private static String aesKeyStr = "NGQxNmUwMjM4M2Y0MTI2MTM3NDI0Y2MxMjA1N2IyNDM=";
	// for 16 hex char
	private static char[] HEXCHAR = { '0', '1', '2', '3', '4', '5', '6', '7','8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };
	// for aes key generate

	public static void initAuth(String rsaKeyPath) throws Exception{
		privateKey = loadPrivateKey(rsaKeyPath);
		aesKey = loadAesKey();
	}
	
	private static SecretKey loadAesKey() throws Exception{
        String buffer = new String(Base64.decodeBase64(aesKeyStr));
        byte[] keyStr = toBytes(buffer);
        SecretKeySpec aesKey = new SecretKeySpec(keyStr,"AES");
        return aesKey;
	}
	
 	private static RSAPrivateKey loadPrivateKey(String rsaKeyPath) throws Exception{   
 		BufferedReader br= new BufferedReader(new FileReader(rsaKeyPath));  
        String readLine= null;  
        StringBuilder sb= new StringBuilder();  
        while((readLine= br.readLine())!=null){  
        	if(readLine.charAt(0)=='-'){  
        		continue;  
        	}else{  
                sb.append(readLine);  
                sb.append('\r');  
             }  
         }
         byte[] buffer = Base64.decodeBase64(sb.toString());  
         PKCS8EncodedKeySpec keySpec= new PKCS8EncodedKeySpec(buffer);  
         KeyFactory keyFactory= KeyFactory.getInstance("RSA");  
         RSAPrivateKey privateKey= (RSAPrivateKey) keyFactory.generatePrivate(keySpec);  
         br.close();
         return privateKey;
    }  
    
    public static String encryptAes(String msg) throws Exception{
		Cipher ecipher = Cipher.getInstance("AES");
		ecipher.init(Cipher.ENCRYPT_MODE,aesKey);
		return toHexString(ecipher.doFinal(msg.getBytes()));
    }
	    
    public static byte[] decryptAes(String msg) throws Exception{
    	Cipher dcipher = Cipher.getInstance("AES");
    		dcipher.init(Cipher.DECRYPT_MODE,aesKey);
    		return dcipher.doFinal(toBytes(msg));
    }

	public static byte[] decryptRsa(String message) throws Exception {
		Cipher cipher = Cipher.getInstance("RSA");
		cipher.init(Cipher.DECRYPT_MODE, privateKey);
		return cipher.doFinal(toBytes(message));
	}

 	public static Object readFromFile(String fileName) throws Exception {
 		ObjectInputStream input = new ObjectInputStream(new FileInputStream(fileName));
 		Object obj = input.readObject();
 		input.close();
 		return obj;
 	}
 	
 	static final byte[] toBytes(String s) {
 		byte[] bytes;
 		bytes = new byte[s.length() / 2];
 		for (int i = 0; i < bytes.length; i++) {
 			bytes[i] = (byte) Integer.parseInt(s.substring(2 * i, 2 * i + 2),16);
 		}
 		return bytes;
 	}

	public static String toHexString(byte[] b) {
		StringBuilder sb = new StringBuilder(b.length * 2);
		for (int i = 0; i < b.length; i++) {
			sb.append(HEXCHAR[(b[i] & 0xf0) >>> 4]);
			sb.append(HEXCHAR[b[i] & 0x0f]);
		}
		return sb.toString();
	}
}

