package com.weibo.wesync.notify.service.rpc;

import java.nio.ByteBuffer;

import org.apache.commons.pool.BasePoolableObjectFactory;
import org.apache.commons.pool.PoolableObjectFactory;
import org.apache.commons.pool.impl.GenericObjectPool;
import org.apache.thrift.TException;
import org.apache.thrift.transport.TTransportException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.weibo.wesync.data.WeSyncMessage.Notice;
import com.weibo.wesync.data.WeSyncMessage.Unread;

/**
 * 
 * @auth linlin9@staff.sina.com.cn 
 */
public class RpcPoolClient extends RpcClient implements Notify.Iface {
	private static final Logger log = LoggerFactory.getLogger(RpcPoolClient.class);

	private GenericObjectPool pool;

	private GenericObjectPool.Config poolConfig;
	private PoolableObjectFactory factory;

	private String host = null;
	private int port = -1;
	private int timeout = 200;
	private int minConnection = 2;
	private int maxConnection = 10;

	private int retryCount = 3;
	private int readRetryCount = 1; // 对于读取接口的重试次数 

	private boolean alive = false;

//	static AtomicLong errorCount = new AtomicLong(0);
//	static AtomicLong successCount = new AtomicLong(0);
//	static List<Integer> elapseTime = new Vector<Integer>();
//	static Thread printThread;

	public RpcPoolClient() {
		super();
//		synchronized (elapseTime) {
//			if (printThread == null) {
//				printThread = new Thread("client-timestat") {
//					public void run() {
//						TimeStatUtil.printStat(successCount, errorCount, elapseTime);
//					}
//				};
//				printThread.start();
//			}
//		}
	}

	public RpcPoolClient(String host, int port, int timeout) throws TTransportException {
		this();
		setHost(host);
		setPort(port);
		setTimeout(timeout);
	}

	public RpcPoolClient(String host, int port, int timeout, int minConnection, int maxConnection)
			throws TTransportException {
		this(host, port, timeout);
		setMinConnection(minConnection);
		setMaxConnection(maxConnection);
	}

	@Override
	public void send(String touid, ByteBuffer notice) throws TException {
		RpcClient client = null;
		int tryCount = 0;
		
		do {
			try {
				client = (RpcClient) pool.borrowObject();
				client.send(touid, notice);
				pool.returnObject(client);
				break;
			} catch (Exception e) {
				log.warn("[will retry] send notice error " + getServerPort() + e);
				if (client != null) {
					try {
						pool.invalidateObject(client);
					} catch (Exception ie) {
						log.warn("invalidate rpc client error ", ie);
					}
				}
			}
		} while (++tryCount < readRetryCount);
	}
	 
	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public int getTimeout() {
		return timeout;
	}

	public void setTimeout(int timeout) {
		this.timeout = timeout;
	}

	public int getMinConnection() {
		return minConnection;
	}

	public void setMinConnection(int minConnection) {
		this.minConnection = minConnection;
	}

	public int getMaxConnection() {
		return maxConnection;
	}

	public void setMaxConnection(int maxConnection) {
		this.maxConnection = maxConnection;
	}

	public int getRetryCount() {
		return retryCount;
	}

	public void setRetryCount(int retryCount) {
		this.retryCount = retryCount;
	}
	
	public void setReadRetryCount(int readRetryCount) {
		this.readRetryCount = readRetryCount;
	}

	public boolean isConnected() {
		return alive;
	}

	public void connect() {
		try {
			initConnect();
			alive = true;
		} catch (TTransportException e) {
			log.error("RPC pool connect " + toString() + " fail", e);
			alive = false;
		}
	}

	public void close() {
		try {
			this.pool.close();
		} catch (Exception e) {
			log.error("RpcPoolClient close error", e);
		}
	}

	private void initConnect() throws TTransportException {
		poolConfig = new RpcPoolClientConfig();
		poolConfig.minIdle = minConnection;
		poolConfig.maxIdle = maxConnection;
		poolConfig.maxActive = maxConnection;

		factory = new RpcClientPoolFactory(host, port, timeout);

		pool = new GenericObjectPool(factory, poolConfig);
	}

	static class RpcPoolClientConfig extends org.apache.commons.pool.impl.GenericObjectPool.Config {
		public RpcPoolClientConfig() {
			super();
			// must test on borrow
			super.testOnBorrow = false;
			super.testOnReturn = false;
			super.testWhileIdle = true;
			super.whenExhaustedAction = (byte) 1;
			super.maxWait = 500;
			super.lifo = false;
			super.minEvictableIdleTimeMillis = -1;
			super.timeBetweenEvictionRunsMillis = 30000L;
			super.softMinEvictableIdleTimeMillis = 60000L;
			super.numTestsPerEvictionRun = -2;
		}
	}

	static class RpcClientPoolFactory extends BasePoolableObjectFactory {
		private final String host;
		private final int port;
		private final int timeout;

		public static final int DEFAUTL_TIMEOUT = 500;

		public RpcClientPoolFactory(final String host, final int port, final int timeout) {
			super();
			if (port > 0) {
				this.host = host;
				this.port = port;
			} else {
				String[] parts = host.split(":");
				this.host = parts[0];
				this.port = Integer.parseInt(parts[1]);
			}

			this.timeout = (timeout > 0) ? timeout : -1;
		}

		public String getFactoryName() {
			return host + ":" + port;
		}

		@Override
		public String toString() {
			return "RpcClient " + host + ":" + port;
		}

		@Override
		public Object makeObject() throws Exception {
			final RpcClient client;
			if (timeout > 0) {
				client = new RpcClient(this.host, this.port, this.timeout);
			} else {
				client = new RpcClient(this.host, this.port, DEFAUTL_TIMEOUT);
			}
			client.connect();
			return client;
		}

		@Override
		public void destroyObject(final Object obj) throws Exception {
			if (obj instanceof RpcClient) {
				final RpcClient client = (RpcClient) obj;

				try {
					client.close();
					
					log.info("client disconnect success: " + client.getServerPort());
				} catch (Exception e) {
					log.warn("client disconnect error: " + client , e);
				}
			}
		}

		@Override
		public boolean validateObject(final Object obj) {
			if (obj instanceof RpcClient) {
				final RpcClient client = (RpcClient) obj;
				try {
					return client.isConnected();
				} catch (final Exception e) {
					return false;
				}
			} else {
				return false;
			}
		}

		@Override
		public void activateObject(Object obj) throws Exception {
			if (obj instanceof RpcClient) {
				final RpcClient client = (RpcClient) obj;
				if (!client.isConnected()) {
					client.connect();
				}
			}
		}

		@Override
		public void passivateObject(Object obj) throws Exception {
			// do nothing for now
		}
		
	}
	
	public static void main(String[] args){
		RpcPoolClient rpc = new RpcPoolClient();
		rpc.setHost("127.0.0.1");
		rpc.setPort(7911);
		rpc.setMaxConnection(10);
		rpc.setMinConnection(2);
		rpc.connect(); // 初始化连接
		
				
		String receiverRoot = "LINlin";
		int unreadNum = 100;
		
		Unread unread = Unread.newBuilder()
				.setFolderId( receiverRoot )
				.setNum( unreadNum )
				.build();
		
		Notice notice = Notice.newBuilder()
				.addUnread(unread)
				.build();
		
		try {
			byte[] content = notice.toByteArray();
			log.info("contentn length is: "+content.length);
			rpc.send("lixiaohui ", ByteBuffer.wrap(content));
			log.info("send finish");
			rpc.close();
		} catch (TException e) {
			e.printStackTrace();
		}
	}
}