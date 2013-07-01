package com.weibo.wesync.notify.service;

import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;

import com.weibo.wejoy.data.storage.MemCacheStorage;
import com.weibo.wesync.notify.utils.ConfigConstants;
import com.weibo.wesync.notify.utils.Util;

/**
 * 
 * @auth jichao1@staff.sina.com.cn 
 */
public class NotifySessionManager {
	private static Logger log = Logger.getLogger(NotifySessionManager.class);
	private MemCacheStorage noticeGWMemCache;
	private ConcurrentHashMap<String, NotifySession> sessions = new ConcurrentHashMap<String, NotifySession>();
	private long default_sess_timeout = 300 * 1000L; // 5 mins
	private String localIP = null;
	
	public void init() {
		new TimeoutChecker().start();
		new TimeoutChecker().start();
		this.localIP = Util.getLocalInternalIp();
	}
	
	public String getLocalIp() {
		return localIP;
	}
	
	public MemCacheStorage getNoticeGWMemCache() {
		return noticeGWMemCache;
	}

	public void setNoticeGWMemCache(MemCacheStorage noticeGWMemCache) {
		this.noticeGWMemCache = noticeGWMemCache;
	}

	public void setSession(NotifySession sess) {
		sessions.put(sess.getUid(), sess);
		long t1 = System.currentTimeMillis();
		noticeGWMemCache.set(String.valueOf(sess.getUid()), sess.getExternalBytes());
		long t2 = System.currentTimeMillis();
		
		if(t2 - t1 > Util.getConfigProp(ConfigConstants.MC_OP_SLOW, 50)) {
			log.warn(sess.getUid() + " set session is slow, using " + (t2 - t1));
		}
	}
	
	public NotifySession getLocalSession(String uid) { 
		return sessions.get(uid);
	}
	
	public NotifySession getRemoteSession(String uid) {
		NotifySession sess = null;
		
		if ((sess = sessions.get(uid)) == null) {
			byte[] cs = (byte[]) noticeGWMemCache.get(uid);
			if(cs != null) {
				sess = new NotifySession();
				sess.setExternalBytes(cs);
			}
		}
		return sess;
	}
	/**
	 * remove session only and if only ip stored in mc same with local ip
	 */
	public void removeSession(String uid) {
		NotifySession localsess = sessions.remove(uid);
		
		// 想同时close IoSession，错了，当客户端主动关闭session时还希望得到服务端回包，因此这里不要主动关闭IoSession，
		// 超时检查程序会处理orphan IoSession的
		// if(localsess != null) {localsess.ioSession.close();}
		
		//delete mc object only and if only ip stored in mc is same with this server
		byte[] cs = (byte[]) noticeGWMemCache.get(uid);
		if(cs != null) {
			NotifySession sess = new NotifySession();
			sess.setExternalBytes(cs);
			
			if (sess.getPushServerIp().equals(this.localIP)){
				noticeGWMemCache.delete(uid);
				log.debug("remote session " + sess + " is removed caused by timeout");
			}
			else {
				log.debug("remote session " + sess + " is not removed because it's " +
					"conflict with localsess: " + localsess);
			}
		}
	}
	
	/**
	 * remove session without checking whether ip stored in mc same with local ip
	 */
	public void removeRemoteSession(String uid){
		noticeGWMemCache.delete(uid);
	}

	private class TimeoutChecker extends Thread {
		public void run() {
			while(true) {
				try {
					Thread.sleep(500L);
					int sessTimeout = Integer.parseInt(Util.getConfigProp("gw_sess_timeout", 
							String.valueOf(default_sess_timeout)));
					Iterator keys = sessions.keySet().iterator();
					long now = System.currentTimeMillis();
					
					while(keys.hasNext()) {
						String uid = (String) keys.next();
						
						synchronized(uid.intern()) {
							NotifySession sess = sessions.get(uid);
							
							if(now - sess.getTimestamp() > sessTimeout) {
								if (log.isDebugEnabled()) {
									log.debug(sess.getUid()+ " session timeout " + sess);
								}
								try {
									removeSession(uid);
									sess.ioSession.close();
									log.info("TimeoutChecker close session " + sess + " caused by timeout");
								} catch (Exception e) {
									log.warn("TimeoutChecker close session failed caused by " + e.getMessage(), e);
								}
							}
						}
					}
				} catch (Exception e) {
					log.warn(e.getMessage(), e);
				}
			}
		}
	}
}
