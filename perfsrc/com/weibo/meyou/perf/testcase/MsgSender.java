package com.weibo.meyou.perf.testcase;

import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.mina.common.IoSession;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.weibo.meyou.perf.sample.TextContent;
import com.weibo.meyou.perf.sdk.message.NotifyCenterPerf;
import com.weibo.meyou.perf.sdk.protocol.MeyouPerf;
import com.weibo.meyou.perf.sdk.protocol.MeyouSortPerf;
import com.weibo.meyou.perf.util.DateUtilPerf;
import com.weibo.meyou.perf.util.MessageEntityPerf;
import com.weibo.meyou.perf.util.MeyouUtilPerf;

public class MsgSender implements Runnable {
	public static final String sendContent = TextContent.text500;
	private  AtomicInteger counter = new AtomicInteger();
	private String fromuid;
	private IoSession session;
	
	public MsgSender(int fromuid){
		this.fromuid = String.valueOf(fromuid);
		session = NotifyCenterPerf.sessionMap.get(fromuid);
	}
		
	public void run(){
		String callbackId = "Heartbeat";
		int msgId = counter.incrementAndGet();
		byte[] entity = MeyouUtilPerf.generateRequestEntity(callbackId,MeyouSortPerf.heartbeat,ByteString.copyFromUtf8(fromuid + " :" + msgId + "****" + sendContent));
	    MessageEntityPerf messageEntity = new MessageEntityPerf(null,entity);
	    long t1 = System.currentTimeMillis();
	    try {
			sendMessage(messageEntity);
		} catch (InvalidProtocolBufferException e) {
			System.out.println("(" + fromuid + ") failed to send HB...");
		}
		long t2 = System.currentTimeMillis();
		long sendMsgTime = t2 - t1;
		String sendOutTime = DateUtilPerf.formateDateTime(new java.util.Date(t2));
		System.out.println("(" + fromuid + ")(" + sendOutTime + ")(" + sendMsgTime + "ms): " + msgId);
	}
	
    public void sendMessage(MessageEntityPerf messageEntity) throws InvalidProtocolBufferException{
		byte[] body = messageEntity.getHttpEntity();
		byte[] header = getHttpHeader(body.length, messageEntity.getUristr()).getBytes();
		byte[] content = new byte[header.length + body.length];
		System.arraycopy(header, 0, content, 0, header.length);
		System.arraycopy(body, 0, content, header.length, body.length);
        MeyouPerf.MeyouPacket meyouPacket = MeyouUtilPerf.generateRequestPacket("Heartbeat", MeyouSortPerf.heartbeat, content);
		session.write(meyouPacket);
    }
    
	private String getHttpHeader(int length, String uri) {
		InetSocketAddress address = (InetSocketAddress) session.getRemoteAddress();  
		int port = address.getPort();  
		String host = address.getAddress().getHostAddress();
	    StringBuilder sb = new StringBuilder();
	    sb.append("POST /wesync?"+uri+" HTTP/1.1\r\n");
	    sb.append("authorization: "+ session.getAttribute("uid")+"\r\n");
	    sb.append("User-Agent: Jakarta Commons-HttpClient/3.1\r\n");
	    sb.append("Host: "+ host + ":" + port+"\r\n");
	    sb.append("Content-Length: "+ length +	"\r\n\r\n");
	    return sb.toString();
	}
}

