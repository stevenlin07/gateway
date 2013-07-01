/**
 * 
 */
package com.weibo.meyou.perf.testcase;


import com.weibo.meyou.perf.WesyncInstancePerf;
import com.weibo.meyou.perf.sdk.message.WesyncExceptionPerf;
import com.weibo.meyou.perf.sdk.protocol.MetaMessageTypePerf;
import com.weibo.meyou.perf.util.MeyouUtilPerf;


public class FileSender extends BaseSender {
	private String imagefilepath;
	
	
	public FileSender(int fromuid, String imagefilepath){
		this.fromuid = String.valueOf(fromuid);
		this.touid = String.valueOf(fromuid - 1);
		this.imagefilepath = imagefilepath;
		this.wesyncInstance = new WesyncInstancePerf();
		this.wesyncInstance.managerCenter.setUid(this.fromuid);
	}
	
	public void run(){
		if (wesyncInstance.httpUtil.reconnected && wesyncInstance.httpUtil.isReceiveThreadQuit){
			System.out.println("<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<< restart "+this.fromuid);
			init();
		}
		
		
		try {
			String msgId = MeyouUtilPerf.getMsgId();
			wesyncInstance.sendFilePerf(msgId, touid, imagefilepath, "imageTest.jpg", MetaMessageTypePerf.image, null, msgId.getBytes(), null, 120);
		} catch (WesyncExceptionPerf e) {
			MeyouUtilPerf.dealException(e);
			System.err.println(this.fromuid+" logged out itself");
			wesyncInstance.logout();
		}
	}

}
