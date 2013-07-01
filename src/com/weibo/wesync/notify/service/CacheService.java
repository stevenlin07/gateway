package com.weibo.wesync.notify.service;

public interface CacheService {
	// mc suffix def
	public static final String ONLINE_SESSION_SUFFIX = ".os";
	
	void setSession(NotifySession session);
	
	void removeSession(long uid);
	
	boolean contains(long uid);
	
	NotifySession getSession(long uid);
}