package com.weibo.wesync.gateway.business;

import org.apache.log4j.Logger;

import cn.sina.api.commons.util.JsonWrapper;

import com.weibo.wejoy.WeijuInstance;
import com.weibo.wejoy.wesync.listener.GroupChatService;

public class WesyncListenerBusiness4Wejoy extends WesyncListenerBusiness {
	private static Logger log = Logger.getLogger(WesyncListenerBusiness4Wejoy.class);
	private WeijuInstance weijuInstance;
	
	public WesyncListenerBusiness4Wejoy(GroupChatService groupChatService) {
		this.wesyncListener = groupChatService;
		this.weijuInstance = WeijuInstance.getInstance(groupChatService);
	}

	public String process(String fromuid, String remoteIp, JsonWrapper json, byte[] attach) {
		String result = weijuInstance.process(fromuid, remoteIp, json, attach);
		if (null == result) {
			result = "{\"code\":404,\"text\":\"WesyncListenerBusiness4Wejoy service is not ready\"}";
		}
		log.info("plugin service resp:" + result);
		return result;
	}

	
}
