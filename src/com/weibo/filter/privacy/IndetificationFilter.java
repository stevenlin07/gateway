package com.weibo.filter.privacy;

import org.apache.log4j.Logger;

import cn.sina.api.commons.util.JsonWrapper;

import com.weibo.wesync.data.WeSyncMessage.Meta;
import com.weibo.wesync.notify.service.app.AppProxy;
import com.weibo.wesync.notify.service.app.AppProxyUtil;
import com.weibo.wesync.notify.service.app.RequestType;
import com.weibo.wesync.notify.service.app.ServerType;

public class IndetificationFilter {
	private static Logger log = Logger.getLogger(IndetificationFilter.class);
	
	/**
	 * 返回值：true 表示通过
	 * @param fromuid
	 * @param touid
	 * @param meta
	 * @return
	 */
	public static boolean isMessagePermitted(String fromuid, String touid, Meta meta) {
		if(isBlackList(fromuid,fromuid)){ // 判断是否在黑名单
			log.warn("[IndetificationFilter]:"+fromuid+" send to "+touid+" is forbid!");
			return false;
		}
		return true;
	}
	
	// 在黑名单，返回true,判断发送者在不在接收者的黑名单里
	public static boolean isBlackList(String fromuid, String touid){
//		String url = "/graph/isblacklist";
//		String params = "uid="+fromuid;
//		JsonWrapper json;
//		try {
//			json = new JsonWrapper(AppProxyUtil.commonJson(url,params, null, RequestType.GET, ServerType.weimiPlatform));
//			String result = AppProxy.getInstance().process(touid,"127.0.0.1", json, null);
//			log.warn("[isBlackList][AppProxy]:"+result);
//			JsonWrapper respJson = new JsonWrapper(result);
//			String flag = respJson.get("result");
//			if("true".equals(flag)){
//				return true;
//			}
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
		return false;
	}
}
