package com.weibo.wesync.notify.service.app.tauth;

import java.io.FileInputStream;
import java.net.URL;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ScheduledThreadPoolExecutor;

import org.apache.commons.codec.binary.Base64;
import org.apache.log4j.Logger;
import com.weibo.wesync.notify.service.app.AppUtil;
import com.weibo.wesync.notify.service.app.tauth.TAuthStore.TauthToken;
import com.weibo.wesync.notify.utils.Util;

import cn.sina.api.commons.util.JsonWrapper;
/**
 * 
 * @auth jichao1@staff.sina.com.cn 
 */
public class TAuthUtil {
	private static Logger log = Logger.getLogger(TAuthUtil.class);
	private static final String configfile = "meyou_conf.properties";
	private static Properties prop = null;
	private static final ScheduledThreadPoolExecutor timer = new ScheduledThreadPoolExecutor(1);
	
	private String appKey = null;
	private static TAuthUtil instance = null;
	
	public static synchronized TAuthUtil getInstance() {
		if(instance == null)
			instance  = new TAuthUtil();
		
		return instance;
	}
	
	public String getAppKey() {
		return appKey;
	}

	private TAuthUtil() {
		FileInputStream in = null;
		prop = new Properties();
		
		try {
			URL url = TAuthUtil.class.getClassLoader().getResource(configfile);
			in = new FileInputStream(url.getFile());
			prop.load(in);
		}
		catch(Exception e) {
			log.error(e.getMessage(), e);
		}
		finally {
			try {
				in.close();
			} 
			catch (Exception e) {
				// ignore
			}
		}
		
		String needtauth = Util.getConfigProp("needtauth", "true");
		
		if("true".equalsIgnoreCase(needtauth)) {
			TAuthStore.init();
			refreshTauthToken();
		
			timer.scheduleWithFixedDelay(new Runnable() {
				public void run() {
					refreshTauthToken();
				}
			}, 8, 8, java.util.concurrent.TimeUnit.HOURS);
		}
	}
	
	private void refreshTauthToken() {
		if (log.isInfoEnabled())
			log.info("reloadTauthToken begin ...");
		
		List<TauthToken> list = TAuthStore.getTokens();
		
		for(TauthToken tt : list) {
			String date = tt.date; 
			String today = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
			String refreshTokenUrl = null;
			
			if (!today.equals(date)) {
				String ret = null;
				try {
					log.info("refreshTauthTokenFromApi begin ...");
					Map<String,String> params = new HashMap<String,String>(); // 增加泛型 liuzhao
					params.put("app_secret", tt.secret);
					refreshTokenUrl = prop.getProperty("refreshTokenUrl", 
							"http://i.api.weibo.com/auth/tauth_token.json?source=" + tt.source);
					ret = AppUtil.getInstance().requestPostUrl(refreshTokenUrl, null, params);
					
					// retry
					if (ret == null || ret.trim().isEmpty())
						ret = AppUtil.getInstance().requestPostUrl(refreshTokenUrl, null, params);
					
					JsonWrapper json = new JsonWrapper(ret.trim());
					String newToken = null;
					
					if ((newToken = json.get("tauth_token"))!= null && !newToken.trim().isEmpty())
					{
						newToken = newToken.trim();
						tt.token = newToken;
						log.info("refreshTauthTokenFromApi end! token change! source:" + tt.source + ", new token:" + newToken);
					} 
					else {
						String info = "refreshTauthTokenFromApi error! ret:" + ret + ", url:" + refreshTokenUrl; 
						log.error(info);
					}
				} 
				catch (Exception e) {
					String info = "refreshTauthTokenFromApi error! ret:" + ret + ", url:" + refreshTokenUrl; 
					log.error(info, e);
				}
			}
		}
		
		TAuthStore.saveToken(list);
	}
	
	private static ThreadLocal<MessageDigest> MD5 = new ThreadLocal<MessageDigest>() {
		@Override
		protected MessageDigest initialValue() {
			try {
				return MessageDigest.getInstance("MD5");
			} catch (Exception e) {
			}
			return null;
		}
	};
	
	private static String md5(String s) throws Exception {
		return  md5Digest((s == null ? "" : s).getBytes());
	}
	
	private static String md5Digest(byte[] data) throws Exception {
		MessageDigest md5 = MD5.get();
		md5.reset();
		md5.update(data);
		byte[] digest = md5.digest();
		return encodeHex(digest);
	}
	
	private static String encodeHex(byte[] bytes) {
		StringBuilder buf = new StringBuilder(bytes.length + bytes.length);
		for (int i = 0; i < bytes.length; i++) {
			if (((int) bytes[i] & 0xff) < 0x10) {
				buf.append("0");
			}
			buf.append(Long.toString((int) bytes[i] & 0xff, 16));
		}
		return buf.toString();
	}
	
	public String getToken(String uid, String source) {
		String tokenStr = TAuthStore.getToken(source);
		log.info("tokenStr:"+tokenStr);
		String token = null;
		
		try {
			String authStr = uid + ":" + md5(uid + tokenStr.trim());
			token = "Token " + new String(Base64.encodeBase64(authStr.getBytes("utf-8")), "utf-8");
			log.info("token:"+token);
		}
		catch(Exception e) {
			log.error("get token failed caused by " + e.getMessage());
		}
		
		return token.trim();
	}
}
