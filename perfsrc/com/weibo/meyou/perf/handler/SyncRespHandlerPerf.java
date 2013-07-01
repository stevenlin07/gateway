package com.weibo.meyou.perf.handler;

import java.util.List;
import java.util.Queue;
import java.util.Timer;

import com.google.protobuf.InvalidProtocolBufferException;
import com.weibo.meyou.perf.sample.TextContent;
import com.weibo.meyou.perf.sdk.message.AudioMessagePerf;
import com.weibo.meyou.perf.sdk.message.FileMessagePerf;
import com.weibo.meyou.perf.sdk.message.TextMessagePerf;
import com.weibo.meyou.perf.sdk.message.WesyncExceptionPerf;
import com.weibo.meyou.perf.sdk.protocol.MetaMessageTypePerf;
import com.weibo.meyou.perf.sdk.protocol.MeyouPerf.MeyouPacket;
import com.weibo.meyou.perf.sdk.protocol.MeyouSortPerf;
import com.weibo.meyou.perf.sdk.protocol.SyncKeyPerf;
import com.weibo.meyou.perf.sdk.protocol.WeSyncMessagePerf.Meta;
import com.weibo.meyou.perf.sdk.protocol.WeSyncMessagePerf.SyncReq;
import com.weibo.meyou.perf.sdk.protocol.WeSyncMessagePerf.SyncResp;
import com.weibo.meyou.perf.sdk.protocol.WeSyncMessagePerf.WesyncExtContent;
import com.weibo.meyou.perf.util.DateUtilPerf;
import com.weibo.meyou.perf.util.HttpUtilPerf;
import com.weibo.meyou.perf.util.MD5UtilPerf;
import com.weibo.meyou.perf.util.ManagerCenterPerf;
import com.weibo.meyou.perf.util.MessageEntityPerf;
import com.weibo.meyou.perf.util.MeyouUtilPerf;
import com.weibo.meyou.perf.util.WesyncStorePerf;
import com.weibo.meyou.perf.util.WesyncUtilPerf;

/*
 * @ClassName: SyncRespHandler 
 * @Description: Sync请求
 * @author LiuZhao
 * @date 2012-9-5 下午6:31:52  
 */
public class SyncRespHandlerPerf extends BasicRespHandlerPerf {
	
	public SyncRespHandlerPerf(WesyncStorePerf wesyncStore, ManagerCenterPerf managerCenter, HttpUtilPerf httpUtil){
		super(wesyncStore, managerCenter, httpUtil);
	}
	
