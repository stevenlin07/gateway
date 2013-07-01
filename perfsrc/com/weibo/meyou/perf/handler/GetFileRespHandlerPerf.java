package com.weibo.meyou.perf.handler;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.List;
import java.util.Timer;
import com.google.protobuf.InvalidProtocolBufferException;
import com.weibo.meyou.perf.sdk.message.FileMessagePerf;
import com.weibo.meyou.perf.sdk.protocol.MeyouPerf.MeyouPacket;
import com.weibo.meyou.perf.sdk.protocol.WeSyncMessagePerf.DataSlice;
import com.weibo.meyou.perf.sdk.protocol.WeSyncMessagePerf.FileData;
import com.weibo.meyou.perf.util.HttpUtilPerf;
import com.weibo.meyou.perf.util.ManagerCenterPerf;
import com.weibo.meyou.perf.util.WesyncStorePerf;

/*
 * @ClassName: GetFileRespHandler 
 * @Description: TODO(这里用一句话描述这个类的作用) 
 * @author LiuZhao
 * @date 2012-9-4 下午9:25:21  
 */
public class GetFileRespHandlerPerf extends BasicRespHandlerPerf {

	public GetFileRespHandlerPerf(WesyncStorePerf wesyncStore, ManagerCenterPerf managerCenter, HttpUtilPerf httpUtil){
		super(wesyncStore, managerCenter, httpUtil);
	}
	
    @Override
    public void handle(MeyouPacket meyouPacket) {
//	DebugOutput.println("开始处理下载的文件包 ---------------------");
	FileData resp = null;
	try {
	    resp = FileData.parseFrom(meyouPacket.getContent().toByteArray());
	} catch (InvalidProtocolBufferException e) {
//	    DebugOutput.println("下载文件返回结果处理异常!");
	    return;
	}
	
	String downloadFileId = resp.getId();// 获取缓存的文件下载方式
	// 如果超时之后，说明此文件已经被清理掉了，则试图清理掉这个等待文件下载的list
	if(!wesyncStore.tagTimeList.containsKey(downloadFileId)){
	    getFileHandlerQuit(downloadFileId);
	    return;
	}	
	FileMessagePerf fileMessage = wesyncStore.downloadFileMessage.get(downloadFileId);
	String downloadPath = fileMessage.msgId;
	int fileLength = fileMessage.fileLength;
	int sliceCount = fileMessage.limit; // 分片总数
	List<Integer> receiveList = fileMessage.hasReveive;
	File downloadFile = new File(downloadPath);
	boolean unCreate = true;   // 默认是没有创建过这个文件
	if(downloadFile.exists()){ // 如果这个文件存在说明创建过
	    unCreate = false;
	}
	RandomAccessFile raf;
	try {
	    raf = new RandomAccessFile(downloadFile,"rw");
	    if(unCreate){ 
		raf.setLength(fileLength);
	    }
	    if(null == resp.getSliceList() || 0 == resp.getSliceList().size()){
//		DebugOutput.println("下载文件结果内容为空!");
		raf.close();
		return;
	    }
	    for(DataSlice s : resp.getSliceList()){
		int index = s.getIndex();
		if(receiveList.contains(index)){// 说明重复已经处理
		    continue;
		}
		raf.seek(managerCenter.getSliceSize()*1024*(index-1));
		raf.write(s.getData().toByteArray());
		receiveList.add(index);
	    } 
	    fileMessage.hasReveive = receiveList;
	    raf.close();
	} catch (FileNotFoundException e) {
//	    WesyncExceptionPerf wesyncException = new WesyncExceptionPerf("下载后的文件路径不存在",e,
//		    WesyncExceptionPerf.BadFilePath);
//	    MeyouNoticePerf notice = new MeyouNoticePerf(NoticeTypePerf.exception,wesyncException,wesyncException.getMessage());
//	    NotifyCenterPerf.clientNotifyChannel.add(notice);
	    getFileHandlerQuit(downloadFileId);
	    return;
	} catch (IOException e) {
//	    WesyncExceptionPerf wesyncException = new WesyncExceptionPerf("下载文件后本地写入失败",e,
//		    WesyncExceptionPerf.BadFileWrite);
//	    MeyouNoticePerf notice = new MeyouNoticePerf(NoticeTypePerf.exception,wesyncException,wesyncException.getMessage());
//	    NotifyCenterPerf.clientNotifyChannel.add(notice);
	    getFileHandlerQuit(downloadFileId);
	    return;
	}
	wesyncStore.downloadFileMessage.put(downloadFileId, fileMessage);
	// 判断是否已经下载完成,
	if(sliceCount <= receiveList.size()){ //如果没有下载完成就继续等待接收    
	    getFileHandlerQuit(downloadFileId);
	}
//	MeyouNoticePerf notice = new MeyouNoticePerf(NoticeTypePerf.downloadfile,fileMessage,downloadFileId);
//	NotifyCenterPerf.clientNotifyChannel.add(notice);	
    }
    
    private void getFileHandlerQuit(String downloadFileId){
	wesyncStore.downloadFileMessage.remove(downloadFileId);
	if(wesyncStore.tagTimeList.containsKey(downloadFileId))
		return; // 说明处理过程中已经超时了
	Timer t = wesyncStore.tagTimeList.get(downloadFileId);// 说明接收成功
	t.cancel();
	wesyncStore.tagTimeList.remove(downloadFileId); 
    }
}
