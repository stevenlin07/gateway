package com.weibo.meyou.perf.handler;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.weibo.meyou.perf.sdk.protocol.MeyouPerf.MeyouPacket;
import com.weibo.meyou.perf.sdk.protocol.MeyouSortPerf;
import com.weibo.meyou.perf.util.DateUtilPerf;
import com.weibo.meyou.perf.util.HttpUtilPerf;
import com.weibo.meyou.perf.util.ManagerCenterPerf;
import com.weibo.meyou.perf.util.WesyncStorePerf;
import com.weibo.meyou.perf.util.WesyncUtilPerf;

/*
 * @ClassName: ResponseProcessor 
 * @Description: TODO(这里用一句话描述这个类的作用) 
 * @author LiuZhao
 * @date 2012-9-4 下午9:39:30  
 */
public class ResponseProcessorPerf {
    private Map<String, ResponseHandlerPerf> handlers = new ConcurrentHashMap<String, ResponseHandlerPerf>();
    private WesyncStorePerf wesyncStore = null;
    private ManagerCenterPerf managerCenter = null;
    private HttpUtilPerf httpUtil = null;
    
    public ResponseProcessorPerf(WesyncStorePerf wesyncStore, ManagerCenterPerf managerCenter, HttpUtilPerf httpUtil) {
		this.wesyncStore = wesyncStore;
		this.managerCenter = managerCenter;
		this.httpUtil = httpUtil;
    	setupHandlers();
    }

    private void setupHandlers(){
	handlers.put("Sync", new SyncRespHandlerPerf(wesyncStore, managerCenter, httpUtil));
	handlers.put("GetItemUnread", new GetItemUnreadRespHandlerPerf(wesyncStore, managerCenter, httpUtil));
	handlers.put("SendFile", new SendFileRespHandlerPerf(wesyncStore, managerCenter, httpUtil));
	handlers.put("GetFile", new GetFileRespHandlerPerf(wesyncStore, managerCenter, httpUtil));
	handlers.put("AppService",new AppServiceRespHandlerPerf(wesyncStore, managerCenter, httpUtil));
    }

    private ResponseHandlerPerf getHandler(String command){
	ResponseHandlerPerf handler = handlers.get(command);
	if( handler == null ) 
	    return handlers.get("Unknown");
	return handler;
    }
	
    public void process(MeyouPacket meyouPacket){
//	DebugOutput.println("Begin to deal Packet");
//	DebugOutput.println(meyouPacket.toString());
	String callbackId = meyouPacket.getCallbackId();
	int sort = meyouPacket.getSort();
	ResponseHandlerPerf handler = null; 
	
	String clientTime = DateUtilPerf.formateDateTime(new java.util.Date(System.currentTimeMillis()));
	switch(sort){
	case MeyouSortPerf.heartbeat :// 处理心跳包的回复
	    break;
	case MeyouSortPerf.handshake :// 处理握手的回复	    
	    wesyncStore.syncBlockingQ.add(meyouPacket);
	    break;
	case MeyouSortPerf.wesync :// 处理wesync协议的回复
	    if("".equals(callbackId)){		
//		System.out.println("(" + managerCenter.getUid() + ")(" + clientTime + ")Client receive server notice and send wesync req(GetItemUnread) to server!");
		WesyncUtilPerf.getUnreadItem(wesyncStore, httpUtil, managerCenter);
		break;
	    }else if("FolderSync".equals(callbackId)||"FolderCreate".equals(callbackId)||"FolderDelete".equals(callbackId)){
	    	wesyncStore.syncBlockingQ.add(meyouPacket);
	    }else if("GroupCreate".equals(callbackId)){
	    	wesyncStore.syncBlockingQ.add(meyouPacket);
	    }else{
//	    	if ((Integer.parseInt(managerCenter.getUid()) % 2) != 0){
//	    	    if("GetItemUnread".equals(callbackId)){
//	    	    	System.out.println("(" + meyouPacket.getUid() + ")(" + clientTime + ")Client receive wesync resp(GetItemUnread) from server!");
//	    	    }else if("Sync".equals(callbackId)){
//	    	    	System.out.println("(" + meyouPacket.getUid() + ")(" + clientTime + ")Client receive wesync resp(Sync) from server!");
//	    	    }	    		
//	    	}
	
			handler = getHandler(callbackId);
			handler.handle(meyouPacket);
	    }
	    break;
	case MeyouSortPerf.appservice:// 处理微博的调用回复 
	    handler = getHandler("AppService");
	    handler.handle(meyouPacket);
	    break; 
	}	
    }
}
