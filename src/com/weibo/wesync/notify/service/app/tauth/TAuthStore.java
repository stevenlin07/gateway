package com.weibo.wesync.notify.service.app.tauth;

import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import com.weibo.wesync.notify.xml.XmlUtil;

/**
 * 
 * @auth jichao1@staff.sina.com.cn 
 */
public class TAuthStore {
	private static Logger log = Logger.getLogger(TAuthStore.class);
	
	private static ConcurrentHashMap<String, TauthToken> tokenMap = new ConcurrentHashMap<String, TauthToken>();
	private static SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
	private static final String fname = "meyou_tauth.xml";	
	public static final String TAUTH_TOKEN_KEY = "tauth_token";
	public static final String TAUTH_TOKEN_DATE = "tauth_token_date";
	private static final String TAUTH_SECRET = "tauth_secret";
	private static final String BR = "\r\n";
	protected static Document document;
	
	public static synchronized void init() {
		SAXReader xmlReader = new SAXReader();
		
		try {
			InputStream is = TAuthStore.class.getClassLoader().getResourceAsStream(fname);
			document = xmlReader.read(is);
			
			@SuppressWarnings("unchecked")
			List<Element> list = (List<Element>) XmlUtil.getChildElementsByName(document.getRootElement(), "token_store");
			
			for(Element token_store : list) {
				TauthToken tauthToken = new TauthToken();
				tauthToken.source =  XmlUtil.getAttByName(token_store, "source");
				tauthToken.desc = XmlUtil.getAttByName(token_store, "desc");
				
				Element tauthTokenDateElem = XmlUtil.getElementByName(token_store, TAUTH_TOKEN_DATE);
				tauthToken.date = tauthTokenDateElem.getText();
				
				Element tauthTokenElem = XmlUtil.getElementByName(token_store, TAUTH_TOKEN_KEY);
				tauthToken.token = tauthTokenElem.getText();
				
				Element tauthSecretElem = XmlUtil.getElementByName(token_store, TAUTH_SECRET);
				tauthToken.secret = tauthSecretElem.getText();
				
				tokenMap.put(tauthToken.source, tauthToken);
			}
		}
		catch(Exception e) {
			log.error(e.getMessage(), e);
		}
		finally {
		}				 
	}
	
	public static synchronized void saveToken(List<TauthToken> list) {
		for(TauthToken tt : list) {
			TauthToken tt0 = tokenMap.get(tt.source);
			
			if(tt0 == null) {
				log.warn("Don't find specified source=" + tt.source);
				tt0 = new TauthToken();
				tt0.desc = "unknown";
				tt0.source = tt.source;
				tokenMap.put(tt0.source, tt0);
			}
			
			tt0.token = tt.token;
			tt0.date = sdf.format(new Date());
		}
				
		saveToken0();
	}
	
	public static synchronized void saveToken(String source, String token) {
		TauthToken tt = tokenMap.get(source);
		
		if(tt == null) {
			init();
			tt = tokenMap.get(source);
			
			if(tt == null) {
				log.warn("Don't find specified source=" + source);
				tt = new TauthToken();
				tt.desc = "unknown";
				tt.source = source;
				tokenMap.put(source, tt);
			}
		}
		
		tt.token = token;
		tt.date = sdf.format(new Date());
				
		saveToken0();
	}
	
	public static void saveToken0() {
		StringBuilder sb = new StringBuilder();
		sb.append("<meyou_tauth>").append(BR);
		
		for(String key : tokenMap.keySet()) {
			TauthToken tt = tokenMap.get(key);
			sb.append("<token_store source=\"").append(tt.source).append("\" desc=\"").append(tt.desc).append("\">").append(BR);
			sb.append("<tauth_token_date>").append(tt.date).append("</tauth_token_date>").append(BR);
			sb.append("<tauth_token>").append(tt.token).append("</tauth_token>").append(BR);
			sb.append("<tauth_secret>").append(tt.secret).append("</tauth_secret>").append(BR);
			sb.append("</token_store>").append(BR);
		}
		
		sb.append("</meyou_tauth>").append(BR);
		RandomAccessFile raf = null;
		
		try {
			URL url = TAuthStore.class.getClassLoader().getResource(fname);
			raf = new RandomAccessFile(url.getPath(), "rw");
			raf.setLength(0);
			raf.write(sb.toString().getBytes());
		}
		catch(Exception e) {
			log.warn(e.getMessage(), e);
		}
		finally {
			if(raf != null) {
				try {
					raf.close();
				} catch (IOException e) {
					log.warn(e.getMessage(), e);
				}
			}
		}
	}
	
	public static synchronized String getToken(String source) {
		TauthToken token = tokenMap.get(source);
		
		if(token == null) {
			init();
			token = tokenMap.get(source);
		}
		
		return token == null ? null : token.token;
	}
	
	public static synchronized List<TauthToken> getTokens() {
		List<TauthToken> list = new ArrayList<TauthToken>();
		
		for(Map.Entry<String, TauthToken> entry: tokenMap.entrySet()) {
			list.add(entry.getValue());
		}
		
		return list;
	}
	
	public static class TauthToken {
		String date;
		String token;
		String source;
		String desc;
		String secret;
	}
	
	public static void main(String argsp[]) {
		saveToken("2841080378", "343151");
	}
}
