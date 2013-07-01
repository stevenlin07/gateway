package com.weibo.wesync.notify.service.rpc;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.weibo.wesync.WeSyncService;
import com.weibo.wesync.notify.utils.ConfigConstants;
import com.weibo.wesync.notify.utils.Util;

/**
 * 
 * @auth linlin9@staff.sina.com.cn
 */
public class RpcServiceManager {
	private static Logger log = LoggerFactory.getLogger(RpcServiceManager.class);
	private WeSyncService weSync = null;

	private static RpcServiceManager instance = null;
	private Thread rpcServerThread = null;
	private int port = 7911;	//default port

	private ConcurrentHashMap<String, RpcPoolClient> rpcClients = new ConcurrentHashMap<String, RpcPoolClient>();
	private static final int maxConnection = 15;
	private static final int minConnection = 5;
	private static final int readRetryTimes = 3;

	public synchronized static RpcServiceManager getInstance() {
		if (instance == null)
			instance = new RpcServiceManager();
		return instance;
	}
	
	private void init(){
		Properties props = new Properties();
		InputStream input = null;
		
		try {
			input = new FileInputStream("../conf/meyou_conf.properties");	
			props.load(input);
			String strPort = props.getProperty("gateway_rpc_port"); 
			if(strPort != null){
				this.port = Integer.parseInt(strPort);
			}
		} catch (IOException e1) {
			log.error("load gateway_rpc.prop failed", e1);
		}finally{
			if(input!=null){
				try {
					input.close();
				} catch (IOException e) {
				}
			}
		}
	}

	private RpcServiceManager() {
	}

	public void start() {
		if (this.weSync == null)
			throw new IllegalArgumentException("missing wesync param for rpc server");
		
		init();
		
		rpcServerThread = new Thread(new RpcServer(this.port, this.weSync));
		rpcServerThread.start();
		log.info("RPC server is running on port " + this.port);
	}

	public RpcPoolClient getRpcClient(String serverip) {
		RpcPoolClient rpcs = this.rpcClients.get(serverip);

		if (rpcs == null) {
			synchronized(rpcClients) {
				if((rpcs = this.rpcClients.get(serverip)) == null) {
					long t1 = System.currentTimeMillis();
					RpcPoolClient poolClient = new RpcPoolClient();
					poolClient.setHost(serverip);
					poolClient.setPort(this.port);
					poolClient.setMaxConnection(maxConnection);
					poolClient.setMinConnection(minConnection);
					poolClient.setReadRetryCount(readRetryTimes);
					poolClient.connect();
					long t2 = System.currentTimeMillis();
					
					if((t2 - t1) > Util.getConfigProp(ConfigConstants.NOTICE_RPC_SLOW, 100)) {
						log.warn("Create notice RPC connection to " + serverip + " is slow, using=" + (t2 - t1));
					}
					
					this.rpcClients.put(serverip, poolClient);
					rpcs = poolClient;
					log.info("New RPC pool client created on " + serverip + ":"
							+ this.port);
				}
			}
		}
		
		return rpcs;
	}
	
	public void setWeSync(WeSyncService weSync) {
		this.weSync = weSync;
	}
	
}