package com.weibo.meyou.perf;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import com.weibo.meyou.perf.sdk.message.FileMessagePerf;
import com.weibo.meyou.perf.sdk.message.WesyncExceptionPerf;
import com.weibo.meyou.perf.sdk.protocol.FileIDPerf;
import com.weibo.meyou.perf.sdk.protocol.MetaMessageTypePerf;
import com.weibo.meyou.perf.sdk.protocol.MeyouPerf;
import com.weibo.meyou.perf.sdk.protocol.MeyouSortPerf;
import com.weibo.meyou.perf.sdk.protocol.WeSyncMessagePerf.Meta;
import com.weibo.meyou.perf.util.CheckUtilPerf;
import com.weibo.meyou.perf.util.DateUtilPerf;
import com.weibo.meyou.perf.util.MessageEntityPerf;
import com.weibo.meyou.perf.util.MeyouUtilPerf;
import com.weibo.meyou.perf.util.WesyncUtilPerf;

public class WesyncApiImplPerf extends BasicImplPerf implements WesyncApiPerf{
    @Override
    public boolean authUser(String username,String password,int timeout) throws WesyncExceptionPerf{
	if(flag) return true; // 如果已经认证过了，就无需再进行认证	    
	managerCenter.checkDeviceInfo(); // 检查设备信息
//	CheckUtil.StringCheck(username,"username"); // 检查用户名
//	CheckUtil.StringCheck(password,"password"); // 检查密码
	if (null == username || null == password) { // 校验用户的参数，用户名，密码
		throw new WesyncExceptionPerf("输入参数错误：必填参数不能为空",
				WesyncExceptionPerf.InputParamError);
	}
	
	timeout = CheckUtilPerf.TimeoutCheck(timeout,2); // 参数校验及超时时间设置
	managerCenter.setUsernamePassword(username, password); // 存储用户名和密码，内部会进行密码加密
	byte[] entity = MeyouUtilPerf.generateRequestEntity("HandShake", MeyouSortPerf.handshake, null);
	MessageEntityPerf messageEntity = new MessageEntityPerf(null,entity);	
//	DebugOutput.println("############################### send auth request ###############################");
	try {	    	
	    MeyouPerf.MeyouPacket packet = syncQueue(messageEntity,"Auth",timeout);
	    managerCenter.setUid(packet.getUid()); // 存储该用户的UID
	} catch (WesyncExceptionPerf e) {
	    logout(); // 退出SDK
	    throw new WesyncExceptionPerf(e.getMessage(),e,e.getStatusCode());
	}
//	DebugOutput.println("############################### receive auth response #############################");
	folderSync(); // 全量同步所有root下的文件文件夹
	WesyncUtilPerf.getUnreadItem(wesyncStore, httpUtil, managerCenter); // 主动获取未读数，在发送心跳之前
//	this.heartBeat = new HeartBeatPerf(this.wesyncStore, this.httpUtil, Thread.currentThread().getName());
//	heartBeat.addTimer();
//	heartBeat.startTimer(240,200); // 启动心跳管理，并发送通知，单位秒，包括：多少秒后启动，以后每多少秒执行一次
	flag = true; // 设置认证位置为true，表示认证成功
	return true; 	    
    }  
    
    @Override
    public boolean sendText(String msgId,String touid,String text,byte[] padding,int timeout)
	    throws WesyncExceptionPerf {
//	DebugOutput.println("say to "+touid +":"+ text);
    	CheckUtilPerf.AuthCheck(flag);
    	int metaLength = CheckUtilPerf.StringCheck(msgId,"msgId") + CheckUtilPerf.StringCheck(text,"text") +
    		CheckUtilPerf.TouidCheck(touid, managerCenter.getUid());
    	CheckUtilPerf.InputLengthCheck(metaLength, padding);
    	String folderId = createConversation(touid);	
    	//System.out.println("(" + Thread.currentThread().getName() + ") create folder " + folderId);
    	Meta meta = WesyncUtilPerf.generatedTextMeta(msgId,touid,text,padding,managerCenter);
    	sendMeta(folderId,meta,timeout);
    	return true;
    }

