package com.weibo.meyou.perf.testcase;

import java.util.concurrent.atomic.AtomicInteger;

import org.apache.mina.common.IoHandlerAdapter;
import org.apache.mina.common.IoSession;

import com.google.protobuf.InvalidProtocolBufferException;
import com.weibo.meyou.perf.sdk.message.NotifyCenterPerf;
import com.weibo.meyou.perf.sdk.message.WesyncExceptionPerf;
import com.weibo.meyou.perf.sdk.protocol.MeyouPerf;
import com.weibo.meyou.perf.sdk.protocol.MeyouSortPerf;
import com.weibo.meyou.perf.util.DateUtilPerf;


public class MinaClientHandler extends IoHandlerAdapter {
	private AtomicInteger uidGenerator;
	
	public MinaClientHandler(int startUid){
		uidGenerator= new AtomicInteger(startUid);
	}
	
	public void messageSent(IoSession session, Object message){
		// 发送信息成功之后调用,不能用于发信息
	}

	// When a new client connects to the server
	public void sessionOpened(IoSession session) throws InvalidProtocolBufferException{
		int fromuid = uidGenerator.getAndAdd(1);
		session.setAttribute("uid", fromuid);
/*		WesyncInstancePerf wesyncInstance = new WesyncInstancePerf();
		try {
		    wesyncInstance.initSDK(prepareForDeviceInfo());
		} catch (WesyncExceptionPerf e) {
		    MeyouUtilPerf.dealException(e);
		}
		
		boolean flag = false;
		
		try {
		    flag = wesyncInstance.authUser(String.valueOf(fromuid), "", 120);
		} catch (WesyncExceptionPerf e) {
			MeyouUtilPerf.dealException(e);   
		}
		if(flag){
		    System.out.println("用户" + fromuid + "认证成功！");
		}else{
		    System.out.println("用户" + fromuid + "认证失败！");
		}
		session.setAttribute("wesyncInstance", wesyncInstance);*/
		NotifyCenterPerf.sessionMap.put(fromuid, session);
		System.out.println("用户" + session.getAttribute("uid") + "认证成功！");
	}

	// When a client disconnects to the server 
	public void sessionClosed(IoSession session) {
		System.out.println("One Client Disconnect: " + session.getAttribute("uid") + "!");
	}

	// Receive response from the server
	public void messageReceived(IoSession session, Object message)
			throws Exception {
		MeyouPerf.MeyouPacket meyouPacket = (MeyouPerf.MeyouPacket) message;
		int sort = meyouPacket.getSort();
		String clientTime = DateUtilPerf.formateDateTime(new java.util.Date(System.currentTimeMillis()));
		switch(sort){
		case MeyouSortPerf.heartbeat :// 处理心跳包的回复
			System.out.println("(" + session.getAttribute("uid") + ")(" + clientTime + ")Client receive HB response from the server!");
		    break;
		case MeyouSortPerf.handshake :// 处理握手的回复	    
			if(meyouPacket.getCode() == 200){
				System.out.println("用户" + session.getAttribute("uid") + "认证成功！");
			}else{
				System.out.println("用户" + session.getAttribute("uid") + "认证失败！");
				throw new WesyncExceptionPerf("用户认证请求失败", WesyncExceptionPerf.ResponseError);
			}
			session.setAttribute("flag", "passed");
		    break;
		case MeyouSortPerf.wesync :// 处理wesync协议的回复
//		    if("".equals(callbackId)){
//				WesyncUtilPerf.getUnreadItem(wesyncStore, httpUtil, managerCenter);
//		    }else if("FolderSync".equals(callbackId)||"FolderCreate".equals(callbackId)||"FolderDelete".equals(callbackId)){
//		    	wesyncStore.syncBlockingQ.add(meyouPacket);
//		    }else if("GroupCreate".equals(callbackId)){
//		    	wesyncStore.syncBlockingQ.add(meyouPacket);
//		    }else{
//				handler = responseProcessor.getHandler(callbackId);
//				handler.handle(meyouPacket);
//		    }
		    break;
		case MeyouSortPerf.appservice:// 处理微博的调用回复 
		}
	}
	
    // 首次获取实例时为必填参数，以后每次使用填写入null即可
//    private DeviceInfoPerf prepareForDeviceInfo(){
//		DeviceInfoPerf deviceInfo = new DeviceInfoPerf();
//		deviceInfo.clientVersionMajor = 10;
//		deviceInfo.clientVersionMinor = 10;
//		deviceInfo.deviceType = "android";
//		deviceInfo.guid = "imei";
//		return deviceInfo;
//    }
}