    @Override
    public void handle(MeyouPacket meyouPacket) {
    	SyncResp resp;
    	try {
    	    resp = SyncResp.parseFrom(meyouPacket.getContent().toByteArray());
    	} catch (InvalidProtocolBufferException e) { // 内部错误不向客户端发送，发到SDK的全局通知通道，考虑log
    	    wesyncStore.syncFolder.clear();
    	    return;
    	}
    	String folderId = resp.getFolderId();
    	String syncKey = wesyncStore.syncKeys.get(folderId);	    
    	if (null == syncKey) {
    	    syncKey = WesyncUtilPerf.TAG_SYNC_KEY;
    	    wesyncStore.syncKeys.put(folderId,syncKey);
    	}
     	List<Meta> serverList = resp.getServerChangesList();
    	for(Meta meta: serverList){// 接收服务端的消息
//    	    String convId = null;
//    	    if(managerCenter.getUid().equals(meta.getTo())){ // 如果meta的to是当前用户的uid，说明是普通会话
//    		convId =  meta.getFrom();
//    	    }else{ //  如果meta的to不是当前用户的uid，说明是群聊会话
//    		convId = meta.getTo();
//    	    }
//    	    MeyouNoticePerf notice = null;
    	    MetaMessageTypePerf type = MetaMessageTypePerf.valueOf((meta.getType().toByteArray())[0]);
    	    if(MetaMessageTypePerf.text == type){
    	    	metaToTextMessage(meta);
//    		notice = new MeyouNoticePerf(NoticeTypePerf.textmessage,metaToTextMessage(meta),convId);
    	    }else if(MetaMessageTypePerf.audio == type){
    	    	metaToAudioMessage(meta);
//    		notice = new MeyouNoticePerf(NoticeTypePerf.audiomessage,metaToAudioMessage(meta),convId);
    	    }else if(MetaMessageTypePerf.image == type || MetaMessageTypePerf.video == type || MetaMessageTypePerf.file == type){
    	    	metaToFileMessage(meta);
//    		notice = new MeyouNoticePerf(NoticeTypePerf.filemessage,metaToFileMessage(meta),convId);			
    	    }else{
    		continue;
    	    }
//    	    NotifyCenterPerf.clientNotifyChannel.add(notice);
    	}

    	for(Meta meta: resp.getClientChangesList()){	    
    	    if(meta.hasContent() ){
    		String msgId = meta.getId();
    		if(wesyncStore.tagTimeList.containsKey(msgId)){// 说明接收成功
    		    Timer t = wesyncStore.tagTimeList.get(msgId);
    		    t.cancel();
    		    wesyncStore.tagTimeList.remove(msgId);
//    		    MeyouNoticePerf notice = new MeyouNoticePerf(NoticeTypePerf.sendback,msgId+"，发送成功！",msgId);
//    		    NotifyCenterPerf.clientNotifyChannel.add(notice);	
    		}
    	    }
    	}
    	String nextSyncKey = resp.getNextKey();
//    	DebugOutput.println("receive sync response:");
//    	DebugOutput.println("folder:"+folderId);
//    	DebugOutput.println("synckey:"+syncKey);
//    	DebugOutput.println("isFullSync:"+resp.getIsFullSync());
//    	DebugOutput.println("Nextsynckey:"+nextSyncKey);
    	syncKey = nextSyncKey;
    	wesyncStore.syncKeys.put(folderId, syncKey);
    	
    	//Client change need to be synchronized to server despite of server changes exist or not
    	if( !syncKey.equals(TAG_SYNC_KEY) && SyncKeyPerf.isEmpty(nextSyncKey) ) { // 
    	   if(syncStop(folderId)){ // 如果退出得到了确认
//    	       DebugOutput.println("sync stop! no message no unread for "+folderId);
    	       wesyncStore.syncFolder.remove(folderId); // 释放该文件夹，其已经不进行同步
    	       return;
    	   }
    	}
//    	try {
//    	    syncContinue(folderId,syncKey);
//    	} catch (WesyncExceptionPerf e) {
////    	    MeyouNoticePerf notice = new MeyouNoticePerf(NoticeTypePerf.exception,e,e.getMessage());
////    	    NotifyCenterPerf.clientNotifyChannel.add(notice);
//    	    wesyncStore.syncFolder.remove(folderId); // 释放该文件夹，其已经不进行同步
//    	    return;
//    	}
        }
        
        private void syncContinue(String folderId,String syncKey) throws WesyncExceptionPerf{
    	String uristr = wesyncStore.uriMap.get("Sync");
    	SyncReq.Builder syncreqBuilder = SyncReq.newBuilder()
    		.setFolderId(folderId)
    		.setIsFullSync(false)
    		.setKey(syncKey);
    	Queue<Meta> sendQueue = wesyncStore.sendMeta.remove(folderId);
    	if( null != sendQueue ){
    	     for(Meta m : sendQueue ){
    	    	syncreqBuilder.addClientChanges(m);
    	     }
    	}
    	byte[] entity = MeyouUtilPerf.generateRequestEntity("Sync",MeyouSortPerf.wesync,syncreqBuilder.build().toByteString());
            MessageEntityPerf messageEntity = new MessageEntityPerf(uristr,entity);
            httpUtil.sendMessage(messageEntity);
//    	DebugOutput.println("send sync request");
//    	DebugOutput.println("folder:"+folderId);
//    	DebugOutput.println("synckey:"+syncKey);
//    	DebugOutput.println("isFullSync:"+false);
//    	DebugOutput.println("############################### send sync continue request ###############################");
//    	try {
//    	    wesyncStore.messageQ.putMessage(messageEntity);
//    	} catch (InterruptedException e) {
//    	    throw new WesyncException("send sync request interrupted",e,WesyncException.InterruptedException);
//    	}
        }
        
