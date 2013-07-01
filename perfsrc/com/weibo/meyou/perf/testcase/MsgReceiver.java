package com.weibo.meyou.perf.testcase;

import com.weibo.meyou.perf.WesyncInstancePerf;
import com.weibo.meyou.perf.sdk.message.WesyncExceptionPerf;
import com.weibo.meyou.perf.util.DeviceInfoPerf;
import com.weibo.meyou.perf.util.MeyouUtilPerf;


public class MsgReceiver implements Runnable {
//    private static String username = "meyoutest002@sina.cn";
//    private static String password = "meyoutest";
    
	private String fromuid;
//	private String touid;
	private WesyncInstancePerf wesyncInstance = null;
	
	public MsgReceiver(int fromuid){
		this.fromuid = String.valueOf(fromuid);
//		this.touid = String.valueOf(fromuid + 1);
		this.wesyncInstance = new WesyncInstancePerf();
		this.wesyncInstance.managerCenter.setUid(this.fromuid);
		this.wesyncInstance.managerCenter.setServerIp("10.210.230.32");
	}
	
    // 首次获取实例时为必填参数，以后每次使用填写入null即可
    private DeviceInfoPerf prepareForDeviceInfo(){
		DeviceInfoPerf deviceInfo = new DeviceInfoPerf();
		deviceInfo.clientVersionMajor = 10;
		deviceInfo.clientVersionMinor = 10;
		deviceInfo.deviceType = "android";
		deviceInfo.guid = "imei";
		return deviceInfo;
    }

	@Override
	public void run() {
		Thread.currentThread().setName("Receiver-" + fromuid);
		
		try {
		    wesyncInstance.initSDK(prepareForDeviceInfo());
		} catch (WesyncExceptionPerf e) {
		    MeyouUtilPerf.dealException(e);
		}
		
		boolean flag = false;
		try {
		    flag = wesyncInstance.authUser(fromuid, "", 120);
		} catch (WesyncExceptionPerf e) {
			MeyouUtilPerf.dealException(e);   
		}
		
		if(flag){
		    System.out.println("用户" + fromuid + "认证成功！");
		}else{
		    System.out.println("用户" + fromuid + "认证失败！");
		}
	}
}
