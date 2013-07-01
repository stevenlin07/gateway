package com.weibo.meyou.perf;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.weibo.meyou.perf.sdk.message.WesyncExceptionPerf;
import com.weibo.meyou.perf.sdk.protocol.FolderIDPerf;
import com.weibo.meyou.perf.sdk.protocol.MeyouPerf;
import com.weibo.meyou.perf.sdk.protocol.MeyouSortPerf;
import com.weibo.meyou.perf.sdk.protocol.SyncKeyPerf;
import com.weibo.meyou.perf.sdk.protocol.WeSyncMessagePerf.FolderCreateResp;
import com.weibo.meyou.perf.sdk.protocol.WeSyncMessagePerf.FolderSyncResp;
import com.weibo.meyou.perf.sdk.protocol.WeSyncMessagePerf.Meta;
import com.weibo.meyou.perf.util.ApiServiceTypePerf;
import com.weibo.meyou.perf.util.CheckUtilPerf;
import com.weibo.meyou.perf.util.DeviceInfoPerf;
import com.weibo.meyou.perf.util.HeartBeatPerf;
import com.weibo.meyou.perf.util.HttpUtilPerf;
import com.weibo.meyou.perf.util.JsonBuilderPerf;
import com.weibo.meyou.perf.util.ManagerCenterPerf;
import com.weibo.meyou.perf.util.MessageEntityPerf;
import com.weibo.meyou.perf.util.MeyouUtilPerf;
import com.weibo.meyou.perf.util.WesyncStorePerf;
import com.weibo.meyou.perf.util.WesyncUtilPerf;

/*
 * @ClassName: BasicApiImpl 
 * @Description: 基础实现类
 * @author LiuZhao
 * @date 2012-10-23 下午10:18:46  
 */
public class BasicImplPerf {

	
	
    public boolean flag = false; // 是否认证过 
    protected boolean connected = false;
    public WesyncStorePerf wesyncStore = new WesyncStorePerf();
    public ManagerCenterPerf managerCenter = new ManagerCenterPerf();
    public HttpUtilPerf httpUtil = null;
    protected HeartBeatPerf heartBeat = null;
    
    public void initSDK(DeviceInfoPerf deviceInfo) throws WesyncExceptionPerf{
    	managerCenter.setDeviceInfo(deviceInfo,wesyncStore);
    }
    
    protected void setTimer(final String msgId,int timeout){
	Timer t = new Timer();
	TimerTask task = new TimerTask(){
	    public void run() {
//		WesyncExceptionPerf wesyncException = new WesyncExceptionPerf(msgId,WesyncExceptionPerf.RequestTimeout);
//		MeyouNoticePerf notice = new MeyouNoticePerf(NoticeTypePerf.exception,wesyncException,wesyncException.getMessage());
//		NotifyCenterPerf.clientNotifyChannel.add(notice);
		wesyncStore.tagTimeList.remove(msgId);
	    }
	};
	t.schedule(task, timeout*1000);
	wesyncStore.tagTimeList.put(msgId,t);	
    }
    