    public boolean sendTextPerf(String msgId,String touid,String text,byte[] padding,int timeout)
    	    throws WesyncExceptionPerf {
//    	DebugOutput.println("say to "+touid +":"+ text);
        	CheckUtilPerf.AuthCheck(flag);
//        	int metaLength = CheckUtilPerf.StringCheck(msgId,"msgId") + CheckUtilPerf.StringCheck(text,"text") +
//        		CheckUtilPerf.TouidCheck(touid, managerCenter.getUid());
//        	CheckUtilPerf.InputLengthCheck(metaLength, padding);
//        	long t1 = System.currentTimeMillis();
        	String folderId = createConversation(touid);
//        	long t2 = System.currentTimeMillis();
//        	System.out.println("(" + DateUtilPerf.formateDateTime(new java.util.Date(t1)) + ")(" + DateUtilPerf.formateDateTime(new java.util.Date(t1)) + ")" +
//        			"(" + managerCenter.getUid() + ")(" + msgId + ")(" + (t2 - t1) + "ms) Create conversation!");
        	//System.out.println("(" + Thread.currentThread().getName() + ") create folder " + folderId);
//        	long t1 = System.currentTimeMillis();
        	Meta meta = WesyncUtilPerf.generatedTextMeta(msgId,touid,text,padding,managerCenter);
        	sendMetaPerf(folderId,meta,timeout);
//        	long t2 = System.currentTimeMillis();
//        	System.out.println("(" + managerCenter.getUid() + ")(" + DateUtilPerf.formateDateTime(new java.util.Date(t1)) + ")(" + DateUtilPerf.formateDateTime(new java.util.Date(t2)) + ")" +
//			"(" + msgId + ")(" + (t2 - t1) + "ms) Send meta message!");       	
        	return true;
        }
    
    
    @Override
    public boolean sendAudio(String msgId, String touid,
	    byte[] audioData,byte[] padding,int timeout) throws WesyncExceptionPerf{
//	DebugOutput.println("sound to "+touid);
	CheckUtilPerf.AuthCheck(flag);
	CheckUtilPerf.AuthCheck(flag);
	int metaLength = CheckUtilPerf.StringCheck(msgId,"msgId") + CheckUtilPerf.TouidCheck(touid, managerCenter.getUid())
		+ CheckUtilPerf.ByteArrayCheck(audioData,"audioData");
	CheckUtilPerf.InputLengthCheck(metaLength, padding);
	timeout = CheckUtilPerf.TimeoutCheck(timeout,1);	
	String folderId = createConversation(touid); // 创建会话
	Meta meta  = WesyncUtilPerf.generatedVoiceMeta(msgId,touid,audioData,padding,managerCenter);
	sendMeta(folderId,meta,timeout);
	return true;
    }
    
