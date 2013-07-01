package com.weibo.wesync.notify.utils;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.LinkedBlockingQueue;

import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.mortbay.log.Log;

import com.google.protobuf.Message;
import com.weibo.wesync.WeSyncURI;
import com.weibo.wesync.notify.protocols.Meyou;
/**
 * 
 * @auth jichao1@staff.sina.com.cn 
 */
public class Util {
	private static Logger log = Logger.getLogger("Util.class");
	private static String localip;
	private static Properties configProp = null;
	private static Thread configLoader = null;
	
	public static void writeSafeUtf8(ObjectOutput out, String s) throws IOException {
		if (s == null) {
			out.writeShort(-1);
		} else {
			byte[] bb = s.getBytes("utf-8");
			out.writeShort(bb.length);
			out.write(bb);
		}
	}
	
	public static String readSafeUtf8(ObjectInput in) throws IOException {
		int len = in.readShort();
		if (len < 0)
			return null;
		
		byte[] bb = new byte[len];
		in.read(bb);
		return new String(bb, "utf-8");
	}
	
	public static void main(String args[]) {
		try {
			String s = InetAddress.getLocalHost().getHostAddress();
			System.out.println(s);
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public static void writePacketLen(ByteArrayOutputStream out, int len) {
		out.write((byte) (len >> 24));
		out.write((byte) (len >> 16));
		out.write((byte) (len >> 8));
		out.write((byte) (len));
	}
	
	public static int readPacketLength(byte[] in) {
		int ch1 = in[0];
        int ch2 = in[1];
        int ch3 = in[2];
        int ch4 = in[3];
        if ((ch1 | ch2 | ch3 | ch4) < 0)
            return -1;
        return ((ch1 << 24) + (ch2 << 16) + (ch3 << 8) + (ch4 << 0));
	}
	
	public static Meyou.MeyouPacket getMeyouPacket(HttpServletRequest request) throws Exception {
		int len = request.getContentLength();
		byte[] buf = new byte[len];
		ServletInputStream sis = request.getInputStream();
		int readBytes = sis.read(buf, 0, len);
	    if(readBytes != len) {
	    	throw new Exception("Oops! readBytes!=len");
	    }
	    Meyou.MeyouPacket meyouPacket = Meyou.MeyouPacket.parseFrom(buf);
	    return meyouPacket;
	}
	
	public static void doDeliver(HttpServletResponse response, LinkedBlockingQueue messages) throws IOException {
		Object message = messages.poll();
		ByteArrayOutputStream outstream = new ByteArrayOutputStream();
		
		while(message != null) {
			byte[] out = getOutputContent(message);
			Util.writePacketLen(outstream, out.length);
			outstream.write(out);
			message = messages.poll();
		}
		
		byte[] respbody = outstream.toByteArray();
		response.getOutputStream().write(respbody);
		response.getOutputStream().flush();
	}
	
	public static void doDeliver(HttpServletResponse response, List messages) throws IOException {
		ByteArrayOutputStream outstream = new ByteArrayOutputStream();
		
		for(Object message : messages) {
			byte[] out = getOutputContent(message);
			Util.writePacketLen(outstream, out.length);
			outstream.write(out);
		}
		
		byte[] respbody = outstream.toByteArray();
		response.getOutputStream().write(respbody);
		response.getOutputStream().flush();
	}
		
	private static byte[] getOutputContent(Object msg) {
		if(msg instanceof String) {
			return ((String) msg).getBytes();
		}
		if(msg instanceof Message) {
			return ((Message) msg).toByteArray();
		}
		
		return new byte[0];
	}
	
	public static WeSyncURI getWeSyncURI(com.weibo.wesync.Command cmd){
		WeSyncURI uri = new WeSyncURI();
		uri.protocolVersion = 10;
		uri.guid = "serverguid";
		uri.deviceType = "server";		
		uri.command = cmd.toByte();
		return uri;
	}
	
	public static String getLocalIp() throws UnknownHostException {
		if(localip == null) {
			localip = InetAddress.getLocalHost().getHostAddress();
		}
		
		return localip;
	}
	
	public static Integer getConfigProp(String key, Integer defaultVal) {
		String configVal = getConfigProp(key, "");
		
		try {
			return "".equals(configVal) ? defaultVal : Integer.parseInt(configVal);
		}
		catch(Exception e) {
			log.error("getconfigProp as integer failed caused by " + e);
		}
		
		return defaultVal;
	}
	
	public static String getConfigProp(String key, String defaultVal) {
		if(configProp == null) {
			synchronized(Util.class) {
				if(configProp == null) {
					loadConfig();
					
					configLoader = new Thread(new Runnable() {
						public void run() {
							while(true) {
								loadConfig();
								
								try {
									// reload every 5 min
									Thread.sleep(5 * 60 * 1000);
								}
								catch(Exception e) {
									Log.warn("config loader failed caused by " + e.getMessage(), e);
								}
							}
						}
					});
					
					configLoader.start();
				}
			}
		}
		
		String val = configProp.getProperty(key);
		return val == null ? defaultVal : val;
	}

	private static void loadConfig() {
		FileInputStream in = null;
		
		try {
			URL url = Util.class.getClassLoader().getResource("meyou_conf.properties");
			in = new FileInputStream(url.getFile());
			configProp = new Properties();
			configProp.load(in);
		}
		catch(Exception e) {
			Log.warn(e.getMessage(), e);
			configProp = null;
		}
		finally {
			if(in != null) {
				try {
					in.close();
				}
				catch(Exception e) {// ignore
				}
			}
		}
	}
	
	public static String getLocalInternalIp() {
		String localIp = null;
		try {
			Enumeration<NetworkInterface> allNetInterfaces = NetworkInterface.getNetworkInterfaces();
			InetAddress inet = null;
			while (allNetInterfaces.hasMoreElements()) {
				NetworkInterface netInterface = (NetworkInterface) allNetInterfaces.nextElement();
				Enumeration<InetAddress> addresses = netInterface.getInetAddresses();
				while (addresses.hasMoreElements()) {
					inet = (InetAddress) addresses.nextElement();
					if (inet.isSiteLocalAddress() && !inet.isLoopbackAddress()  
							&& inet.getHostAddress().indexOf(":") == -1) {
						localIp = inet.getHostAddress();
						break;
					}
				}
			}
		} catch (SocketException e) {
		}
		return localIp;
	}
}
