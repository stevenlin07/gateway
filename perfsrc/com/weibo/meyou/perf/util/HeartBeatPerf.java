package com.weibo.meyou.perf.util;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.weibo.meyou.perf.sdk.protocol.MeyouSortPerf;


/*
 * @ClassName: ManageHeartBeat 
 * @Description: 管理与服务器长连的心跳
 * @author LiuZhao
 * @date 2012-9-13 下午3:45:03  
 */
public class HeartBeatPerf {
    
    private Timer timer;
    // 单位秒
    private long timesliceDefaulet = 30;
    private long startTime = 240;
    private HttpUtilPerf httpUtil = null;
    private String startThreadName = null;
    private static ScheduledExecutorService timers = Executors.newScheduledThreadPool(200);
    
	public HeartBeatPerf(HttpUtilPerf httpUtil, String startThreadname){
		this.httpUtil = httpUtil;
		this.startThreadName = startThreadname;
	}
	
    /*
     * @Title: startTimer 
     * @Description: 启动心跳
     * @param timeslice 心跳间隔，单位秒
     */
    public void startTimer(long startTimeNew,long timesliceDefauletNew){
	if(null == timer){
	    timer = new Timer();
	    startTime = startTimeNew;
	    timesliceDefaulet = timesliceDefauletNew;	    
	    timer.schedule(new HeartBeatTask(this.httpUtil, this.startThreadName),startTime*1000,timesliceDefaulet*1000);
	}
    }
    
    public void addTimer(){
    	timers.scheduleWithFixedDelay(new HeartBeatThread(this.httpUtil, this.startThreadName), 5, 120, TimeUnit.SECONDS);
    }
    
    /*
     * @Title: restartTimer 
     * @Description: 重启定时器
     * @param timeslice
     */
    public void restartTimer(long startTimeNew,long timesliceDefauletNew){
	if(null == timer){
	    timer = new Timer();
	}else{
	    timer.cancel();
	}
	startTime = startTimeNew;
	timesliceDefaulet = timesliceDefauletNew;	
	timer.schedule(new HeartBeatTask(this.httpUtil, this.startThreadName),startTime*1000,timesliceDefaulet*1000);
    }
    
    /*
     * @Title: stopTimer 
     * @Description: 关闭定时器
     */
    public void stopTimer(){
		if(null != timer){
		    timer.cancel();
		}
    }
}

/*
 * @ClassName: HeartBeatTimer 
 * @Description: 定时心跳程序
 * @author LiuZhao
 * @date 2012-9-6 下午1:54:26  
 */
class HeartBeatTask extends TimerTask {
    private HttpUtilPerf httpUtil = null;
    private String startThreadName = null;
    
    public HeartBeatTask(HttpUtilPerf httpUtil, String startThreadname){
		this.httpUtil = httpUtil;
		this.startThreadName = startThreadname;    	
    }
 
    @Override
    public void run() {
    	System.out.println(this.startThreadName+" is sending HB...");
		String callbackId = "Heartbeat";
		byte[] entity = MeyouUtilPerf.generateRequestEntity(callbackId,MeyouSortPerf.notice,null);
	        MessageEntityPerf messageEntity = new MessageEntityPerf(null,entity);    
//	        DebugOutput.println("############################### send heartbeat request ###############################");
	    if(!httpUtil.startConnection()){
		    return;
		}
	    httpUtil.sendMessage(messageEntity);
    }
}


class HeartBeatThread extends Thread {
    private HttpUtilPerf httpUtil = null;
    private String startThreadName = null;
    
    public HeartBeatThread(HttpUtilPerf httpUtil, String startThreadname){
		this.httpUtil = httpUtil;
		this.startThreadName = startThreadname;    	
    }
 
    @Override
    public void run() {
    	System.out.println(this.startThreadName + " is sending HB...");
		String callbackId = "Heartbeat";
		byte[] entity = MeyouUtilPerf.generateRequestEntity(callbackId,MeyouSortPerf.notice,null);
	        MessageEntityPerf messageEntity = new MessageEntityPerf(null,entity);
	        
	    if(!httpUtil.startConnection()){
		    return;
		}
	    httpUtil.sendMessage(messageEntity);	
    }
}
