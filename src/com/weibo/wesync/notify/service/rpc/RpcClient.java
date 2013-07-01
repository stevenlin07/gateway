package com.weibo.wesync.notify.service.rpc;

import java.nio.ByteBuffer;

import org.apache.thrift.TException;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TFramedTransport;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;
import org.apache.thrift.transport.TTransportException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @auth linlin9@staff.sina.com.cn 
 */
public class RpcClient implements Notify.Iface {
	private final static Logger log = LoggerFactory.getLogger(RpcClient.class);
	
	private TTransport trans;
	private TProtocol protocol;
	private Notify.Client client = null;

	private String host = null;
	private int port = -1;
	private int timeout = 200;

	private boolean alive = false;

	public RpcClient() {
	}

	public RpcClient(String host, int port, int timeout) throws TTransportException {
		this();
		setHost(host);
		setPort(port);
		setTimeout(timeout);
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

	public String getServerPort() {
		return getHost() + ":" + getPort();
	}

	public int getTimeout() {
		return timeout;
	}

	public void setTimeout(int timeout) {
		this.timeout = timeout;
	}

	public boolean isConnected() {
		return alive && trans != null && trans.isOpen();
	}

	public void disconnect() {
		trans.close();
		trans = null;
		alive = false;
	}

	public void connect() {
		try {
			initConnect();
			alive = true;
		} catch (TTransportException e) {
			log.error("connect fail, " + getServerPort(), e);
			alive = false;
		}
	}

	private void initConnect() throws TTransportException {
		if (host != null && port > 0 && (trans == null || !trans.isOpen())) {
			trans =  new TFramedTransport(new TSocket(host, port, timeout));
		
			protocol = new TBinaryProtocol(trans, true, true);
			client = new Notify.Client(protocol);

			trans.open();
			log.info("client success connect to " + host + ":" + port + " client:" + client);
		}
	}

	@Override
	public void send(String touid, ByteBuffer notice) throws TException {
		client.send(touid, notice);
	}
	
	@Override
	public void sendSimple(String text) throws TException {
		client.sendSimple(text);
	}
	
	public void close() {
		if (this.trans != null) {
			this.trans.close();
		}  
		
		this.alive = false;
	}
}