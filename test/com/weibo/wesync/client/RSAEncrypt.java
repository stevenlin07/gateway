package com.weibo.wesync.client;

import java.io.BufferedReader;  
import java.io.File;  
import java.io.FileNotFoundException;  
import java.io.FileReader;  
import java.io.IOException;  
import java.security.InvalidKeyException;  
import java.security.KeyFactory;  
import java.security.KeyPair;  
import java.security.KeyPairGenerator;  
import java.security.NoSuchAlgorithmException;  
import java.security.SecureRandom;  
import java.security.interfaces.RSAPrivateKey;  
import java.security.interfaces.RSAPublicKey;  
import java.security.spec.InvalidKeySpecException;  
import java.security.spec.PKCS8EncodedKeySpec;  
import java.security.spec.X509EncodedKeySpec;  
  
import javax.crypto.BadPaddingException;  
import javax.crypto.Cipher;  
import javax.crypto.IllegalBlockSizeException;  
import javax.crypto.NoSuchPaddingException;  

import org.apache.commons.codec.binary.Base64;
  
  
public class RSAEncrypt {  
    
	private String password;
	private String keyPath;
	
	// for 16 hex char
	private static char[] HEXCHAR = { '0', '1', '2', '3', '4', '5', '6', '7','8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };
	
	/** 
     * 从文件中加载公钥 
     * @param keyFileName 公钥文件名 
     * @return 是否成功 
     * @throws Exception  
     */  
    public static RSAPublicKey loadPublicKey(String keyFileName) throws Exception{  
    	String readLine= null;   
    	BufferedReader br= new BufferedReader(new FileReader(keyFileName));      
        StringBuilder sb= new StringBuilder();  
        while((readLine= br.readLine())!=null && !"".equals(readLine)){  
        	if(readLine.charAt(0)=='-'){  
        		continue;  
            }else{  
                sb.append(readLine).append('\r');  
            }  
        }  
        byte[] buffer= Base64.decodeBase64(sb.toString());  
        KeyFactory keyFactory= KeyFactory.getInstance("RSA");  
        X509EncodedKeySpec keySpec= new X509EncodedKeySpec(buffer);  
        RSAPublicKey publicKey = (RSAPublicKey) keyFactory.generatePublic(keySpec);
        br.close();
        return publicKey;
     }  
  
    /** 
     * 从文件中加载私钥 
     * @param keyFileName 私钥文件名 
     * @return 是否成功 
     * @throws Exception  
     */  
    public static RSAPrivateKey loadPrivateKey(String keyFileName) throws Exception{   
            File keyFile= new File(keyFileName);  
            BufferedReader br= new BufferedReader(new FileReader(keyFile));  
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
            byte[] buffer= Base64.decodeBase64(sb.toString());  
            PKCS8EncodedKeySpec keySpec= new PKCS8EncodedKeySpec(buffer);  
            KeyFactory keyFactory= KeyFactory.getInstance("RSA");  
            RSAPrivateKey privateKey= (RSAPrivateKey) keyFactory.generatePrivate(keySpec);  
            br.close();
            return privateKey;
    }  
  
    /** 
     * 加密过程 
     * @param publicKey 公钥 
     * @param plainTextData 明文数据 
     * @return 
     * @throws Exception 加密过程中的异常信息 
     */  
    public static byte[] encrypt(RSAPublicKey publicKey, byte[] plainTextData) throws Exception{  
        Cipher cipher= null;    
        cipher= Cipher.getInstance("RSA");  
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);  
        byte[] output= cipher.doFinal(plainTextData);  
        return output;  
    }  
  
    /** 
     * 解密过程 
     * @param privateKey 私钥 
     * @param cipherData 密文数据 
     * @return 明文 
     * @throws Exception 解密过程中的异常信息 
     */  
    public static byte[] decrypt(RSAPrivateKey privateKey, byte[] cipherData) throws Exception{  
        Cipher cipher= null;  
        cipher= Cipher.getInstance("RSA");  
        cipher.init(Cipher.DECRYPT_MODE, privateKey);  
        byte[] output= cipher.doFinal(cipherData);  
        return output;         
    }  
  

    /** 
     * 字节数据转十六进制字符串 
     * @param data 输入数据 
     * @return 十六进制内容 
     */  
	public static String toHexString(byte[] b) {
		StringBuilder sb = new StringBuilder(b.length * 2);
		for (int i = 0; i < b.length; i++) {
			sb.append(HEXCHAR[(b[i] & 0xf0) >>> 4]);
			sb.append(HEXCHAR[b[i] & 0x0f]);
		}
		return sb.toString();
	} 
}  

