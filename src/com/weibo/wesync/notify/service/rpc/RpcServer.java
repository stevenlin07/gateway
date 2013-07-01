package com.weibo.wesync.notify.service.rpc;

import java.net.InetSocketAddress;

import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.server.THsHaServer;
import org.apache.thrift.server.THsHaServer.Args;
import org.apache.thrift.server.TServer;
import org.apache.thrift.transport.TFramedTransport;
import org.apache.thrift.transport.TNonblockingServerSocket;
import org.apache.thrift.transport.TTransportException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.weibo.wesync.WeSyncService;
import com.weibo.wesync.notify.service.rpc.handler.NotifyHandler;
import com.weibo.wesync.notify.utils.Util;

/**
 * 
 * @auth linlin9@staff.sina.com.cn 
 */
public class RpcServer implements Runnable{
	private static final Logger log = LoggerFactory.getLogger(RpcServer.class);
	private WeSyncService weSync = null;
	
	private TServer server = null;
	private int port;
	
	public RpcServer(int port, WeSyncService weSync){
		if (port > 1024)
			this.port = port;
		this.weSync = weSync;
	}
	
	@Override
	public void run() {
		TNonblockingServerSocket socket = null;
		Notify.Processor processor = null;
		
		try {
			socket = new TNonblockingServerSocket( 
					new InetSocketAddress(Util.getLocalInternalIp(), port), 0);
			
			processor = new Notify.Processor(new NotifyHandler(this.weSync));
		} catch (TTransportException e) {
			log.error("TTransportException occured in starting RPC server", e);
		}  
		
		THsHaServer.Args arg = new Args(socket);
		arg.protocolFactory(new TBinaryProtocol.Factory(true, true));
		arg.transportFactory(new TFramedTransport.Factory());
		arg.processor(processor);
		arg.workerThreads(Runtime.getRuntime().availableProcessors());
		arg.maxReadBufferBytesPerRequest = 1024 * 1024; // 1MB
		server = new THsHaServer(arg);
		server.serve();
	}
}