    public boolean sendAudioPerf(String msgId, String touid,
    	    byte[] audioData,byte[] padding,int timeout) throws WesyncExceptionPerf{
//    	DebugOutput.println("sound to "+touid);
    	CheckUtilPerf.AuthCheck(flag);
//    	int metaLength = CheckUtilPerf.StringCheck(msgId,"msgId") + CheckUtilPerf.TouidCheck(touid, managerCenter.getUid())
//    		+ CheckUtilPerf.ByteArrayCheck(audioData,"audioData");
//    	CheckUtilPerf.InputLengthCheck(metaLength, padding);
    	timeout = CheckUtilPerf.TimeoutCheck(timeout,1);	
    	String folderId = createConversation(touid); // 创建会话
//    	long t1 = System.currentTimeMillis();
    	Meta meta  = WesyncUtilPerf.generatedVoiceMeta(msgId,touid,audioData,padding,managerCenter);
    	sendMetaPerf(folderId,meta,timeout);
//    	long t2 = System.currentTimeMillis();
//    	System.out.println("(" + managerCenter.getUid() + ")(" + DateUtilPerf.formateDateTime(new java.util.Date(t1)) + ")(" + DateUtilPerf.formateDateTime(new java.util.Date(t2)) + ")" +
//		"(" + msgId + ")(" + (t2 - t1) + "ms) Send meta audio!");      	
    	return true;
        }
    
    
    @Override
    public int sendFile(String msgId,String touid,String filepath,String filename,MetaMessageTypePerf type,
	    int[] index,byte[] padding,byte[] thumbnail,int timeout) throws WesyncExceptionPerf{
//	DebugOutput.println("发送文件：请求ID = "+ msgId + ";接收方 = " + touid);
    	CheckUtilPerf.AuthCheck(flag);
    	int metaLength = CheckUtilPerf.StringCheck(msgId,"msgId") + + CheckUtilPerf.StringCheck(filepath,"filepath") +
    		CheckUtilPerf.StringCheck(filename,"filename") + CheckUtilPerf.TouidCheck(touid, managerCenter.getUid())+ 
    		CheckUtilPerf.StringCheck(type.toString(),"type") + CheckUtilPerf.StringCheck(filename, "filename");
    	CheckUtilPerf.InputLengthCheck(metaLength, padding);
    	String folderId = createConversation(touid);		
    	String fileId = FileIDPerf.generateId(managerCenter.getUid(), touid, msgId);
    	List<Integer> indexList = null;
    	if(null != index){ // 针对重传的处理,缓存所有的需要重传的分片号
    	    indexList = new LinkedList<Integer>();
    	    for(int j=0;j<index.length;j++){
    		indexList.add(index[j]);
    	    }
    	}
    	// 分片大小，sliceSize，fileLength单位KB，fileLengthByte长度单位 byte
    	int sliceSize = managerCenter.getSliceSize();	
    	int fileLengthByte;
    	FileInputStream fis = null;
    	try {
    	    fis = new FileInputStream(new File(filepath));
    	    fileLengthByte = fis.available();
//    	    DebugOutput.println("发送文件的大小："+ fileLengthByte);
    	    if(fileLengthByte <= 0){
    		throw new WesyncExceptionPerf("发送文件不能为空",WesyncExceptionPerf.BadFileRead);
    	    }
    	    int remainder = (int)((fileLengthByte/1024) % sliceSize); // 分片的余数
    	    int limit = ( 0 == remainder ? (fileLengthByte/1024) / sliceSize : (fileLengthByte/1024) / sliceSize + 1 );// 确定分片的数量
    	    byte[] sendBuf = new byte[sliceSize*1024];	// 根据分片的大小来划分分片
    	    byte[] lastSendBuf = new byte[fileLengthByte-(limit-1)*sliceSize*1024];
    	    for( int i = 1; i <= limit; i++){
    		if(null != indexList && !indexList.contains(i)) 
    		    continue; // 如果indexList非空，说明属于重传操作，和 indexList 列表为空	
    		if( i != limit){
    		    fis.read(sendBuf);
//    		    DebugOutput.println("文件ID："+fileId+"---发送第"+i+"片"+"----大小："+sendBuf.length);
    		    MessageEntityPerf messageEntity = WesyncUtilPerf.sendFile(i,limit, sendBuf,fileId,wesyncStore);
    		    wesyncStore.messageQ.putFileBlock(messageEntity);		    
    		}else{
    		    fis.read(lastSendBuf);
//    		    DebugOutput.println("文件ID："+fileId+"---发送第"+i+"片"+"----大小："+lastSendBuf.length);
    		    MessageEntityPerf messageEntity = WesyncUtilPerf.sendFile(i,limit, lastSendBuf,fileId,wesyncStore);
    		    wesyncStore.messageQ.putFileBlock(messageEntity);	
    		}
    	    }
    	    timeout = CheckUtilPerf.TimeoutCheck(timeout,limit);
    	    setTimer(fileId,timeout); // 设置定时器    
    	    Meta meta  = WesyncUtilPerf.generatedFileMeta(msgId,fileId,touid,type,thumbnail,padding,fileLengthByte,filename,managerCenter);    
    	    sendMeta(folderId,meta,60);
    	    return limit;	
    	} catch (IOException e) {
    	    throw new WesyncExceptionPerf("要发送的文件读取失败",e,WesyncExceptionPerf.BadFileRead);
    	} catch (InterruptedException e) {
    	    throw new WesyncExceptionPerf("sendfile请求返回被中断",e,WesyncExceptionPerf.InterruptedException);
    	} finally {
    	    try {
    		fis.close();
    	    } catch (IOException e) {
    	    }
    	}
    }

    
    public int sendFilePerf(String msgId,String touid,String filepath,String filename,MetaMessageTypePerf type,
    	    int[] index,byte[] padding,byte[] thumbnail,int timeout) throws WesyncExceptionPerf{
//    	DebugOutput.println("发送文件：请求ID = "+ msgId + ";接收方 = " + touid);
        	CheckUtilPerf.AuthCheck(flag);
        	int metaLength = CheckUtilPerf.StringCheck(msgId,"msgId") + + CheckUtilPerf.StringCheck(filepath,"filepath") +
        		CheckUtilPerf.StringCheck(filename,"filename") + CheckUtilPerf.TouidCheck(touid, managerCenter.getUid())+ 
        		CheckUtilPerf.StringCheck(type.toString(),"type") + CheckUtilPerf.StringCheck(filename, "filename");
        	CheckUtilPerf.InputLengthCheck(metaLength, padding);
        	String folderId = createConversation(touid);		
        	String fileId = FileIDPerf.generateId(managerCenter.getUid(), touid, msgId);
        	List<Integer> indexList = null;
        	if(null != index){ // 针对重传的处理,缓存所有的需要重传的分片号
        	    indexList = new LinkedList<Integer>();
        	    for(int j=0;j<index.length;j++){
        		indexList.add(index[j]);
        	    }
        	}
        	// 分片大小，sliceSize，fileLength单位KB，fileLengthByte长度单位 byte
        	int sliceSize = managerCenter.getSliceSize();	
        	int fileLengthByte;
        	FileInputStream fis = null;
        	try {
        	    fis = new FileInputStream(new File(filepath));
        	    fileLengthByte = fis.available();
//        	    DebugOutput.println("发送文件的大小："+ fileLengthByte);
        	    if(fileLengthByte <= 0){
        		throw new WesyncExceptionPerf("发送文件不能为空",WesyncExceptionPerf.BadFileRead);
        	    }
        	    int remainder = (int)((fileLengthByte/1024) % sliceSize); // 分片的余数
        	    int limit = ( 0 == remainder ? (fileLengthByte/1024) / sliceSize : (fileLengthByte/1024) / sliceSize + 1 );// 确定分片的数量
        	    byte[] sendBuf = new byte[sliceSize*1024];	// 根据分片的大小来划分分片
        	    byte[] lastSendBuf = new byte[fileLengthByte-(limit-1)*sliceSize*1024];
        	    for( int i = 1; i <= limit; i++){
        		if(null != indexList && !indexList.contains(i)) 
        		    continue; // 如果indexList非空，说明属于重传操作，和 indexList 列表为空	
        		if( i != limit){
        		    fis.read(sendBuf);
//        		    DebugOutput.println("文件ID："+fileId+"---发送第"+i+"片"+"----大小："+sendBuf.length);
        		    MessageEntityPerf messageEntity = WesyncUtilPerf.sendFile(i,limit, sendBuf,fileId,wesyncStore);
        		    wesyncStore.messageQ.putFileBlock(messageEntity);		    
        		}else{
        		    fis.read(lastSendBuf);
//        		    DebugOutput.println("文件ID："+fileId+"---发送第"+i+"片"+"----大小："+lastSendBuf.length);
        		    MessageEntityPerf messageEntity = WesyncUtilPerf.sendFile(i,limit, lastSendBuf,fileId,wesyncStore);
        		    wesyncStore.messageQ.putFileBlock(messageEntity);	
        		}
        	    }
        	    timeout = CheckUtilPerf.TimeoutCheck(timeout,limit);
        	    setTimer(fileId,timeout); // 设置定时器    
        	    Meta meta  = WesyncUtilPerf.generatedFileMeta(msgId,fileId,touid,type,thumbnail,padding,fileLengthByte,filename,managerCenter);    
        	    sendMetaPerf(folderId,meta,60);
        	    return limit;	
        	} catch (IOException e) {
        	    throw new WesyncExceptionPerf("要发送的文件读取失败",e,WesyncExceptionPerf.BadFileRead);
        	} catch (InterruptedException e) {
        	    throw new WesyncExceptionPerf("sendfile请求返回被中断",e,WesyncExceptionPerf.InterruptedException);
        	} finally {
        	    try {
        		fis.close();
        	    } catch (IOException e) {
        	    }
        	}
        }    
    
