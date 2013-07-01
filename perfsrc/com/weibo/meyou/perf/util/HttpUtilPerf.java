package com.weibo.meyou.perf.util;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.concurrent.LinkedBlockingQueue;

import com.weibo.meyou.perf.handler.ResponseProcessorPerf;
import com.weibo.meyou.perf.sdk.message.NotifyCenterPerf;
import com.weibo.meyou.perf.sdk.message.WesyncExceptionPerf;
import com.weibo.meyou.perf.sdk.protocol.MeyouPerf;

/*
 * @ClassName: HttpUtil 
 * @Description: 进行连接管理，采用NIO的方式：一个发送线程，一个接收线程
 * @author LiuZhao
 * @date 2012-9-13 下午1:22:11  
 */

public class HttpUtilPerf{

    private Selector selector;   
    private SocketChannel socketChannel;
    private ResponseProcessorPerf responseProcessor = null;
    private WesyncStorePerf wesyncStore = null;
    private ManagerCenterPerf managerCenter = null;  
    public boolean isReceiveThreadQuit = true;  
    public boolean reconnected = true;
    private Thread receiverThread;
    
    public HttpUtilPerf(WesyncStorePerf wesyncStore, ManagerCenterPerf managerCenter){
		if ((wesyncStore == null) || (managerCenter == null))
			throw new IllegalArgumentException();
		this.wesyncStore = wesyncStore;
		this.managerCenter = managerCenter;
		this.responseProcessor = new ResponseProcessorPerf(this.wesyncStore, this.managerCenter, this);
    }
    
    private void preStop() throws Exception{
		if(isReceiveThreadQuit){
			System.err.println("接收线程异常退出，清理接收线程！！！！");
			receiverThread.interrupt();
		}
		
		if(null != selector && selector.isOpen()){
		    selector.close(); // 终止selector--接收线程会退出		
		}
		if(null != socketChannel && socketChannel.isOpen()){
		    socketChannel.close(); 
		}
		Thread.sleep(100);		
    }
    
    public void stop(){
		try {
		    preStop();
		} catch (Exception e) {
		}
	
		isReceiveThreadQuit = true;
		reconnected = true;
    }

    public boolean start(){
    	if(!isReceiveThreadQuit){
    		return true;
    	}
	
    	if(!reconnected){
    		return true;
    	}
	// 如果执行以下的代码，说明SDK的收发线程一定是停止的，并且心跳已经停止
		try {
		    selector = Selector.open();
		    socketChannel = SocketChannel.open();
		    socketChannel.configureBlocking(false);
		    socketChannel.connect(new InetSocketAddress(managerCenter.getServerIp(),managerCenter.getServerPort()));
		    socketChannel.register(selector, SelectionKey.OP_CONNECT);
		    if(selector.select()>0){
			for(SelectionKey key:selector.selectedKeys()){	       
			    if(key.isConnectable()){
				key.interestOps(SelectionKey.OP_READ);
			    }
			}
		    }
		    socketChannel.finishConnect();
		    receiverThread = new Thread(new ClientReceive(Thread.currentThread().getName()));
		    receiverThread.start();
		} catch (Exception e) {
		    reconnected = true;
		    return false;
		}
		
		reconnected = false;
		 return true;
    }
    
    
    // 启动网络
    public boolean startConnection(){
		boolean startflag = false;
		int[] ports = managerCenter.getServerPorts();
		for(int i = 0 ;i< ports.length;i++){
		    managerCenter.setServerPort(i);
		    startflag = start();
		    if(startflag){
		    	return startflag;
		    }
		}
	
		return startflag;
    }

    // 停止网络
    public void stopConnection(){
    	stop();
    }
    
    public void clear(){
    	stop();
    	managerCenter.clear();
    }
    
    class ClientReceive implements Runnable{
	
    private ByteBuffer receiveByteBuffer = ByteBuffer.allocate(102400);    	
	private String name;
	
