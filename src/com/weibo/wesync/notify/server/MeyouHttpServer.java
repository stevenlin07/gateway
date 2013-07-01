package com.weibo.wesync.notify.server;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.apache.mina.common.ExecutorThreadModel;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.transport.socket.nio.SocketAcceptor;
import org.apache.mina.transport.socket.nio.SocketAcceptorConfig;
import org.apache.mina.transport.socket.nio.SocketSessionConfig;

import com.weibo.wesync.WeSyncService;
import com.weibo.wesync.notify.server.filter.MeyouHttpCodecFactory;
import com.weibo.wesync.notify.utils.Util;
/**
 * 
 * @auth jichao1@staff.sina.com.cn 
 */
public class MeyouHttpServer {
	private static Logger log = Logger.getLogger("MeyouHttpServer.class");
    private SocketAcceptor commSocketAcceptor;
    private static MeyouHttpServer instance = new MeyouHttpServer();
    public MeyouHandlerManager handlerManager;
    public WeSyncService weSync;
	
	private static final String ADDRESS = "gateway_address";
	private static final String PORT = "gateway_port";
	
    private MeyouHttpServer() {
    }
    
    public static MeyouHttpServer getInstance() {
    	return instance;
    }
 
    public void start() {        
        createListeners();
        startListeners();
    }

	private synchronized void createListeners() {
    	commSocketAcceptor = buildSocketAcceptor();
    	// Customize Executor that will be used by processors to process incoming stanzas
    	ExecutorThreadModel threadModel = ExecutorThreadModel.getInstance("CommServer");
    	int eventThreads = 16;
    	ThreadPoolExecutor eventExecutor = (ThreadPoolExecutor) threadModel.getExecutor();
    	eventExecutor.setCorePoolSize(eventThreads + 1);
    	eventExecutor.setMaximumPoolSize(eventThreads + 1);
    	eventExecutor.setKeepAliveTime(60, TimeUnit.SECONDS);
    	commSocketAcceptor.getDefaultConfig().setThreadModel(threadModel);
    	commSocketAcceptor.getFilterChain().addFirst("binary", new ProtocolCodecFilter(new MeyouHttpCodecFactory()));
    }

	private SocketAcceptor buildSocketAcceptor() {
        SocketAcceptor socketAcceptor;
        // Create SocketAcceptor with correct number of processors
        int ioThreads = Runtime.getRuntime().availableProcessors();
        // Set the executor that processors will use. Note that processors will use another executor
        // for processing events (i.e. incoming traffic)
        Executor ioExecutor = new ThreadPoolExecutor(
            ioThreads + 1, ioThreads + 1, 60, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>() );
        socketAcceptor = new SocketAcceptor(ioThreads, ioExecutor);
        // Set that it will be possible to bind a socket if there is a connection in the timeout state
        SocketAcceptorConfig socketAcceptorConfig = socketAcceptor.getDefaultConfig();
        socketAcceptorConfig.setReuseAddress(true);
        // Set the listen backlog (queue) length. Default is 50.
        socketAcceptorConfig.setBacklog(50);

        // Set default (low level) settings for new socket connections
        SocketSessionConfig socketSessionConfig = socketAcceptorConfig.getSessionConfig();
        //socketSessionConfig.setKeepAlive();
        int receiveBuffer = -1;
        if (receiveBuffer > 0 ) {
            socketSessionConfig.setReceiveBufferSize(receiveBuffer);
        } else
        	socketSessionConfig.setReceiveBufferSize(16 * 1024 * 1024); // 16M
        int sendBuffer = -1;
        if (sendBuffer > 0 ) {
            socketSessionConfig.setSendBufferSize(sendBuffer);
        } else 
        	socketSessionConfig.setSendBufferSize(16 * 1024 * 1024);	// 16M
        
        int linger = -1;
        if (linger > 0 ) {
            socketSessionConfig.setSoLinger(linger);
        }
        socketSessionConfig.setTcpNoDelay(socketSessionConfig.isTcpNoDelay());
        return socketAcceptor;
    }
	
    private synchronized void startListeners() {
        try {
        	//监听anyLocalAddress，内外网均可访问
        	int port = Integer.parseInt(Util.getConfigProp(PORT, "8080"));
            commSocketAcceptor.bind(new InetSocketAddress(port), new MeyouGWHandler(handlerManager));
        }
        catch (Exception e) {
        	log.error(e.getMessage(), e);
        }
    }
    
    /**
	 * 获取本地内网IP，多块网卡时取到一次立即返回，非localhost地址
	 * 
	 * @return
	 */
	public static String getLocalIp() {
		String localIp = null;
		try {
			Enumeration<NetworkInterface> allNetInterfaces = NetworkInterface.getNetworkInterfaces();
			InetAddress inet = null;
			while (allNetInterfaces.hasMoreElements()) {
				NetworkInterface netInterface = (NetworkInterface) allNetInterfaces.nextElement();
				Enumeration<InetAddress> addresses = netInterface.getInetAddresses();
				while (addresses.hasMoreElements()) {
					inet = (InetAddress) addresses.nextElement();
					if (inet.isSiteLocalAddress() && !inet.isLoopbackAddress() // 127.开头的都是lookback地址
							&& inet.getHostAddress().indexOf(":") == -1) {
						localIp = inet.getHostAddress();
						break;
					}
				}
			}
		} catch (SocketException e) {
			// 异常时视为获取失败
		}
		return localIp;
	}
    
    private void stopConnectionManagerListener() {
        if (commSocketAcceptor != null) {
            commSocketAcceptor.unbindAll();
            commSocketAcceptor = null;
        }
    }

    public void stop() {        
        stopConnectionManagerListener();        
    }
    
    public static void main(String args[]) {
    	MeyouHttpServer server = MeyouHttpServer.getInstance();
    	server.start();
		
    	try {
			Thread.currentThread().join();
		} 
    	catch (InterruptedException e) {
    		log.error(e.getMessage(), e);
		}
    }
}

