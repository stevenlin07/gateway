/**
 * 
 */
package com.weibo.meyou.perf.testcase;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import com.weibo.meyou.perf.WesyncInstancePerf;
import com.weibo.meyou.perf.sdk.message.WesyncExceptionPerf;
import com.weibo.meyou.perf.util.DateUtilPerf;
import com.weibo.meyou.perf.util.MeyouUtilPerf;


public class AudioSender extends BaseSender {
	private String audioFilePath;
	
	
	public AudioSender(int fromuid, String audioFilePath){
		this.fromuid = String.valueOf(fromuid);
		this.touid = String.valueOf(fromuid - 1);
		this.audioFilePath = audioFilePath;
		this.wesyncInstance = new WesyncInstancePerf();
		this.wesyncInstance.managerCenter.setUid(this.fromuid);
	}
	
    private byte[] readAudio(){
		byte[] audioData = null;
		FileInputStream fis  = null;
		try {
		    fis = new FileInputStream(new File(audioFilePath));
		    int fileLength  = fis.available();
		    audioData = new byte[fileLength] ;
		    fis.read(audioData);
		} catch (FileNotFoundException e1) {
		    e1.printStackTrace();
		} catch (IOException e) {
		    e.printStackTrace();
		} finally {
		    try {
			fis.close();
		    } catch (IOException e) {
			e.printStackTrace();
		    }
		}
		return audioData;
    }
	
	public void run(){
		if (wesyncInstance.httpUtil.reconnected && wesyncInstance.httpUtil.isReceiveThreadQuit){
			System.out.println("<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<< restart "+this.fromuid);
			init();
		}
		
		
		try {
			String msgId = MeyouUtilPerf.getMsgId();
			wesyncInstance.sendAudioPerf(msgId, touid, readAudio(), msgId.getBytes(), 120);
//			long t2 = System.currentTimeMillis();
//			long sendMsgTime = t2 - Long.parseLong(msgId);
//			String sendOutTime = DateUtilPerf.formateDateTime(new java.util.Date(t2));
//			System.out.println("(" + fromuid + ")(" + msgId + ")(" + sendOutTime + ")(" + sendMsgTime + "ms)");
		} catch (WesyncExceptionPerf e) {
			MeyouUtilPerf.dealException(e);
			System.err.println(this.fromuid+" logged out itself");
			wesyncInstance.logout();
		}
	}
}
