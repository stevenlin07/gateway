package com.weibo.wesync.gateway.business;

import cn.sina.api.commons.util.JsonWrapper;

import com.weibo.wesync.CommandListener;

public abstract class WesyncListenerBusiness {

	public CommandListener wesyncListener;
	
	public abstract String process(String fromuid,String remoteIp,JsonWrapper json,byte[] attach);
}
