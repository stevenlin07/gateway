/*   
 * @Title: GetItemUnreadRespHandler.java 
 * @Package com.weibo.meyou.sdk.handler.wesync 
 * @Description: TODO(用一句话描述该文件做什么) 
 * @author LiuZhao  
 * @date 2012-9-4 下午9:26:15 
 * @version V1.0   
 */
package com.weibo.meyou.perf.handler;

import java.util.List;

import com.google.protobuf.InvalidProtocolBufferException;
import com.weibo.meyou.perf.sdk.message.WesyncExceptionPerf;
import com.weibo.meyou.perf.sdk.protocol.MeyouPerf.MeyouPacket;
import com.weibo.meyou.perf.sdk.protocol.WeSyncMessagePerf.GetItemUnreadResp;
import com.weibo.meyou.perf.sdk.protocol.WeSyncMessagePerf.Unread;
import com.weibo.meyou.perf.util.DateUtilPerf;
import com.weibo.meyou.perf.util.HttpUtilPerf;
import com.weibo.meyou.perf.util.ManagerCenterPerf;
import com.weibo.meyou.perf.util.WesyncStorePerf;
import com.weibo.meyou.perf.util.WesyncUtilPerf;

/*
 * @ClassName: GetItemUnreadRespHandler 
 * @Description: TODO(这里用一句话描述这个类的作用) 
 * @author LiuZhao
 * @date 2012-9-4 下午9:26:15  
 */
public class GetItemUnreadRespHandlerPerf extends BasicRespHandlerPerf {

	public GetItemUnreadRespHandlerPerf(WesyncStorePerf wesyncStore, ManagerCenterPerf managerCenter, HttpUtilPerf httpUtil){
		super(wesyncStore, managerCenter, httpUtil);
	}
	
    /*  
     * <p>Title: handle</p> 
     * <p>Description: </p> 
     * @param meyouPacket 
     * @see com.weibo.meyou.sdk.test.ResponseHandler#handle(com.weibo.wesync.notify.protocols.Meyou.MeyouPacket) 
     */
    @Override
    public void handle(MeyouPacket meyouPacket) {
//    	DebugOutput.println("############################### receive getUnreadItem response #############################");
    	GetItemUnreadResp unread = null;
    	try {
    	    unread = GetItemUnreadResp.parseFrom(meyouPacket.getContent().toByteArray());
    	} catch (InvalidProtocolBufferException e) {
//    	    DebugOutput.println("获取未读数返回处理异常!");
    		System.out.println("获取未读数返回处理异常!");
    	    return;
    	}

    	List<Unread> unreadList = unread.getUnreadList();
    	if(unreadList.size() == 0){
    	    return;
    	}
    	boolean flag = false;
    	for( Unread u : unreadList){
    	   
    	    String folderId = u.getFolderId();
    	    if(u.getNum()<=0){
    		continue;
    	    }
    	    
    	    try {
//    	    	String clientTime = DateUtilPerf.formateDateTime(new java.util.Date(System.currentTimeMillis()));
//    	    	System.out.println("(" + managerCenter.getUid() + ")(" + clientTime + ")Client send wesync req(Sync) to server!");
    		flag = WesyncUtilPerf.syncPerf(folderId, wesyncStore, httpUtil, managerCenter);
    		if(!flag){ // 发送被拒绝，要积累一个请求 
    		    wesyncStore.noticeFolder.add(folderId);
    		}
    	    } catch (WesyncExceptionPerf e) {
//    		MeyouNoticePerf notice = new MeyouNoticePerf(NoticeTypePerf.exception,e,e.getMessage());
//    		NotifyCenterPerf.clientNotifyChannel.add(notice);
    		continue;
    	    }
    	}
    	}
}
