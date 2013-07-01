package com.weibo.meyou.perf.util;

import java.util.List;
import java.util.Map;
import java.util.Queue;

import com.weibo.meyou.perf.sdk.message.WesyncExceptionPerf;
import com.weibo.meyou.perf.sdk.protocol.MetaMessageTypePerf;
import com.weibo.meyou.perf.sdk.protocol.MeyouPerf;
import com.weibo.meyou.perf.sdk.protocol.MeyouSortPerf;
import com.weibo.meyou.perf.sdk.protocol.WeSyncMessagePerf.DataSlice;
import com.weibo.meyou.perf.sdk.protocol.WeSyncMessagePerf.FileData;
import com.weibo.meyou.perf.sdk.protocol.WeSyncMessagePerf.FolderCreateReq;
import com.weibo.meyou.perf.sdk.protocol.WeSyncMessagePerf.FolderDeleteReq;
import com.weibo.meyou.perf.sdk.protocol.WeSyncMessagePerf.FolderSyncReq;
import com.weibo.meyou.perf.sdk.protocol.WeSyncMessagePerf.Meta;
import com.weibo.meyou.perf.sdk.protocol.WeSyncMessagePerf.SyncReq;
import com.weibo.meyou.perf.sdk.protocol.WeSyncMessagePerf.WesyncExtContent;
import com.google.protobuf.ByteString;

public class WesyncUtilPerf {
    
    public static String TAG_SYNC_KEY = "0";

   // 生成FolderSync的请求消息体
    public static MessageEntityPerf folderSync(String folderId, WesyncStorePerf wesyncStore){
    	String syncKey = WesyncUtilPerf.TAG_SYNC_KEY;
    	wesyncStore.syncKeys.put(folderId, syncKey);
    	String uristr = wesyncStore.uriMap.get("FolderSync");
    	FolderSyncReq req = FolderSyncReq.newBuilder()
    		.setId(folderId)
    		.setKey(syncKey)
    		.build();			
    	byte[] entity = MeyouUtilPerf.generateRequestEntity("FolderSync",MeyouSortPerf.wesync,req.toByteString());		   
    	MessageEntityPerf messageEntity =new MessageEntityPerf(uristr,entity);
    	return messageEntity;
    }
  
    // 生成FolderDelete的请求消息体
    public static MessageEntityPerf folderDelete(String touid, WesyncStorePerf wesyncStore) {
	String uristr = wesyncStore.uriMap.get("FolderDelete");
	FolderDeleteReq req = FolderDeleteReq.newBuilder()
		.setUserChatWith(touid)
		.setIsContentOnly(true)
		.build();	
	byte[] entity = MeyouUtilPerf.generateRequestEntity("FolderDelete",MeyouSortPerf.wesync,req.toByteString());
	MessageEntityPerf messageEntity =new MessageEntityPerf(uristr,entity);
	return messageEntity;
    }
    
    // 生成FolderCreate的请求消息体（同步）
    public static MessageEntityPerf folderCreate(String touid, WesyncStorePerf wesyncStore){
	String uristr = wesyncStore.uriMap.get("FolderCreate");
	FolderCreateReq req = FolderCreateReq.newBuilder()
		.setUserChatWith(touid)
		.build();
	byte[] entity = MeyouUtilPerf.generateRequestEntity("FolderCreate",MeyouSortPerf.wesync,req.toByteString());		
	MessageEntityPerf messageEntity = new MessageEntityPerf(uristr,entity);
	return messageEntity;	    
    }

    // 生成SendFile的请求消息体
    public static MessageEntityPerf sendFile(int index,int limit,byte[] sendBuf,String fileId, WesyncStorePerf wesyncStore){
	String uristr = wesyncStore.uriMap.get("SendFile");	
	DataSlice slice = DataSlice.newBuilder()
		.setIndex(index)
		.setLimit(limit)
		.setData(ByteString.copyFrom(sendBuf))
		.build();
	FileData req = FileData.newBuilder()
		.setId(fileId)
		.addSlice(slice)
		.build();
	byte[] entity = MeyouUtilPerf.generateRequestEntity("SendFile",MeyouSortPerf.wesync,req.toByteString());
	MessageEntityPerf messageEntity = new MessageEntityPerf(uristr,entity);
	return messageEntity;
    }
    
    // 生成GetFile的请求消息体
    public static MessageEntityPerf getFile(String fileId,int limit,int index, WesyncStorePerf wesyncStore) throws WesyncExceptionPerf{
	String uristr = wesyncStore.uriMap.get("GetFile");	
	System.out.println(uristr);
	DataSlice slice = DataSlice.newBuilder()		
		.setIndex(index)
		.setLimit(limit)
		.build();
	FileData req = FileData.newBuilder()
		.setId(fileId)
		.addSlice(slice)
		.build();
	byte[] entity = MeyouUtilPerf.generateRequestEntity("GetFile",MeyouSortPerf.wesync,req.toByteString());
	MessageEntityPerf messageEntity = new MessageEntityPerf(uristr,entity);	
        return messageEntity;
    }
    