    @Override
    public boolean downloadFile(String fileId,String downloadPath,int filelengthByte,int[] index,int timeout) throws WesyncExceptionPerf{
//	DebugOutput.println("下载文件："+fileId+";文件长度："+ filelengthByte);
	CheckUtilPerf.AuthCheck(flag);
	int metaLength = CheckUtilPerf.StringCheck(fileId,"fileId") + CheckUtilPerf.StringCheck(downloadPath,"downloadPath") 
		+ CheckUtilPerf.IntCheck(filelengthByte, "filelengthByte");
	CheckUtilPerf.InputLengthCheck(metaLength,null);
	if(!httpUtil.startConnection()){
	    throw new WesyncExceptionPerf("网络启动失败",WesyncExceptionPerf.NetworkInterruption);
	}
	int sliceSize = managerCenter.getSliceSize();
	int remainder = (int)((filelengthByte/1024) % sliceSize);
	int limit = ( 0 == remainder ?  (filelengthByte/1024) / sliceSize :  (filelengthByte/1024) / sliceSize + 1 ); // 分片总数
	FileMessagePerf fileMessage = new FileMessagePerf();
	fileMessage.fileId = fileId; // 文件ID
	fileMessage.msgId = downloadPath; // 文件下载后保存的路径
	fileMessage.fileLength = filelengthByte; // 文件的长度，byte
	fileMessage.limit = limit; // 分片的数量
	fileMessage.hasReveive = new ArrayList<Integer>(); 
	wesyncStore.downloadFileMessage.put(fileId, fileMessage); 
	timeout = CheckUtilPerf.TimeoutCheck(timeout,limit);
	setTimer(fileId,timeout); // 设置定时器
	List<Integer> indexList = null;
	if(null != index){  // 针对重传的处理,缓存所有的需要重传的分片号
	    indexList = new LinkedList<Integer>();
	    for(int j=0;j<index.length;j++){
		indexList.add(index[j]);
	    }
	}
	for(int i=1;i<=limit;i++){
	    if(null != indexList && !indexList.contains(i)) 
		continue; // 如果indexList非空，说明属于重传操作，和 indexList 列表为空
//	    DebugOutput.println("文件:"+fileId+"共"+limit+"片的第"+i+"片的下载请求");
	    MessageEntityPerf messageEntity = WesyncUtilPerf.getFile(fileId,limit,i,wesyncStore);	
//	    DebugOutput.println("############################### send donwload request ###############################");
	    try {
		wesyncStore.messageQ.putMessage(messageEntity);
	    } catch (InterruptedException e) {
		throw new WesyncExceptionPerf("downloadFile请求被中断",e,WesyncExceptionPerf.InterruptedException);
	    }
	}
	return true;
    }