	public ClientReceive(String name){
		this.name = name;
	}
	
	
	@Override
	public void run() {
		Thread.currentThread().setName(this.name);
	    isReceiveThreadQuit = false;
	    while(true){
			try{
			    selector.select(); // 断网之后，会不断地返回可读事件。算是bug么？	    
			    for(SelectionKey key:selector.selectedKeys()){       
					if(key.isReadable()){
					    selector.selectedKeys().remove(key);
					    SocketChannel socketChannel = (SocketChannel) key.channel();
					    dealResult(socketChannel);
				     	key.interestOps(SelectionKey.OP_READ);	   
					}
			    }	       
			}catch(Exception e){ // 可能抛出的异常包括：IOException，ClosedSelectorException
			    // 都是可以关闭连接的异常，则让该线程退出即可
//			    WesyncExceptionPerf wesyncException = new WesyncExceptionPerf("网络中断",e,
//					WesyncExceptionPerf.NetworkInterruption);
//			    MeyouNoticePerf notice = new MeyouNoticePerf(NoticeTypePerf.exception,wesyncException,
//					wesyncException.getMessage());
//			    NotifyCenterPerf.clientNotifyChannel.add(notice);
			    System.err.println("当前接收线程异常退出 "+Thread.currentThread().getName());
			    isReceiveThreadQuit = true;
			    reconnected = true;
			    break;
			}
	    }
	    receiveThreadQuit(); //接收线程退出
	}
	
	public void dealResult(SocketChannel socketChannel) throws WesyncExceptionPerf{  // 进行结果预处理
	    try {
	    	while((socketChannel.read(receiveByteBuffer))>0) {
		}
	    } catch (IOException e) {
		// 从channel中读取数据错误，在网络启动后断网，发生 Operation timed out,这个异常，网络断掉了，则退出两条线程
	    	receiveByteBuffer.clear();
	    	throw new WesyncExceptionPerf("从网络中读取数据失败，网络中断，重启网络",e,WesyncExceptionPerf.NetworkInterruption);
	    }
	    
	    byte[] buf = new byte[receiveByteBuffer.position()];
	    receiveByteBuffer.position(0); // 设置position = 0
	    receiveByteBuffer.get(buf);    // 将receiveByteBuffer的内容存入buf缓冲区中
	    receiveByteBuffer.clear();     // 清空缓冲区即可
	    
	    if(buf.length <= 0){ // 网络中断收到空包
	    	System.err.println(Thread.currentThread().getName()+">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>空包，网络中断");
	    	throw new WesyncExceptionPerf("接收到空包，网络中断",WesyncExceptionPerf.NetworkInterruption);
	    }
	    
	    // 进入下面的逻辑，因为没有收到空数据
	    int packetLength = 0; // 一个独立的数据包的总长度
	    int position = 0;    
 
	    while(position < buf.length){ // 循环是因为包没有处理完毕
			byte[] tempPacket = new byte[buf.length-position];
			System.arraycopy(buf,position, tempPacket, 0, tempPacket.length);
			int contentLength = getRespContentLength(tempPacket);
	//		DebugOutput.println("reminder:"+tempPacket.length+";position:"+position+";contentLength:"+contentLength);
			if(contentLength <= 0){ //数据包不完整，将其重新存入接收缓冲区继续攒包
//			    receiveByteBuffer.put(tempPacket,0,tempPacket.length); 
	//		    DebugOutput.println("package is not completed,save it to receive buffer again:"+receiveByteBuffer.position());
	//		    System.out.println("数据包不完整，将其重新存入接收缓冲区继续攒包 = " + receiveByteBuffer.position());
//			    if(tempPacket.length >= 256){ //经验值，如果在这么长的内容中仍然无法计算出contentLength，视为数据包乱序，可以暂时停止接收数据包
	//			DebugOutput.println("reveive a bad package");
//				}
			    return; 		
			}		
			packetLength = lastResult(tempPacket, contentLength); // 处理数据包
			if (0 == packetLength || -1 == packetLength) { // 可认为需要攒包，或者包处理出错，丢掉
				receiveByteBuffer.clear();
				if (0 == packetLength) {
					receiveByteBuffer.put(buf, position, buf.length
							- position);
				}
				return;
			}
			position = packetLength + position;
	    }

	    return;
	}
	
	private int getRespContentLength(byte[] resp) {
			try {
				int headoff = 0;
				for (int i = 0; i < resp.length; i++) {
					if (i + 1 < resp.length && resp[i] == '\r'
							&& resp[i + 1] == '\n') {
						byte[] temp = new byte[i];
						System.arraycopy(resp, headoff, temp, 0, i - headoff);
						String line = new String(temp);
						String[] tokens = line.split(": ");
						i += 2;
						headoff = i;
						if ("Content-Length".equals(tokens[0])) {
							return Integer.valueOf(tokens[1].trim());
						}
					}
				}
			} catch (Exception e) {
				return 0;
			}
			return 0;
		}
	    