    // 生成文本消息的meta
    public static Meta generatedTextMeta(String msgId, String touid, String text,byte[] padding, ManagerCenterPerf manageCenter){
	if(null == padding){
	    padding = new byte[]{0};
	}
	WesyncExtContent contentExt = WesyncExtContent.newBuilder()
		.setContent(ByteString.copyFrom(padding))
		.build();	
	Meta.Builder metaBuilder = Meta.newBuilder();
	metaBuilder.setFrom(manageCenter.getUid())
		.setId(msgId)
		.setTo(touid)
		.setTime((int)(System.currentTimeMillis()/1000))
		.setType(ByteString.copyFrom(new byte[]{(MetaMessageTypePerf.text.toByte())}))
		.setContent(ByteString.copyFromUtf8(text))
		.setContentExt(contentExt.toByteString());
	return metaBuilder.build();  
    }

    // 生成声音的meta,声音的信息是存放在content中
    public static Meta generatedVoiceMeta(String msgId, String touid,
	    byte[] audioData,byte[] padding, ManagerCenterPerf manageCenter){	
	if(null == padding){
	    padding = new byte[]{0};
	}
	WesyncExtContent contentExt = WesyncExtContent.newBuilder()
		.setContent(ByteString.copyFrom(padding))
		.build();
	Meta.Builder metaBuilder = Meta.newBuilder();
	metaBuilder.setFrom(manageCenter.getUid())
		.setId(msgId)
		.setTo(touid)
		.setType(ByteString.copyFrom(new byte[]{(MetaMessageTypePerf.audio.toByte())}))
		.setTime((int)(System.currentTimeMillis()/1000))	
		.setContent(ByteString.copyFrom(audioData))
		.setContentExt(contentExt.toByteString());
	return metaBuilder.build();  
    }
    
    // 生成文件的meta
    public static Meta generatedFileMeta(String msgId,String fileId,String touid,MetaMessageTypePerf type,
	    byte[] thumbnail,byte[] padding,int fileLength,String filename, ManagerCenterPerf manageCenter) {	
	WesyncExtContent.Builder contentExt = WesyncExtContent.newBuilder();
	if(null == padding){
	    padding = new byte[]{0};
	}
	contentExt.setContent(ByteString.copyFrom(padding));	
	contentExt.setFileLength(fileLength);
	contentExt.setFileName(filename);
	Meta.Builder metaBuilder = Meta.newBuilder();
	metaBuilder.setFrom(manageCenter.getUid())
		.setId(msgId)
		.setTo(touid)
		.setType(ByteString.copyFrom(new byte[]{(type.toByte())}))
		.setTime((int)(System.currentTimeMillis()/1000))	
		.setContent(ByteString.copyFromUtf8(fileId));
	if(null != thumbnail){
	    contentExt.setThumbnail(ByteString.copyFrom(thumbnail));
	}
	metaBuilder.setContentExt(contentExt.build().toByteString());
	return metaBuilder.build();  
    }
	
    public static JsonBuilderPerf weiboJson(Map<String, String> paramerters, ApiServiceTypePerf type,String appname, ManagerCenterPerf manageCenter) {
	JsonBuilderPerf json = new JsonBuilderPerf();	
	json.append("appname",appname);
	json.append("type", type.get());	
	JsonBuilderPerf param = new JsonBuilderPerf();
	param.append("source",manageCenter.getAppKey());
	for(Object key:paramerters.keySet()){
	    param.append((String) key,(String)paramerters.get(key)); 
	}
	json.append("parameter", param.flip());
	System.out.println(json);
	return json.flip();
    }
    
    
    public static MessageEntityPerf uploadContact(List<String> contact){
	JsonBuilderPerf json = new JsonBuilderPerf();
	json.append("type","upload");
	JsonBuilderPerf builder = new JsonBuilderPerf();
	StringBuilder sb = new StringBuilder("[");
	if (contact != null && !contact.isEmpty()) {
		for (String telnum : contact) {
			sb.append("\"").append(telnum).append("\"").append(",");
		}
		sb.setLength(sb.length() - 1);
	}
	sb.append("]");
	builder.append("tels", new JsonBuilderPerf(sb.toString(), true));
	json.append("data", new JsonBuilderPerf(builder.flip().toString(), true));
	String content = json.flip().toString();
	MeyouPerf.MeyouPacket meyouPacket = MeyouPerf.MeyouPacket.newBuilder()
		.setCallbackId("CONTACT")
		.setSort(MeyouSortPerf.contact)
		.setContent(ByteString.copyFromUtf8(content))
		.build();
	byte[] entity = meyouPacket.toByteArray();
	MessageEntityPerf messageEntity = new MessageEntityPerf(null,entity);
	return messageEntity;
    }
    
