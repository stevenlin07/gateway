package com.weibo.wesync.notify.service.rpc.handler;

import java.nio.ByteBuffer;

import org.apache.thrift.TException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.weibo.wesync.WeSyncService;
import com.weibo.wesync.data.WeSyncMessage.Notice;
import com.weibo.wesync.notify.service.rpc.Notify;

/**
 * 
 * @auth linlin9@staff.sina.com.cn 
 */
public class NotifyHandler implements Notify.Iface{
	private static final Logger log = LoggerFactory.getLogger(NotifyHandler.class);
	private WeSyncService weSync = null;
	
	public NotifyHandler(WeSyncService wesync){
		this.weSync = wesync;
	}
	
	@Override
	public void send(String touid, ByteBuffer noticebb) throws TException {
		Notice notice = null;
		
		int length = noticebb.limit()- noticebb.position();	
		byte[] recvContent = new byte[length];
		noticebb.get(recvContent, 0, length);
		
		try {
			notice = Notice.parseFrom(ByteString.copyFrom(recvContent));
		} catch (InvalidProtocolBufferException e) {
			log.error("Fail to parse notice from rpc read byte buffer.", e);
		}
		this.weSync.getNoticeService().send(touid, notice);
	}

	@Override
	public void sendSimple(String text) throws TException {
		System.out.println("Hello sendsimple: "+ text);
	}
}