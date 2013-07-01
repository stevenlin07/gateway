package com.weibo.meyou.perf.sdk.message;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.mina.common.IoSession;


/*
 * @ClassName: NotifyCenter 
 * @Description: SDK对客户端的通知中心
 * 这个中心可能吐出来的消息类型
 * 上层客户端可以通过msgId和type来区分通知的类型
 * @author LiuZhao
 * @date 2012-9-10 上午1:21:13  
 */
public class NotifyCenterPerf {
    public static Map<Integer, IoSession> sessionMap = new ConcurrentHashMap<Integer, IoSession>();
    
    public static void closeNotifyCenter(){
    	sessionMap.clear();
    }
}
