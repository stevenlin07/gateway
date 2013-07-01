package com.weibo.wesync.notify.service.app;

import cn.sina.api.commons.util.ByteArrayPart;
import cn.sina.api.commons.util.JsonBuilder;
import cn.sina.api.commons.util.JsonWrapper;
import java.util.HashMap;
import java.util.Map; 

public class AppProxyUtil {
	public static AppRequest decodeAppRequest(JsonWrapper json, byte[] attach) {
		AppRequest appRequest = new AppRequest();
		appRequest.code = 402;
		if (null == json) {
			appRequest.text = "json is null";
			return appRequest;
		}

		String type = json.get("requestType");
		if (null != type) {
			appRequest.requestType = RequestType.valueOf(Integer.valueOf(type)
					.intValue());
			if (RequestType.unknow == appRequest.requestType) {
				appRequest.text = "requestType is unknow";
				appRequest.code = 402;
			} else {
				appRequest.code = 200;
			}
		} else {
			appRequest.text = "requestType is null";
			appRequest.code = 402;
		}
		if (402 == appRequest.code) {
			return appRequest;
		}

		String stype = json.get("serverType");
		if (null != stype) {
			appRequest.serverType = ServerType.valueOf(Integer.valueOf(stype)
					.intValue());
			if (ServerType.unknown == appRequest.serverType) {
				appRequest.text = "serverType is unknow";
				appRequest.code = 402;
			} else {
				appRequest.code = 200;
			}
		} else {
			appRequest.text = "serverType is null";
			appRequest.code = 402;
		}
		if (402 == appRequest.code) {
			return appRequest;
		}

		appRequest.url = json.get("url");
		if (StringCheck(appRequest.url)) {
			appRequest.code = 200;
		} else {
			appRequest.text = "url is null";
			appRequest.code = 402;
		}
		if (402 == appRequest.code) {
			return appRequest;
		}
		if ((null != attach) && (0 < attach.length)) {
			appRequest.fileParam = json.get("fileParam");
			if (StringCheck(appRequest.fileParam)) {
				appRequest.code = 200;
				appRequest.attach = attach;
			} else {
				appRequest.text = "fileParam is null";
				appRequest.code = 402;
			}
		}
		if (402 == appRequest.code) {
			return appRequest;
		}

		appRequest.requestString = json.get("params");
		appRequest.source = json.get("source");
		return appRequest;
	}

	public static String deliverAppRequest(AppRequest appRequest,
			Map<String, String> headers) {
		String ret = "{\"code\":404,\"text\":\"app response failed\"}";
		if (RequestType.GET == appRequest.requestType) {
			if (StringCheck(appRequest.requestString)) {
				appRequest.url = (appRequest.url + "?" + appRequest.requestString);
			}
			ret = AppUtil.getInstance().requestGetUrl(appRequest.url, headers,
					null);
		} else if (RequestType.POST == appRequest.requestType) {
			Map params = new HashMap();
			params.put("requestString", appRequest.requestString);
			ret = AppUtil.getInstance().requestPostUrl(appRequest.url, headers,
					params);
		} else if (RequestType.MULTIPART == appRequest.requestType) {
			Map nameValues = new HashMap();
			if (StringCheck(appRequest.requestString)) {
				try {
					String[] split = appRequest.requestString.split("&");
					for (int i = 0; i < split.length; i++) {
						String[] pv = split[i].split("=");
						nameValues.put(pv[0], pv[1]);
					}
				} catch (Exception e) {
					return "{\"code\":404,\"text\":\"parse the params String failed，may be the(&)is not single or miss(=)\"}";
				}
			}
			if (null != appRequest.fileParam) {
				nameValues.put(appRequest.fileParam, new ByteArrayPart(
						appRequest.attach, appRequest.fileParam,
						"application/octet-stream"));
			}

			ret = AppUtil.getInstance().postMultipartUrl(appRequest.url,
					headers, nameValues);
		}

		return ret;
	}

	public static boolean StringCheck(String str) {
		if ((null == str) || ("".equals(str.trim()))
				|| ("null".equals(str.trim())) || (0 > str.length())) {
			return false;
		}
		return true;
	}

	public static String commonJson(String url, String params,
			String fileParam, RequestType requestType, ServerType serverType) {
		JsonBuilder json = new JsonBuilder();
		json.append("url", url);
		json.append("requestType", requestType.get());
		json.append("serverType", serverType.get());
		if (null != params)
			json.append("params", params);
		if (null != fileParam)
			json.append("fileParam", fileParam);
		return json.flip().toString();
	}
}