    // 返回false，表示sync被拒绝，true，表示sync请求成功
    public synchronized static boolean sync(String folderId, WesyncStorePerf wesyncStore) throws WesyncExceptionPerf{
	if(wesyncStore.syncFolder.contains(folderId)){ 
	   System.err.println(folderId+" is syncing! return sync");
//	    return false;
	} else{
	wesyncStore.syncFolder.add(folderId);	
	}
	
	String syncKey = wesyncStore.syncKeys.get(folderId);
	Boolean isFullSync = false;
	if(null == syncKey){
	    System.out.println("首次访问，初始化synckey，全量");
	    syncKey = WesyncUtilPerf.TAG_SYNC_KEY;
	    isFullSync = true;
	    wesyncStore.syncKeys.put(folderId, syncKey);
	}
	
	String uristr = wesyncStore.uriMap.get("Sync");
	SyncReq.Builder syncreqBuilder = SyncReq.newBuilder()
		.setFolderId(folderId)
		.setIsFullSync(isFullSync)
		.setKey(syncKey);
	Queue<Meta> sendQueue = wesyncStore.sendMeta.remove(folderId);
	if( null != sendQueue ){
	     for(Meta m : sendQueue ){
	    	syncreqBuilder.addClientChanges(m);
	     }
	}
	byte[] entity = MeyouUtilPerf.generateRequestEntity("Sync",MeyouSortPerf.wesync,syncreqBuilder.build().toByteString());
        MessageEntityPerf messageEntity = new MessageEntityPerf(uristr,entity);
	//System.out.println("############################### send sync request ############################### "+folderId);
	try {
	    wesyncStore.messageQ.putMessage(messageEntity);
	} catch (InterruptedException e) {
		System.out.println("消息放入队列失败！！！！！");
	    throw new WesyncExceptionPerf("sync请求被中断",e,WesyncExceptionPerf.InterruptedException);
	}
	return true;
    }

    
  public synchronized static boolean syncPerf(String folderId, WesyncStorePerf wesyncStore, HttpUtilPerf httpUtil, ManagerCenterPerf managerCenter) throws WesyncExceptionPerf{
//	if(wesyncStore.syncFolder.contains(folderId)){ 
//	   System.err.println(folderId+" is syncing! return sync");
//	    return false;
//	} else{
//	wesyncStore.syncFolder.add(folderId);	
//	}

	String syncKey = wesyncStore.syncKeys.get(folderId);
	Boolean isFullSync = false;
	if(null == syncKey){
	    System.out.println("首次访问，初始化synckey，全量");
	    syncKey = WesyncUtilPerf.TAG_SYNC_KEY;
	    isFullSync = true;
	    wesyncStore.syncKeys.put(folderId, syncKey);
	}
	
	String uristr = wesyncStore.uriMap.get("Sync");
	SyncReq.Builder syncreqBuilder = SyncReq.newBuilder()
		.setFolderId(folderId)
		.setIsFullSync(isFullSync)
		.setKey(syncKey);
//	syncreqBuilder.addClientChanges(meta);
	Queue<Meta> sendQueue = wesyncStore.sendMeta.remove(folderId);
	if( null != sendQueue ){
	     for(Meta m : sendQueue ){
	    	syncreqBuilder.addClientChanges(m);
	     }
	}
	byte[] entity = MeyouUtilPerf.generateRequestEntity("Sync",MeyouSortPerf.wesync,syncreqBuilder.build().toByteString());
    MessageEntityPerf messageEntity = new MessageEntityPerf(uristr,entity);
    httpUtil.sendMessage(messageEntity);

    return true;
  }
    
    
    public static void getUnreadItem(WesyncStorePerf wesyncStore, HttpUtilPerf httpUtil, ManagerCenterPerf managerCenter){
//    	long t1 = System.currentTimeMillis();
		String uristr = wesyncStore.uriMap.get("GetItemUnread");
		byte[] entity = MeyouUtilPerf.generateRequestEntity("GetItemUnread",MeyouSortPerf.wesync,null);
		MessageEntityPerf messageEntity = new MessageEntityPerf(uristr,entity);
		httpUtil.sendMessage(messageEntity);
//		long t2 = System.currentTimeMillis();
//		System.out.println("(" + managerCenter.getUid() + ")(" + DateUtilPerf.formateDateTime(new java.util.Date(t1)) + ")(" + DateUtilPerf.formateDateTime(new java.util.Date(t2)) + ")" +
//				"(" + (t2 - t1) + "ms)Client finish sending wesync req(GetItemUnread) to server!");
    }   
}