        private boolean syncStop(String folderId){
    	if(wesyncStore.noticeFolder.remove(folderId)){ // 如果发现积攒了通知就清理掉这个通知，说明需要继续sync
//    	    DebugOutput.println("when the "+folderId+" locked, it has notice! continue!");
    	    return false;
    	}	
    	Queue<Meta> sendQueue = wesyncStore.sendMeta.get(folderId); // 发现并没有未读数的通知,判断是否要发上去的消息
    	if((null != sendQueue) && (0 <= sendQueue.size())){
//    	    DebugOutput.println("when the "+folderId+" locked, the send queue is not empty!");
    	    return false;
    	}
    	return true;	
        }
     
        TextMessagePerf metaToTextMessage(Meta meta){
    	TextMessagePerf textMessage = new TextMessagePerf();
    	textMessage.msgId = meta.getId();
    	textMessage.fromuid = meta.getFrom();
    	textMessage.time = (long)meta.getTime()*1000;
    	textMessage.text = meta.getContent().toStringUtf8();
    	WesyncExtContent contentEx;
    	try {
    	    contentEx = WesyncExtContent.parseFrom(meta.getContentExt());
    	    textMessage.padding = contentEx.getContent().toByteArray();
    	} catch (InvalidProtocolBufferException e) {
//    	    DebugOutput.println("WesyncExtContent处理异常!");
    	}
//    	
//    	System.out.println("********** 接收到一条文本消息（Begin） **********");
//    	System.out.println("消息ID：" + textMessage.msgId);
//    	System.out.println("发送者：" + textMessage.fromuid);
//    	String timestr = new java.text.
//    		SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(new java.util.Date(textMessage.time));
//    	System.out.println("发送时间：" + timestr);
    	
    	long currentTime = System.currentTimeMillis();
    	String sendTimeStr = new String(textMessage.padding);
    	long msgSendTime = currentTime - Long.parseLong(sendTimeStr);
    	String originTime = DateUtilPerf.formateDateTime(new java.util.Date(Long.parseLong(sendTimeStr)));
    	String destTime = DateUtilPerf.formateDateTime(new java.util.Date(currentTime));
    	System.out.println("(" + originTime + ")(" + destTime + ")(" + msgSendTime + "ms)(" + sendTimeStr + ")(" + Thread.currentThread().getName() + ")接收消息成功！" + textMessage.text.substring(0, textMessage.text.indexOf("****")));
//    	System.out.println("(" + msgSendTime + "ms)(" + Thread.currentThread().getName() + ")接收消息成功！" + textMessage.text.substring(0, textMessage.text.indexOf("****")));
    	return textMessage;
        }
        
