package com.weibo.wesync.notify.service.app;


/**
 * @ClassName: ServerType 
 * @Description: 服务类型，用来识别服务
 * @author liuzhao
 * @date 2013-4-17 下午4:37:15 
 */
public enum ServerType {

	/** 微米小平台服务*/
	weimiPlatform(1), 
	/** 微米活动服务器*/
	weimiActivity(2), 
	/** 微博接口代理*/
	weiboPlatform(3), 
	/** 用来满足内部服务*/
	intraPlatform(4), 
	unknown(99);

	private final int value;

	private ServerType(int value) {
		this.value = value;
	}

	public int get() {
		return value;
	}

	public static ServerType valueOf(int value) {
		for (ServerType type : ServerType.values()) {
			if (value == type.value)
				return type;
		}
		return unknown;
	}
}
