package com.weibo.wesync.authFilter;

import java.net.URLEncoder;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import com.weibo.wesync.notify.service.app.AppUtil;
import com.weibo.wesync.notify.utils.Util;

public enum SinaSso {

	INSTANCE;
	
	private String bssoPin = Util.getConfigProp("bssoPin","70269226506e773f52a5ed9471f1691c");
	private String bssoUrlPath = Util.getConfigProp("bssoUrlPath","http://ilogin.sina.com.cn/api/chksso.php?entry=shouji");
	private static final Logger log = Logger.getLogger(SinaSso.class);
	
	
	public String authUser(String username, String password, String ip) {
		String urlPath = null;
		String result = null;
		try {
			String md5 = md5Digest(new StringBuilder(username).append(password)
					.append(ip).append(bssoPin).toString().getBytes("GBK"));
			StringBuilder sb = new StringBuilder();
			sb.append(bssoUrlPath);
			sb.append("&user=").append(URLEncoder.encode(username, "GBK"));
			sb.append("&ip=").append(URLEncoder.encode(ip, "utf8"));
			sb.append("&m=").append(URLEncoder.encode(md5, "utf8"));
			Map<String, String> postParams = new HashMap<String, String>();
			postParams.put("pw", password);

			urlPath = sb.toString();
			result = AppUtil.getInstance().requestPostUrl(urlPath, null, postParams);
			
			log.warn(result);
			if ("succ".equals(getFieldValue(result, "result"))) {
				String uid = getFieldValue(result, "uniqueid");
				return (uid != null ? uid : username);
			}
		} catch (Exception e) {
			log.warn("authUser error!", e);
		}

		if (log.isInfoEnabled()) {
			String msg = new StringBuilder().append("authUser fail, username:")
					.append(username).append(", password:").append(password)
					.append(", ip:").append(ip).append(", urlPath:")
					.append(urlPath).append(", result:").append(result)
					.toString();
			log.info(msg);
		}

		return null;
	}

	public static String md5Digest(byte[] data) throws Exception {
		MessageDigest md5 = MD5.get();
		md5.reset();
		md5.update(data);
		byte[] digest = md5.digest();
		return encodeHex(digest);
	}

	public static String encodeHex(byte[] bytes) {
		StringBuilder buf = new StringBuilder(bytes.length + bytes.length);
		for (int i = 0; i < bytes.length; i++) {
			if (((int) bytes[i] & 0xff) < 0x10) {
				buf.append("0");
			}
			buf.append(Long.toString((int) bytes[i] & 0xff, 16));
		}
		return buf.toString();
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
	
	public static String getFieldValue(String s, String field) {
		if (s == null)
			return null;
		
		int pos1 = s.indexOf(field);
		if (pos1 >= 0) {
			int len = field.length() + 1;
			int pos2 = s.indexOf('&', pos1 + len);
			if (pos2 > pos1)
				return EscInvalidXmlChar(s.substring(pos1 + len, pos2));
			else
				return EscInvalidXmlChar(s.substring(pos1 + len));
		} 

		return null;
	}
	
	public static String EscInvalidXmlChar(String s) {
		if (s == null)
			return null;
		
		boolean valid = true;
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			if (c < 0x20 && c != '\t' && c != '\n' && c !='\r') {
				valid = false;
				break;
			}
		}
		if (valid)
			return s;
		
		StringBuilder sb = new StringBuilder(s);
		for (int i = 0; i < sb.length(); i++) {
			char c = sb.charAt(i);
			if (c < 0x20 && c != '\t' && c != '\n' && c !='\r') {
				sb.setCharAt(i, ' ');
			}
		}
		return sb.toString();
	}
}
