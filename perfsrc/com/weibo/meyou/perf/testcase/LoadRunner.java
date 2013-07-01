package com.weibo.meyou.perf.testcase;

import java.net.InetSocketAddress;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.mina.common.ConnectFuture;
import org.apache.mina.common.DefaultIoFilterChainBuilder;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.filter.executor.ExecutorFilter;
import org.apache.mina.transport.socket.nio.SocketConnector;

import com.weibo.meyou.perf.client.codec.MeyouHttpCodecFactory;

public class LoadRunner {
	public static void main(String[] args) throws InterruptedException{
		int threadInterval = 200;
		
		int threadCount = Integer.parseInt(args[0]);
		if(threadCount == 0) threadCount = 1;
		
		int startUid = Integer.parseInt(args[1]);
		if (startUid == 0) startUid = 1000000001;
		// 创建IO连接器
		SocketConnector connector = new SocketConnector();
		
        // Change the worker timeout to 1 second to make the I/O thread quit soon
        // when there's no connection to manage.
//        connector.setWorkerTimeout(1);
        
		// 创建接收数据的过滤器
		DefaultIoFilterChainBuilder chain = connector.getFilterChain();
		// 设定过滤器
		chain.addLast("binary", new ProtocolCodecFilter(new MeyouHttpCodecFactory()));
		chain.addLast("threadPool",
				new ExecutorFilter(Executors.newCachedThreadPool()));
		
		// 连接到服务器
		ConnectFuture cf = null;
		MinaClientHandler handler = new MinaClientHandler(startUid);
		
		String ipAddress = "10.210.230.32";
		int successConn = 0;

		for (int i = 1; i <= threadCount; i++) {
			// 连接服务器
			cf = connector.connect(new InetSocketAddress(ipAddress, 7070), handler);
			// 等待异步执行的结果返回
			cf.join();
			if(!cf.isConnected()){
				// 如果连接失败，则增加一次新的尝试 
				threadCount = threadCount + 1;
			}else{
				successConn++;
			}
			
			if(successConn % 2 == 1){
				ipAddress = "10.210.230.33";
			}else{
				ipAddress = "10.210.230.32";
			}
			Thread.sleep(threadInterval);
		}

		
		int processorCount = Runtime.getRuntime().availableProcessors();
		ScheduledExecutorService senders = Executors.newScheduledThreadPool(processorCount);
		for(int i = startUid; i < startUid + threadCount; i++){
			senders.scheduleWithFixedDelay(new MsgSender(i), 0, 10, TimeUnit.SECONDS);
		}
	}
}