    /*
     * @Title: syncQueue 
     * @Description: 专门用来处理SDK中的同步操作
     * @return Meyou.MeyouPacket
     * @throws WesyncException
     */
    protected MeyouPerf.MeyouPacket syncQueue(MessageEntityPerf messageEntity,String requestName,int timeout) throws WesyncExceptionPerf{
//	long t1 = System.currentTimeMillis();
    if(!httpUtil.startConnection()){
	    throw new WesyncExceptionPerf(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>网络启动失败!!!!!>>>>>>",WesyncExceptionPerf.NetworkInterruption);
	} // 启动网络
	connected = true;
	httpUtil.sendMessage(messageEntity);
//	try {
//	    wesyncStore.messageQ.putMessage(messageEntity);
//	} catch (InterruptedException e) {
//	    throw new WesyncException(requestName+"请求被中断",e,WesyncException.InterruptedException);
//	}
	try {
	    MeyouPerf.MeyouPacket packet = wesyncStore.syncBlockingQ.poll(timeout,TimeUnit.SECONDS); // 接收返回结果
	    wesyncStore.syncBlockingQ.clear();
	    if(null == packet){ // 说明该队列直接超时返回了
		throw new WesyncExceptionPerf(requestName+"请求超时",WesyncExceptionPerf.RequestTimeout);
	    }
	    if(packet.getCode()!=200){ 
		throw new WesyncExceptionPerf(requestName+"请求失败",WesyncExceptionPerf.ResponseError);
	    }
	    
//	    long t2 = System.currentTimeMillis();
//    	System.out.println("(" + DateUtilPerf.formateDateTime(new java.util.Date(t1)) + ")(" + DateUtilPerf.formateDateTime(new java.util.Date(t1)) + ")" +
//    			"(" + managerCenter.getUid() + ")(" + (t2 - t1) + "ms)" + requestName + "!");
	    return packet;	
	} catch (InterruptedException e) {
	    throw new WesyncExceptionPerf(requestName+"回复被中断",e,WesyncExceptionPerf.InterruptedException);
	}
    }
    
    /*
     * @Title: folderSync(Asynchronous)
     * @Description: 全量同步用户的所有文件夹
     * @return boolean
     * @throws WesyncException
     */
    protected void folderSync(){
    	String rootId = FolderIDPerf.onRoot(managerCenter.getUid());
    	List<String> childList = new LinkedList<String>();
//    	String flag = "true";
    	while(true){	    
    	    MessageEntityPerf messageEntity = WesyncUtilPerf.folderSync(rootId, wesyncStore);
    	    try {
//    		DebugOutput.println("############################### send folderSync request ###############################");
    		MeyouPerf.MeyouPacket packet = syncQueue(messageEntity,"FolderSync",60);
    		FolderSyncResp resp = FolderSyncResp.parseFrom(packet.getContent().toByteArray());
    		for (String childId : resp.getChildIdList()) {    
//    		    DebugOutput.println("childId = "+ childId);
    		    String split[] = childId.split("-");
    		    String uid = split[2];
    		    if(null != uid && !("".equals(uid))){
//    			DebugOutput.println(uid+";");
    			int firstchr=uid.charAt(0); 
    			if(firstchr >= 48 || firstchr <= 57){ // 数字开头说明是单聊
    			    childList.add(uid);    
    			    wesyncStore.syncKeys.put(childId, WesyncUtilPerf.TAG_SYNC_KEY); //初始化 synckey
    			}else if(firstchr == 103){ // 说明是群的ID
    			    wesyncStore.syncKeys.put(childId, WesyncUtilPerf.TAG_SYNC_KEY); //初始化 synckey	    
    		    	}else {
    		    	    continue; // 其他的不处理
    		    	}		
    		    }
    		}
    		String nextSyncKey = resp.getNextKey();
    		if(SyncKeyPerf.isEmpty(nextSyncKey)) {  // 考虑，如果folderSync不是一次完成的，只能将这个做成同步，否则会造成异常	    		    
    		    break;
    		}
    	    } catch (WesyncExceptionPerf e) {
//    		flag = "false";
    		break;
    	    } catch (InvalidProtocolBufferException e) {
//    		flag = "false";
    		break;
    	    }  
    	}
//    	DebugOutput.println("############################### receive folderSync response #############################");
//    	MeyouNoticePerf notice = new MeyouNoticePerf(NoticeTypePerf.conversation,childList,flag);
//    	NotifyCenterPerf.clientNotifyChannel.add(notice); // 同步完成
    }
    
    
    //createConversation(Sync)：创建会话是个同步的过程，返回folderId
    protected String createConversation(String touid) throws WesyncExceptionPerf{
//	DebugOutput.println("Create conversation with "+ touid);
	String folderId = null;	
	int firstchr=touid.charAt(0); 
	if(firstchr >= 48 || firstchr <= 57){ // 数字开头说明是单聊
	    folderId = FolderIDPerf.onConversation(managerCenter.getUid(), touid);	
	}else if(firstchr == 103){ // 说明是群的ID，g
	    folderId = FolderIDPerf.onGroup(managerCenter.getUid(), touid);	
	    if(!wesyncStore.syncKeys.containsKey(folderId))
		wesyncStore.syncKeys.put(folderId, WesyncUtilPerf.TAG_SYNC_KEY);
	    return folderId;
    	}else{ 
	    throw new WesyncExceptionPerf("必填参数touid无法处理",WesyncExceptionPerf.InputParamError); 
	}
	if(wesyncStore.syncKeys.containsKey(folderId))
	    return folderId;	
	MessageEntityPerf messageEntity = WesyncUtilPerf.folderCreate(touid, wesyncStore);
	MeyouPerf.MeyouPacket packet = syncQueue(messageEntity,"FolderCreate",60);	
	FolderCreateResp resp;
	try {
	    resp = FolderCreateResp.parseFrom(packet.getContent().toByteArray());
	} catch (InvalidProtocolBufferException e) {
//	    DebugOutput.println("FolderCreate Response Error");
	    throw new WesyncExceptionPerf("创建会话请求失败",WesyncExceptionPerf.ResponseError);
	}
	folderId = resp.getFolderId();
	wesyncStore.syncKeys.put(folderId, WesyncUtilPerf.TAG_SYNC_KEY);
//	DebugOutput.println("Create conversation with "+ touid +" successed!");
	return folderId;
    }
    
    protected void weibo(Map<String, String> paramerters, ApiServiceTypePerf apiServiceType,String appName,String withtag,int timeout) throws WesyncExceptionPerf{
	CheckUtilPerf.AuthCheck(flag);
	CheckUtilPerf.MapCheck(paramerters, "paramerters");
	CheckUtilPerf.StringCheck(appName, "appName");
	timeout = CheckUtilPerf.TimeoutCheck(timeout, 1);
	JsonBuilderPerf jsonBuilder = WesyncUtilPerf.weiboJson(paramerters,apiServiceType,appName,managerCenter);
	ByteString content = ByteString.copyFromUtf8(jsonBuilder.toString());
	byte[] entity = MeyouUtilPerf.generateRequestEntity(withtag, MeyouSortPerf.appservice, content);
	MessageEntityPerf messageEntity = new MessageEntityPerf(null,entity);
	if(!httpUtil.startConnection()){
	    throw new WesyncExceptionPerf("网络启动失败",WesyncExceptionPerf.NetworkInterruption);
	}
	setTimer(withtag,timeout); // 设置定时器		
//	DebugOutput.println("############################### send weibo request ###############################");
	try {
	    wesyncStore.messageQ.putMessage(messageEntity);
	} catch (InterruptedException e) {
	    throw new WesyncExceptionPerf("发送微博接口请求被中断",e,WesyncExceptionPerf.InterruptedException);
	}
    } 
    
    protected void weiboExt(Map<String, String> paramerters, ApiServiceTypePerf apiServiceType,String appName,
	    String picfilePath,String withtag,int timeout) throws WesyncExceptionPerf{
	CheckUtilPerf.AuthCheck(flag);
	CheckUtilPerf.MapCheck(paramerters, "paramerters");
	CheckUtilPerf.StringCheck(appName, "appName");
	timeout = CheckUtilPerf.TimeoutCheck(timeout, 1);
	JsonBuilderPerf jsonBuilder = WesyncUtilPerf.weiboJson(paramerters,apiServiceType,appName,managerCenter);
	ByteString content = ByteString.copyFromUtf8(jsonBuilder.toString());
	byte[] entity = MeyouUtilPerf.generateRequestEntityExt(withtag, MeyouSortPerf.appservice, content,picfilePath);	
	MessageEntityPerf messageEntity = new MessageEntityPerf(null,entity);
	if(!httpUtil.startConnection()){
	    throw new WesyncExceptionPerf("网络启动失败",WesyncExceptionPerf.NetworkInterruption);
	}
	setTimer(withtag,timeout); // 设置定时器	
//	DebugOutput.println("############################### send weiboex request ###############################");
	try {
	    wesyncStore.messageQ.putMessage(messageEntity);
	} catch (InterruptedException e) {
	    throw new WesyncExceptionPerf("发送带内容的微博接口请求被中断",e,WesyncExceptionPerf.InterruptedException);
	}	
    }
    
//	DebugOutput.println("发送消息："+meta.getId()+"-------------------------");
    protected void sendMeta(String folderId,Meta meta,int timeout) throws WesyncExceptionPerf{
    if(!httpUtil.startConnection()){
    	System.out.println("网络启动失败");
	    throw new WesyncExceptionPerf("网络启动失败",WesyncExceptionPerf.NetworkInterruption);
	}
	Queue<Meta> queue = wesyncStore.sendMeta.get(folderId);
	if (queue == null) {
	    queue = new LinkedList<Meta>();
	    wesyncStore.sendMeta.put(folderId, queue);
	}
	queue.add(meta);
	wesyncStore.sendMeta.put(folderId, queue);
	setTimer(meta.getId(),timeout); // 设置定时
	WesyncUtilPerf.sync(folderId, wesyncStore);
    }  
    
    protected void sendMetaPerf(String folderId, Meta meta,int timeout) throws WesyncExceptionPerf{
    if(!httpUtil.startConnection()){
    	System.out.println("网络启动失败");
	    throw new WesyncExceptionPerf("网络启动失败",WesyncExceptionPerf.NetworkInterruption);
	}
	Queue<Meta> queue = wesyncStore.sendMeta.get(folderId);
	if (queue == null) {
	    queue = new LinkedList<Meta>();
	    wesyncStore.sendMeta.put(folderId, queue);
	}
	queue.add(meta);
	wesyncStore.sendMeta.put(folderId, queue);
	setTimer(meta.getId(),timeout); // 设置定时
	WesyncUtilPerf.syncPerf(folderId, wesyncStore, httpUtil, managerCenter);
    }

	public HttpUtilPerf getHttpUtil() {
		return httpUtil;
	}  
    
    
}