    @Override
    public boolean logout(){	
//	DebugOutput.println("User exec logout");
	flag = false;
//	heartBeat.stopTimer(); // 停止发送心跳包
	httpUtil.clear(); // 清空管理中心	
//	NotifyCenter.closeNotifyCenter(); // 清空通知中心
	wesyncStore.clearWesyncStore(); // 清空SDK的存储
	try {
	    Thread.sleep(1000);
	} catch (InterruptedException e) {
	}
//	DebugOutput.println("User exec logout completed");
	return true;
    }
    
    @Override
    public void uploadContact(List<String> contact,int timeout) throws WesyncExceptionPerf{	
	CheckUtilPerf.AuthCheck(flag);
	int metaLength = CheckUtilPerf.GroupListCheck(contact, "contact",managerCenter.getUid());
	CheckUtilPerf.InputLengthCheck(metaLength,null);
	timeout = CheckUtilPerf.TimeoutCheck(timeout,1);
	MessageEntityPerf messageEntity = WesyncUtilPerf.uploadContact(contact);
	setTimer("contact",timeout); // 设置定时器	
	httpUtil.startConnection();
	try {
	    wesyncStore.messageQ.putMessage(messageEntity);
	} catch (InterruptedException e) {
	    throw new WesyncExceptionPerf("发送上传通讯录请求被中断",e,WesyncExceptionPerf.InterruptedException);
	}
    }
}
