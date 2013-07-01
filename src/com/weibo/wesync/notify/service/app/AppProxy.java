package com.weibo.wesync.notify.service.app;

import cn.sina.api.commons.util.JsonWrapper;
import com.weibo.wesync.notify.service.app.tauth.TAuthUtil;
import com.weibo.wesync.notify.utils.Util;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;
import org.apache.log4j.Logger;

public class AppProxy {
	private static Logger log = Logger.getLogger(AppProxy.class);
	private static AppProxy appProxy;
	private static String weimiActivity = Util.getConfigProp("weimiActivity",
			"http://server.u-mai.net:27000/api");
	private static String weimiPlatform = Util.getConfigProp("weimiPlatform",
			"http://180.149.138.89");

	private static TAuthUtil tauth = TAuthUtil.getInstance();

	public static AppProxy getInstance() {
		if (appProxy == null) {
			synchronized (AppProxy.class) {
				if (appProxy == null) {
					appProxy = new AppProxy();
				}
			}
		}
		return appProxy;
	}

	public String process(String fromuid, String remoteIp, JsonWrapper json,
			byte[] attach) {
		String result = "{\"code\":403,\"text\":\"app request failed\"}";
		try {
			AppRequest appRequest = AppProxyUtil.decodeAppRequest(json, attach);
			appRequest.uid = fromuid;
			appRequest.remoteIp = remoteIp;
			if (200 != appRequest.code) {
				result = "{\"code\":" + appRequest.code + ",\"text\":\""
						+ appRequest.text + "\"}";
				log.error(result);
				return result;
			}
			if (ServerType.weimiActivity == appRequest.serverType)
				result = replyWeimiActivity(appRequest);
			else if (ServerType.weimiPlatform == appRequest.serverType)
				result = replyWeimiPlatform(appRequest);
			else if (ServerType.weiboPlatform == appRequest.serverType) {
				result = replyWeiboPlatform(appRequest);
			}
			if (null == result)
				result = "{\"code\":403,\"text\":\"app request failed\"}";
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
		return result;
	}

	private String replyWeimiActivity(AppRequest appRequest) {
		appRequest.url = (weimiActivity + appRequest.url);
		Map headers = new HashMap();
		headers.put("ls_uid", appRequest.uid);
		return AppProxyUtil.deliverAppRequest(appRequest, headers);
	}

	private String replyWeimiPlatform(AppRequest appRequest) {
		appRequest.url = (weimiPlatform + appRequest.url);
		Map headers = new HashMap();
		headers.put("X-Matrix-UID", appRequest.uid);
		headers.put("X-Matrix-AppID", Util.getConfigProp("X-Matrix-AppID", "1"));
		headers.put("X-Matrix-RemoteIP", appRequest.remoteIp);
		return AppProxyUtil.deliverAppRequest(appRequest, headers);
	}

	private String replyWeiboPlatform(AppRequest appRequest) {
		Map headers = new HashMap();
		if (null == appRequest.source) {
			return "{\"code\":403,\"text\":\"weibo request source is empty!\"}";
		}
		headers.put("Authorization",
				tauth.getToken(appRequest.uid, appRequest.source));
		headers.put("cuid", appRequest.uid);
		System.out.println(headers.toString());
		return AppProxyUtil.deliverAppRequest(appRequest, headers);
	}
}