	private int lastResult(byte[] buf,int contentLength){ // 进行结果处理
	    int headLength = 0;
	    int packetLength = 0;
	    for(int i = 0; i < buf.length; i++) {
		if(i + 3 < buf.length && buf[i] == '\r' && buf[i + 1] == '\n' 
			&& buf[i + 2] == '\r' && buf[i + 3] == '\n') {
		    i += 4;
		    byte[] temp = new byte[contentLength];
		    headLength = i;
		    packetLength = headLength + temp.length;
//		    DebugOutput.println("packet:"+packetLength+";header:"+headLength+";body:"+temp.length);
		    if(buf.length < packetLength){ // 读到了不足1个包的内容，应该继续攒包
//			DebugOutput.println("has receive:"+ buf.length+"< needed "+packetLength+"需要继续攒包");
			return 0;
		    }		    	    
		    try{
			System.arraycopy(buf,headLength, temp, 0, contentLength);
			MeyouPerf.MeyouPacket meyouPacket = MeyouPerf.MeyouPacket.parseFrom(temp);
		    	responseProcessor.process(meyouPacket);	
		    	return packetLength;
		    } catch (Exception e) {
		    	System.out.println("接收结果处理错误");
		    	return -1; // 出错
		    }	    
		}
	    }   
	    return packetLength;
	}
	
	private void receiveThreadQuit(){ // 接收线程退出
	    receiveByteBuffer.clear();
	    isReceiveThreadQuit = true;
	}
    }
    

    
    public void sendMessage(MessageEntityPerf messageEntity){
		byte[] body = messageEntity.getHttpEntity();
		byte[] header = getHttpHeader(body.length,messageEntity.getUristr()).getBytes();
        ByteBuffer bb = ByteBuffer.allocate(header.length + body.length);
        bb.put(header);
        bb.put(body);
        bb.flip();
//	        DebugOutput.println(new String(header));
//	        DebugOutput.println("will send:"+bb.limit()+";Connected:"+ socketChannel.isConnected());
		int send = 0; // 发送出去的字节数
		int reminder = bb.limit(); // 尚未发送的字节数
		int tryNum = 1; // 尝试发送的次数
		
		while(reminder > 0){
		    try { // 根据发送出去的字节数来进行判断
			send = socketChannel.write(bb);
		    } catch (IOException e) { // 发送失败，说明网络中断
//			DebugOutput.println("send failed，quit");
		    System.out.println("网络异常，数据发送失败，发送线程退出");
//			WesyncExceptionPerf wesyncException = new WesyncExceptionPerf("网络异常，数据发送失败，重启网络",
//				    WesyncExceptionPerf.NetworkInterruption);
//			MeyouNoticePerf notice = new MeyouNoticePerf(NoticeTypePerf.exception,wesyncException,wesyncException.getMessage());
//			NotifyCenterPerf.clientNotifyChannel.add(notice);
			return; // 因为已经启动了新的发送线程，该线程可以停止掉			
		    } 	 
		    if(send <= 0){ // 数据完全没有发送出去
			if(tryNum >= 25){ // 等待总时长超过30s(按照短消息的收发时间折半计算得来)
			    try {
				Thread.sleep(100*tryNum);
			    } catch (InterruptedException e) {
				return;
			    }
			}else{
			    try {
				Thread.sleep(20*tryNum);
			    } catch (InterruptedException e) {
				return;
			   }
			}
		    } 
		    reminder = reminder - send; // 未发送出去的字节数		
		    bb.position(bb.limit()-reminder); // 重新设定postison		   
//		    DebugOutput.println("has send:"+send+";reminder:"+reminder+";try time:"+ tryNum);
		    tryNum++;
		}
		
		bb = null;
    }
    
	public String getHttpHeader(int length, String uri) {
	    StringBuilder sb = new StringBuilder();
	    sb.append("POST /wesync?"+uri+" HTTP/1.1\r\n");
	    sb.append("authorization: "+ managerCenter.getUid()+"\r\n");
//	    if(reconnected){
////		sb.append("authorization: Basic "+managerCenter.getUsernamePassword()+"=\r\n");
//	    	sb.append("authorization: "+ managerCenter.getUid()+"\r\n");
//	    	reconnected = false; // 表示已经连接，以后在连接就是
//	    }
	    sb.append("User-Agent: Jakarta Commons-HttpClient/3.1\r\n");
	    sb.append("Host: "+managerCenter.getServerIp() + ":" + managerCenter.getServerPort()+"\r\n");
	    sb.append("Content-Length: "+ length +	"\r\n\r\n");
	    return sb.toString();
	}
}
