package com.weibo.meyou.perf.util;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.Timer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

import com.weibo.meyou.perf.sdk.message.FileMessagePerf;
import com.weibo.meyou.perf.sdk.protocol.MeyouPerf;
import com.weibo.meyou.perf.sdk.protocol.WeSyncMessagePerf.Meta;


/*
 * @ClassName: WesyncStore 
 * @Description: Wesync协议的内部存储
 * @author LiuZhao
 * @date 2012-9-4 上午11:23:23  
 */
public class WesyncStorePerf {

    public void clearWesyncStore(){
	syncBlockingQ.clear();
	messageQ.clear();
	syncKeys.clear();
	
	for(String msgId:tagTimeList.keySet()){
	    Timer t = tagTimeList.get(msgId);
	    t.cancel();
	    tagTimeList.remove(msgId);
	}
	tagTimeList.clear();
	downloadFileMessage.clear();
	syncFolder.clear();
	sendMeta.clear();
    }
    // 专门用来为那些必须同步的操作准备的map ，不存在竞争，HashMap即可，无需静态，因为存储本身就是单例
    public LinkedBlockingQueue<MeyouPerf.MeyouPacket> syncBlockingQ = new LinkedBlockingQueue<MeyouPerf.MeyouPacket>();
     // 与Http对接的两级队列
    public MessageBlockingQueuePerf messageQ = new MessageBlockingQueuePerf();
    // folderId -- syncKey 用来标识每个文件的读取进度，存在竞争，需要同步
    public Map<String,String> syncKeys = new ConcurrentHashMap<String,String>();
    // 下载文件fileId -- FileMessage
    public Map<String,FileMessagePerf> downloadFileMessage = new ConcurrentHashMap<String,FileMessagePerf>();   
    // 存储发送出去的消息ID，以及对应的定时器
    public Map<String,Timer> tagTimeList = new ConcurrentHashMap<String,Timer>();
    // 用于在发消息之前暂存Meta消息的map folderId --> Meta的队列，消息发送之后，即移除这个消息
    public Map<String, Queue<Meta>> sendMeta = new ConcurrentHashMap<String, Queue<Meta>>();
  
    // 判断当前的folder是否在做sync，如果存在，就跳出  
    public Set<String> syncFolder = Collections.synchronizedSet(new HashSet<String>());
	 
    // 判断当前的folder是否在做sync，如果存在，就跳出  
    public Set<String> noticeFolder = new HashSet<String>();
 
	  
    // 命令对应的base64编码后的信息
    public Map<String,String> uriMap = new ConcurrentHashMap<String,String>();
}
