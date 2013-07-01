package com.weibo.wesync.notify.service;

import java.util.Date;
import java.util.concurrent.ScheduledExecutorService;

import org.apache.log4j.Logger;

//import cn.sina.api.commons.cache.driver.VikaCacheClient;
import cn.sina.api.data.storage.cache.MemCacheStorage;

public class CacheServiceImpl implements CacheService {
	private static Logger log = Logger.getLogger(CacheServiceImpl.class);
	private long memCacheTimeToLiveMs = 5 * 60 * 1000L;
	private Date dMemCacheTimeToLiveMs = new Date(memCacheTimeToLiveMs);
	
	private static final String SEP_CHAR = ",";
	private MemCacheStorage memCacheStorage;
	private ScheduledExecutorService reloadDeviceExecutor;
	private int dataLoadBatchSize;
	private boolean loadDataOnInit;
	
	public void setMemCacheStorage(MemCacheStorage memCacheStorage) {
		this.memCacheStorage = memCacheStorage;
	}
	
	public void init() {
		// 防止timeout checker
//		reloadDeviceExecutor = Executors.newScheduledThreadPool(1);
//		reloadDeviceExecutor.schedule(new LoadDeviceDataTask(), getNextExcDelay(), TimeUnit.MILLISECONDS);
//		
//		if(loadDataOnInit) {
//			doLoadDeviceData();
//		}
	}

	public void setSession(NotifySession session) {
		String key = session.getUid() + ONLINE_SESSION_SUFFIX;
		memCacheStorage.set(key, session, dMemCacheTimeToLiveMs);
	}
	
	public void removeSession(long uid) {
		String key = uid + ONLINE_SESSION_SUFFIX;
		memCacheStorage.delete(key);
	}
	
	public boolean contains(long uid) {
		String key = uid + ONLINE_SESSION_SUFFIX;
		Object obj = memCacheStorage.get(key);
		log.debug("CacheServiceImpl check contains " + key +"|"+ obj);
		
		if(obj != null) {
			return true;
		}
		
		return false;
	}
	
	public NotifySession getSession(long uid) {
		String key = uid + ONLINE_SESSION_SUFFIX;
		Object obj = memCacheStorage.get(key);
		
		if(obj != null) {
			NotifySession session = (NotifySession) obj;
			return session;
		}
		
		return null;
	}
}

