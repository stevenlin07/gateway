package com.weibo.wesync.notify;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;


/*
 * @ClassName: RsaUtil 
 * @Description: 认证加密管理工具
 * @author LiuZhao
 * @date 2012-9-6 上午11:24:18  
 */
class RsaTool {

    private char[] HEXCHAR = { '0', '1', '2', '3', '4', '5', '6', '7','8', '9', 'a', 'b', 'c', 'd', 'e', 'f' }; 
    private RsaTool(){
	
    }
   
    // 对密码使用RSA公钥进行加密
    public static String encryptPassword(String password) throws Exception{
	RsaTool rsaTool = new RsaTool();	
	RSAPublicKey publicKey = rsaTool.loadPublicKey();	 
	return rsaTool.encrypt(password.getBytes(),publicKey);
    }

    private RSAPublicKey loadPublicKey() throws Exception{  
	InputStream is = this.getClass().getClassLoader().getResourceAsStream("public.pem");
	BufferedReader br= new BufferedReader(new InputStreamReader(is)); 
	String readLine= null;   
        StringBuilder sb= new StringBuilder();  
        try {
	    while((readLine= br.readLine())!=null){  
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
	    return publicKey;
	} catch (IOException e) {
	    throw new Exception("公钥文件不存在",e);
	} catch (NoSuchAlgorithmException e) {
	    throw new Exception("加密算法不存在",e);
	} catch (InvalidKeySpecException e) {
	    throw new Exception("公钥为空",e);
	} finally{
	    try {
		br.close();
		is.close();
	    } catch (IOException e) {
		throw new Exception("加密文件关闭错误",e);
	    }	    
	}
     } 
  
    private String encrypt(byte[] plainTextData,RSAPublicKey publicKey) throws Exception{  
        Cipher cipher= null;    
        try {
            String str = "RSA/ECB/PKCS1Padding";     
	    cipher= Cipher.getInstance(str);
	    cipher.init(Cipher.ENCRYPT_MODE, publicKey);  
	    byte[] output= cipher.doFinal(plainTextData);  
	    return toHexString(output); 
	} catch (NoSuchAlgorithmException e) {
	    e.printStackTrace();
	    throw new Exception("加密算法不存在",e);
	} catch (NoSuchPaddingException e) {
	    throw new Exception("加密的填充机制不存在",e);
	} catch (InvalidKeyException e) {
	    throw new Exception("公钥为空",e);
	} catch (IllegalBlockSizeException e) {
	    throw new Exception("数据输入非法",e);
	} catch (BadPaddingException e) {
	    throw new Exception("填充机制错误",e);
	}   
    }  
  
    /*
     * @Title: toHexString 
     * @Description: 将字节数组转换成十六进制字符串
     * @param b
     * @return String
     */
    private String toHexString(byte[] b) {
	StringBuilder sb = new StringBuilder(b.length * 2);
	for (int i = 0; i < b.length; i++) {
	    sb.append(HEXCHAR[(b[i] & 0xf0) >>> 4]);
	    sb.append(HEXCHAR[b[i] & 0x0f]);
	}
	return sb.toString();
    } 
}
