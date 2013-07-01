package com.weibo.wesync.authFilter;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.codec.binary.Base64;
import org.apache.log4j.Logger;
import com.weibo.wesync.notify.service.app.AppUtil;
import com.weibo.wesync.notify.utils.Util;
import cn.sina.api.commons.util.JsonWrapper;

public class AuthFilterServlet{
	
	private static long mTokenTimeout = Long.parseLong(Util.getConfigProp("mTokenTimeout","86400000"));
	private static final Logger log = Logger.getLogger(AuthFilterServlet.class);

	public AuthResult authBasic(String authorization,String ip) {
		AuthResult authResult = new AuthResult();
		if(null == authorization){
			log.error("authorization header is null,auth login failed!");
			return authResult;
		}
		if (authorization.startsWith("MAuth")){ // MAuth没有需要进行加密，只在闪断的时候更新
			authResult = mAuth(authResult,authorization,ip);
			if( 901 == authResult.code){
				log.info(authResult.uid +" mauth out of time failed ");
				return authResult;
			}
		}else if(authorization.startsWith("WBasic")){
			authResult = weimiAuth(authResult,authorization,ip);
	 	}else if(authorization.startsWith("MWBasic")){
			authResult = weimiAuth(authResult,authorization,ip);
			if(null == authResult.uid){ // 认证不成功，返回903
				authResult.code = 903;
				return authResult;
			}
	 	}else if (authorization.startsWith("OAuth")){
			authResult = oAuth(authResult,authorization,ip);
	 	}else if (authorization.startsWith("Basic")) {
	 		authResult = basicAuth(authResult,authorization,ip);
	 	}else if (authorization.startsWith("MOAuth")){ // 服务于闪断的meyouToken超时，验证OAuth
	 		authResult = oAuth(authResult,authorization,ip);// 认证成功，更换meyouToken
			if(null == authResult.uid){ // 认证不成功，返回902
				authResult.code = 902;
				return authResult;
			} 		
		}else if (authorization.startsWith("MBasic")){ // 服务于闪断的meyouToken超时，验证MBasic
			authResult = basicAuth(authResult,authorization,ip); // 认证成功，更换meyouToken
			if(null == authResult.uid){ // 认证不成功，返回903
				authResult.code = 903;
				return authResult;
			} 			
		}
		if (null != authResult.uid){
			log.debug("uid:" + authResult.uid+ ",from:"+ip+",auth login successed!"); 
			try {
				authResult.meyouToken = AuthUtil.encryptAes(System.currentTimeMillis()+":"+authResult.uid);
			} catch (Exception e) {
				log.error("encryptAes failed",e);
			}		
		}else{
			authResult.code = 401;
		}
		return authResult;    	     	
	}

	public void init(String privateKeyPath) {
		try {
			AuthUtil.initAuth(privateKeyPath);
		} catch (Exception readPriKeyException) {
			log.error("read private_key failed",readPriKeyException);
		}
	}
	
	private static AuthResult basicAuth(AuthResult authResult,String authorization,String ip){
		authorization = authorization.substring(authorization.indexOf(' ') + 1);
 		byte[] bytes = Base64.decodeBase64(authorization.getBytes());
     	authorization = new String(bytes); // get from Auth header :username:password    	
     	String username = authorization.substring(0, authorization.indexOf(':'));  
     	String password = authorization.substring(authorization.indexOf(':') + 1);  
     	try {	
			byte[] decByte = AuthUtil.decryptRsa(password); 
			password = new String(decByte);	
			authResult.uid = SinaSso.INSTANCE.authUser(username, password, ip); // Sina SSO
		} catch (Exception decryptException) {
			log.error("private_key decrypt failed",decryptException);
		}
     	return authResult;
	}
	
	private static AuthResult oAuth(AuthResult authResult,String authorization,String ip){
		authorization = authorization.substring(authorization.indexOf(' ') + 1);
		byte[] bytes = Base64.decodeBase64(authorization.getBytes());
     	authorization = new String(bytes);
		String oAuth = authorization; // get oauth			
		try {
			byte[] decByte = AuthUtil.decryptRsa(oAuth);
			oAuth = new String(decByte);
			String url = "https://api.weibo.com/oauth2/get_token_info";
			Map<String, String> params = new HashMap<String, String>();
			params.put("access_token",oAuth);
			String ret = AppUtil.getInstance().requestPostUrl(url, null, params);
			JsonWrapper jsonWrapper = new JsonWrapper(ret);
			authResult.uid = jsonWrapper.get("uid");
		} catch (Exception decryptException) {
			log.error("private_key decrypt failed from oauth",decryptException);
		}
     	return authResult;
	}
	
	private static AuthResult mAuth(AuthResult authResult,String authorization,String ip){
		authResult.code = 901;
		String mAuth = authorization.substring(authorization.indexOf(' ') + 1);
		byte[] decByte;
		try {
			decByte = AuthUtil.decryptAes(mAuth); // 解密
			mAuth = new String(decByte);          
			String oldtime = mAuth.substring(0, mAuth.indexOf(':')); // 获得mtoken中的时间
			String uid = mAuth.substring(mAuth.indexOf(':')+1);      // 获得uid
			long nowtime = System.currentTimeMillis();
			long out = nowtime - Long.valueOf(oldtime);
			if(out < mTokenTimeout){  // 如果重复的token没有超时
				authResult.uid = uid;
				authResult.code = 200;	
				log.info(uid+" last login time is "+oldtime+" mauth successed");
			}		
		} catch (Exception decryptException) {
			log.error("meyouToken decrypt failed",decryptException);
		}
     	return authResult;
	}
	
	private static AuthResult weimiAuth(AuthResult authResult,String authorization,String ip){
		authorization = authorization.substring(authorization.indexOf(' ') + 1);
 		byte[] bytes = Base64.decodeBase64(authorization.getBytes());
     	authorization = new String(bytes);
     	String username = authorization.substring(0, authorization.indexOf(':'));  
     	String password = authorization.substring(authorization.indexOf(':') + 1);  
     	try {	
			byte[] decByte = AuthUtil.decryptRsa(password);
			password = new String(decByte);
			StringBuilder requestUrl = new StringBuilder();
			requestUrl.append(Util.getConfigProp("weimiServer","http://10.55.40.66:8888"));
			requestUrl.append("/login");
			Map<String, String> headers = new HashMap<String, String>();
			headers.put("X-Matrix-AppID", "1"); // 发送这条连接的头，保证请求的安全性
			Map<String, String> params = new HashMap<String, String>();	
			params.put("username", username);
			params.put("password", password);
			params.put("type","0");
			String resp = AppUtil.getInstance().requestPostUrl(requestUrl.toString(), headers, params);
			if(null != resp){
				JsonWrapper json = new JsonWrapper(resp);
				String apistatus = json.get("apistatus");
				if("1".equals(apistatus)){
					JsonWrapper result = json.getNode("result");
					authResult.uid = result.get("uid");
					log.info(authResult.uid +" login weimi auth success!");
				}
			}	
		} catch (Exception decryptException) {
			log.error("login weimi private_key decrypt failed",decryptException);
		}
     	return authResult;
	}
}