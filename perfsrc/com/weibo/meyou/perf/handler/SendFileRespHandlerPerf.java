/*   
 * @Title: SendFileRespHandler.java 
 * @Package com.weibo.meyou.sdk.handler.wesync 
 * @Description: 
 * @author LiuZhao  
 * @date 2012-9-4 下午9:27:05 
 * @version V1.0   
 */
package com.weibo.meyou.perf.handler;

import java.util.LinkedList;
import java.util.List;
import java.util.Timer;

import com.google.protobuf.InvalidProtocolBufferException;
import com.weibo.meyou.perf.sdk.protocol.MeyouPerf.MeyouPacket;
import com.weibo.meyou.perf.sdk.protocol.WeSyncMessagePerf.DataSlice;
import com.weibo.meyou.perf.sdk.protocol.WeSyncMessagePerf.FileData;
import com.weibo.meyou.perf.util.HttpUtilPerf;
import com.weibo.meyou.perf.util.ManagerCenterPerf;
import com.weibo.meyou.perf.util.WesyncStorePerf;

/*
 * @ClassName: SendFileRespHandler 
 * @Description: 发送文件的回执
 * @author LiuZhao
 * @date 2012-9-4 下午9:27:05  
 */
public class SendFileRespHandlerPerf extends BasicRespHandlerPerf {
	public SendFileRespHandlerPerf(WesyncStorePerf wesyncStore, ManagerCenterPerf managerCenter, HttpUtilPerf httpUtil){
		super(wesyncStore, managerCenter, httpUtil);
	}

    @Override
    public void handle(MeyouPacket meyouPacket) {
    	FileData resp;
    	try {
    	    resp = FileData.parseFrom(meyouPacket.getContent().toByteArray());
    	} catch (InvalidProtocolBufferException e) {
    	    return;
    	}
    	String fileId = resp.getId();
    	fileId = fileId.substring(fileId.indexOf("-", 0)+1, fileId.length());
//    	String msgId = fileId.substring(fileId.indexOf("-", 0)+1, fileId.length());
    	List<Integer> unUploadSlice = new LinkedList<Integer>();

    	if(!resp.getSliceList().isEmpty()){
    	    for(DataSlice s:resp.getSliceList()){// 如果缺少分片，说明缺少列表不为空
    		unUploadSlice.add(s.getIndex());
    	    }
    	}else{
    	    if(wesyncStore.tagTimeList.containsKey(fileId)){// 说明接收成功
    		Timer t = wesyncStore.tagTimeList.get(fileId);
    		t.cancel();
    		wesyncStore.tagTimeList.remove(fileId);	
    	    }    
    	}
//    	MeyouNoticePerf notice = new MeyouNoticePerf(NoticeTypePerf.sendfile,unUploadSlice,msgId);
//    	NotifyCenterPerf.clientNotifyChannel.add(notice);	
    }

}
