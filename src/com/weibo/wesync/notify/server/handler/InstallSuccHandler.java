package com.weibo.wesync.notify.server.handler;

import java.util.Arrays;
import java.util.Set;

import org.apache.log4j.Logger;

import cn.sina.api.commons.util.JsonBuilder;
import cn.sina.api.commons.util.JsonWrapper;

import com.weibo.meyou.notice.service.Notice;
import com.weibo.meyou.notice.service.NoticeServiceManager;
import com.weibo.meyou.notice.utils.OpenAPIDataUtils;


public class InstallSuccHandler {
	Logger log = Logger.getLogger(InstallSuccHandler.class);
	
	public void process(String fromUid, JsonWrapper requestJson){
		try {			
			long[] bilFriUids = OpenAPIDataUtils.instance.getBilateralFriendIds(fromUid, requestJson.get("source"));
			Set<Long> installMeyouUidSet = NoticeServiceManager.instance.screenUids4InstallMeyou(bilFriUids);
			
			StringBuilder installMeyouUidSb = new StringBuilder();
			StringBuilder notInstallMeyouUidSb = new StringBuilder();
			
			for (long bilFriId : bilFriUids) {
				if(installMeyouUidSet.contains(bilFriId)){
					installMeyouUidSb.append(bilFriId).append(",");
				} else {
					notInstallMeyouUidSb.append(bilFriId).append(",");
				}
			}
			
			log.debug(String.format("%s first install meyou but do not push notice to these people who have not install meyou, %s",  
					fromUid, notInstallMeyouUidSb.toString()));
			
			if(installMeyouUidSet.size() == 0){
				return ;
			}
			
			JsonBuilder jb = new JsonBuilder();
			jb.append("type", Notice.install_succ);
			jb.append("source", requestJson.get("source"));
			jb.append("fromUid", fromUid);
			jb.append("toUids", installMeyouUidSb.toString());
			jb.flip();
			JsonWrapper jsonWra = new JsonWrapper(jb.toString());
			
			NoticeServiceManager.instance.sendNotice(jsonWra);
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
	}
	
	public static void main(String[] args) {
		long[] l = new long[]{1,2,3};
		System.out.println(Arrays.toString(l));
	}	
}