        AudioMessagePerf metaToAudioMessage(Meta meta){
    	AudioMessagePerf audioMessage = new AudioMessagePerf();
    	audioMessage.msgId = meta.getId();
    	audioMessage.fromuid = meta.getFrom();
    	audioMessage.time = meta.getTime()*1000;
    	audioMessage.audioData = meta.getContent().toByteArray();
    	WesyncExtContent contentEx;
    	try {
    	    contentEx = WesyncExtContent.parseFrom(meta.getContentExt());
    	    audioMessage.padding = contentEx.getContent().toByteArray();
    	} catch (InvalidProtocolBufferException e) {
//    	    DebugOutput.println("WesyncExtContent处理异常!");
    	}
    	
    	long currentTime = System.currentTimeMillis();
    	String sendTimeStr = new String(audioMessage.padding);
    	long audioSendTime = currentTime - Long.parseLong(sendTimeStr);
    	String originTime = DateUtilPerf.formateDateTime(new java.util.Date(Long.parseLong(sendTimeStr)));
    	String destTime = DateUtilPerf.formateDateTime(new java.util.Date(currentTime));
    	String audioStreamMD5 = MD5UtilPerf.getBytesMD5(audioMessage.audioData);
    	if(TextContent.md5Map.containsKey(audioStreamMD5)){
    		System.out.println("(" + originTime + ")("+destTime+")(" + audioSendTime + "ms)(" + sendTimeStr + ")(" + Thread.currentThread().getName() + ")接收音频文件成功！大小为" + TextContent.md5Map.get(audioStreamMD5) + "<<<<<<<<<<<<<<<<<<<<<<<<<<<<< " + audioMessage.fromuid);
//    		System.out.println("(" + audioSendTime + "ms)(" + Thread.currentThread().getName() + ")接收音频文件成功！大小为" + TextContent.md5Map.get(audioStreamMD5) + "<<<<<<<<<<<<<<<<<<<<<<<<<<<<< " + audioMessage.fromuid);
    	}else{
    		System.out.println("(" + originTime + ")("+destTime+")(" + audioSendTime + "ms)(" + sendTimeStr + ")(" + Thread.currentThread().getName() + ")接收音频文件失败！<<<<<<<<<<<<<<<<<<<<<<<<<<<<< " + audioMessage.fromuid);
//    		System.out.println("(" + audioSendTime + "ms)(" + Thread.currentThread().getName() + ")接收音频文件失败！<<<<<<<<<<<<<<<<<<<<<<<<<<<<< " + audioMessage.fromuid);
    	}
    	
    	return audioMessage;
        }
        
        FileMessagePerf metaToFileMessage(Meta meta){
    	FileMessagePerf fileMessage = new FileMessagePerf();
    	fileMessage.msgId = meta.getId();
    	fileMessage.fromuid = meta.getFrom();
    	fileMessage.time = meta.getTime()*1000;
    	byte[] fileType = meta.getType().toByteArray();
    	fileMessage.type = MetaMessageTypePerf.valueOf(fileType[0]);
    	fileMessage.fileId = meta.getContent().toStringUtf8();
    	WesyncExtContent contentEx;
    	try {
    	    contentEx = WesyncExtContent.parseFrom(meta.getContentExt());
    	    fileMessage.padding = contentEx.getContent().toByteArray();
    	    fileMessage.thumbData =  null;
    	    if(null != contentEx.getThumbnail().toByteArray()){
    		fileMessage.thumbData = contentEx.getThumbnail().toByteArray();
    	    }	    
    	    fileMessage.filename = contentEx.getFileName();
    	    fileMessage.fileLength = contentEx.getFileLength();
    	} catch (InvalidProtocolBufferException e) {
//    	    DebugOutput.println("WesyncExtContent处理异常!");
    	}	
    	
    	long currentTime = System.currentTimeMillis();
    	String sendTimeStr = new String(fileMessage.padding);
    	long fileSendTime = currentTime - Long.parseLong(sendTimeStr);
    	String originTime = DateUtilPerf.formateDateTime(new java.util.Date(Long.parseLong(sendTimeStr)));
    	String destTime = DateUtilPerf.formateDateTime(new java.util.Date(currentTime));
    	if(fileMessage.fileLength == TextContent.IMAGE_FILE_SIZE){
    		System.out.println("(" + originTime + ")("+destTime+")(" + fileSendTime + "ms)(" + sendTimeStr + ")(" + Thread.currentThread().getName() + ")接收图片文件成功！<<<<<<<< " + fileMessage.fromuid);
//    		System.out.println("(" + fileSendTime + "ms)(" + Thread.currentThread().getName() + ")接收图片文件成功！<<<<<<<< " + fileMessage.fromuid);
    	}else{
    		System.out.println("(" + originTime + ")("+destTime+")(" + fileSendTime + "ms)(" + sendTimeStr + ")(" + Thread.currentThread().getName() + ")接收图片文件失败！<<<<<<<< " + fileMessage.fromuid);
//    		System.out.println("(" + fileSendTime + "ms)(" + Thread.currentThread().getName() + ")接收图片文件失败！<<<<<<<< " + fileMessage.fromuid);
    	}
    	return fileMessage;
    }
}
