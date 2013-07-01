package com.weibo.wesync.client;

import java.io.ByteArrayOutputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.net.InetSocketAddress;

import com.weibo.wesync.notify.protocols.Meyou;
import com.weibo.wesync.notify.protocols.MeyouSort;
import com.weibo.wesync.notify.utils.Util;

public class NHttpClient3 {
	private static Selector selector;
	private static ByteBuffer byteBuffer = ByteBuffer.allocate(1024);
	
	private static String getHttpHeader(int length) {
		StringBuilder sb = new StringBuilder();
		sb.append("POST /wesync HTTP/1.1\r\n");
		sb.append("authorization: Basic d3VqaTI4QGdtYWlsLmNvbToxNDE0MTQ=\r\n");
		sb.append("uid: 2565640713\r\n");
		sb.append("User-Agent: Jakarta Commons-HttpClient/3.1\r\n");
		sb.append("Host: 123.125.106.28:8082\r\n");
		sb.append("Content-Length: " + length +	"\r\n\r\n");
		return sb.toString();
	}
	
	private static byte[] getRespBody(byte[] resp, int contentLength) {
		String s = new String(resp);
		
		for(int i = 0; i < resp.length; i++) {
			if(i + 3 < resp.length && resp[i] == '\r' && resp[i + 1] == '\n' && resp[i + 2] == '\r' && resp[i + 3] == '\n') {
				i += 4;
				byte[] temp = new byte[contentLength];
				System.arraycopy(resp, i, temp, 0, contentLength);
				s = new String(temp);
				return temp;
			}
		}
		
		return new byte[0];
	}
	
	private static int getRespContentLength(byte[] resp) {
		int headoff = 0;
		
		for(int i = 0; i < resp.length; i++) {
			if(i + 1 < resp.length && resp[i] == '\r' && resp[i + 1] == '\n') {
				byte[] temp = new byte[i];
				System.arraycopy(resp, headoff, temp, 0, i - headoff);
				String line = new String(temp);
				String[] tokens = line.split(": ");
				i += 2;
				headoff = i;
				
				if("Content-Length".equals(tokens[0])) {
					System.out.println(tokens[1].trim());
					return Integer.valueOf(tokens[1].trim());
				}
			}
		}
		
		return 0;
	}
	
	public static void main(String args[]) throws Exception {
		//开启selector,并建立socket到指定端口的连接
		if(selector == null)
			selector = Selector.open();
		SocketChannel channel = SocketChannel.open();
		channel.configureBlocking(false);
		channel.connect(new InetSocketAddress("123.125.106.28", 8083));
		channel.register(selector, SelectionKey.OP_CONNECT);
		
		new Thread(new Runnable() {
			public void run() {
				while(true) {
					try {
						selector.select();
						Iterator<SelectionKey> keyIterator = selector.selectedKeys().iterator();
						while(keyIterator.hasNext()) {
							SelectionKey key = keyIterator.next();
							
							//连接事件
							if(key.isConnectable()) {
								keyIterator.remove();
								SocketChannel socketChannel = (SocketChannel) key.channel();
								//进行信息读取
								for(int i = 0; i < 10; i++) {
									
									if(socketChannel.isConnectionPending())
										socketChannel.finishConnect();
									
									Meyou.MeyouPacket packet = null;
									
									if(i % 2 == 0) {
										packet = Meyou.MeyouPacket.newBuilder()
												.setCallbackId(String.valueOf(System.currentTimeMillis()))
												.setSort(MeyouSort.notice)
												.build();
									}
									else {
										packet = Meyou.MeyouPacket.newBuilder()
												.setCallbackId(String.valueOf(System.currentTimeMillis()))
												.setSort(MeyouSort.wesync)
												.build();
									}
									
									System.out.println(packet);
									byte[] body = packet.toByteArray();
									byte[] header = getHttpHeader(body.length).getBytes();
									byte[] httpreq = new byte[header.length + body.length];
									System.arraycopy(header, 0, httpreq, 0, header.length);
									System.arraycopy(body, 0, httpreq, header.length, body.length);
									socketChannel.write(ByteBuffer.wrap(httpreq));//向服务器发信息,信息中即服务器上的文件名
									socketChannel.register(selector, SelectionKey.OP_READ);
									Thread.sleep(5);
								}
							}
						}
					}
					catch(Exception e) {
						e.printStackTrace();
					}
				}
			}
		}).start();
		
		new Thread(new Runnable() {
			public void run() {
				try {
					//进行信息读取
					while(true) {
						selector.select();
						Iterator<SelectionKey> keyIterator = selector.selectedKeys().iterator();
						while(keyIterator.hasNext()) {
							SelectionKey key = keyIterator.next();
							
							//读事件
							if(key.isReadable()) {
								keyIterator.remove();
								SocketChannel socketChannel = (SocketChannel) key.channel();
								byteBuffer.clear();
								
								if(!socketChannel.isConnected())
									return;
								
								ByteArrayOutputStream outstream = new ByteArrayOutputStream();
								int readBytes = 0;
								
								while((readBytes = socketChannel.read(byteBuffer)) > 0) {
									outstream.write(byteBuffer.array(), 0, readBytes);
								}
								 
								byte[] buf = outstream.toByteArray();
								
								int contentLength = getRespContentLength(buf);
								
								if(contentLength > 0) {
									byte[] respbody = getRespBody(buf, contentLength);
									Meyou.MeyouPacket meyouPacket = Meyou.MeyouPacket.parseFrom(respbody);
									System.out.println(meyouPacket);
								}
								else {
									System.out.println(new String(buf));
								}
							}
						}
					}
				}
				catch(Exception e) {
					e.printStackTrace();
				}
			}
		}).start();
	}
}
