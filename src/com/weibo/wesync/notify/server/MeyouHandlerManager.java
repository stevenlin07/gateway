package com.weibo.wesync.notify.server;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.commons.codec.binary.Base64;
import org.apache.log4j.Logger;
import org.apache.mina.common.IoSession;

import cn.sina.api.commons.util.JsonWrapper;

import com.google.protobuf.ByteString;
import com.weibo.contact.wesync.ContactProcessor;
import com.weibo.wesync.WeSyncService;
import com.weibo.wesync.gateway.business.WesyncListenerBusiness;
import com.weibo.wesync.notify.protocols.Meyou;
import com.weibo.wesync.notify.protocols.MeyouSort;
import com.weibo.wesync.notify.server.handler.CloseHandler;
import com.weibo.wesync.notify.server.handler.HeartbeatHandler;
import com.weibo.wesync.notify.server.handler.INotifyHandler;
import com.weibo.wesync.notify.server.handler.TestHandler;
import com.weibo.wesync.notify.service.NotifySession;
import com.weibo.wesync.notify.service.NotifySessionManager;
import com.weibo.wesync.notify.service.app.AppProxy;
import com.weibo.wesync.notify.utils.Util;
/**
 * 
 * @auth jichao1@staff.sina.com.cn 
 */
public class MeyouHandlerManager {
	private static Logger log = Logger.getLogger(MeyouHandlerManager.class);
	private ConcurrentHashMap<String, INotifyHandler> handlers = new ConcurrentHashMap<String, INotifyHandler>();
	private ThreadPoolExecutor executor4ShortTask = new ThreadPoolExecutor(Runtime.getRuntime().availableProcessors(), 
		Runtime.getRuntime().availableProcessors(), 60, TimeUnit.SECONDS, new java.util.concurrent.LinkedBlockingQueue<Runnable>());
	private ThreadPoolExecutor executor4LongTask = new ThreadPoolExecutor(Runtime.getRuntime().availableProcessors(), 
			Runtime.getRuntime().availableProcessors(), 60, TimeUnit.SECONDS, new java.util.concurrent.LinkedBlockingQueue<Runnable>());
	private WeSyncService weSync = null;
	private HeartbeatHandler heartbeatHandler;
	private CloseHandler closeHandler;
	
	private static final int SHORT_TASK_LIMIT = 1024;
	private static final int OK_CODE = 200;
	private static final int ERROR_CODE = 402;
	private static final String OK_TEXT = "OK";
	AppProxy appProxy;
	WesyncListenerBusiness wesyncListenerBusiness;
	ContactProcessor contactProcessor;
	NotifySessionManager sessionManager;
	
	public MeyouHandlerManager(WeSyncService weSync, WesyncListenerBusiness wesyncListenerBusiness) {
		handlers.put("test", new TestHandler());
		heartbeatHandler = new HeartbeatHandler();
		closeHandler = new CloseHandler();
	    this.weSync = weSync;
	    appProxy = 	AppProxy.getInstance();
	    contactProcessor = new ContactProcessor();    

	    this.wesyncListenerBusiness = wesyncListenerBusiness;
	}
	
	public void process(Meyou.MeyouPacket packet, IoSession session) {
		if (packet.hasContent() && packet.getContent().size() > SHORT_TASK_LIMIT){
			executor4LongTask.execute(new GatewayTask(packet, session));
			return;
		}
		executor4ShortTask.execute(new GatewayTask(packet, session));
	}
	
	public NotifySessionManager getSessionManager() {
		return sessionManager;
	}

	public void setSessionManager(NotifySessionManager sessionManager) {
		this.sessionManager = sessionManager;
	}
	
	/** 为用户准备数据*/
	public void prepareUser(String fromuid) {
		if (this.weSync == null) {
			return;
		}
		if(!weSync.getDataService().prepareForNewUser(fromuid)) {
			log.error("[prepareUser][fail]");
		} else {
			log.info("[prepareUser][succ]");
		}
	}
	
	public List<Meyou.MeyouPacket> processMeyoutPacket(Meyou.MeyouPacket meyouPacket, byte[] uriData,
			IoSession session) 
	{
		List<Meyou.MeyouPacket> packets = new ArrayList<Meyou.MeyouPacket>();
		String fromuid = (String) session.getAttribute("uid");
		String text = (String) session.getAttribute("meyouToken");
		int code = OK_CODE;
		try {
			byte[] result = null; 
			initSession(fromuid, session);
			if(meyouPacket.getSort() == MeyouSort.handshake) {
				log.debug(fromuid + " send handshake req");
				String meyouToken = (String) session.getAttribute("meyouToken");
				if(!weSync.getDataService().prepareForNewUser(fromuid)) {
					code = ERROR_CODE;
				}
			}
			else if(meyouPacket.getSort() == MeyouSort.heartbeat) {
				log.debug("get heartbeat from user " + fromuid);
				heartbeatHandler.process(meyouPacket, session);
			}
			else if(meyouPacket.getSort() == MeyouSort.close) {
				log.debug(fromuid + " send close req");
				closeHandler.process(meyouPacket, session);
			}
			else if(meyouPacket.getSort() == MeyouSort.wesync) {
				log.debug(fromuid + " send wesync req");
				long t1 = System.currentTimeMillis();
				result = weSync.request(fromuid, uriData, meyouPacket.getContent().toByteArray());
				long t2 = System.currentTimeMillis();
				if(t2 - t1 > 50) {
					log.warn("wesync is slow using " + (t2 - t1));
				}
			}
			else if(meyouPacket.getSort() == MeyouSort.contact) {
				String req = meyouPacket.getContent().toStringUtf8();
				log.debug("meyou gateway get contact req: " + req);
				String resp = contactProcessor.process(fromuid, req);
				log.debug("meyou gateway get appservice resp: " + resp);
				result = resp.getBytes();
			}else if(meyouPacket.getSort() == MeyouSort.appproxy) {
				String remoteIp =  ((InetSocketAddress) session.getRemoteAddress()).getHostName();
				String req = meyouPacket.getContent().toStringUtf8();
				log.debug("meyou gateway get appProxy req: " + req);
				byte[] attach = meyouPacket.getAttache().toByteArray(); // 附加文件之用
				JsonWrapper json = new JsonWrapper(req);
				String resp = appProxy.process(fromuid,remoteIp,json, attach);
				log.debug("meyou gateway get appProxy resp: " + resp);
				result = resp.getBytes();
			}else if(meyouPacket.getSort() == MeyouSort.business) {
				String remoteIp =  ((InetSocketAddress) session.getRemoteAddress()).getHostName();
				String req = meyouPacket.getContent().toStringUtf8();
				log.debug("meyou gateway get Plugin req: " + req);
				byte[] attach = meyouPacket.getAttache().toByteArray(); // 附加文件之用
				JsonWrapper json = new JsonWrapper(req);
				String resp = this.wesyncListenerBusiness.process(fromuid,remoteIp,json, attach);
				log.debug("meyou gateway get Plugin resp: " + resp);
				result = resp.getBytes();
			}
			else {
				log.warn(fromuid + " send not supported meyougateway sort: " + meyouPacket.getSort());
			}
			
			Meyou.MeyouPacket.Builder resultBuilder = Meyou.MeyouPacket.newBuilder()
					.setSort(meyouPacket.getSort())
					.setCallbackId(meyouPacket.getCallbackId())
					.setText(text)
					.setUid(fromuid);
			
			if(result != null) {
				resultBuilder.setCode(OK_CODE);
				resultBuilder.setContent(ByteString.copyFrom(result));
			} else {
				resultBuilder.setCode(ERROR_CODE);
			}
					
			packets.add(resultBuilder.build());
			return packets;
		}
		catch(Exception e) {
			Meyou.MeyouPacket resultPacket = Meyou.MeyouPacket.newBuilder()
				.setCode(ERROR_CODE)
				.setText(text)
				.setSort(meyouPacket.getSort())
			    .setCallbackId(meyouPacket.getCallbackId())
			    .build();
			
			log.error("wesync failed caused by " + e.getMessage(), e);
			packets.add(resultPacket);
			return packets;
		}
	}
	
	/**
	 * If having local session, refresh its timestamp; if not having, create new one and
	 * set to remote.
	 * @param fromuid
	 * @param ioSession
	 */
	private void initSession(String fromuid, IoSession ioSession) {
		// keepalive
		NotifySession sess = sessionManager.getLocalSession(fromuid);
		
		if(sess != null) {
			sess.resetTimestamp();
			// 为什么每次要重新set iosession？这是因为在切换网络的情况下，新socket会重建，而NotifySession仍然保留了旧连接的iosessoin
			// 因此需要每次都更新iosession来处理这种情况
			sess.ioSession = ioSession;
		}
		else {
			sess = new NotifySession();
			sess.setUid(fromuid);
			sess.setPushServerIp(Util.getLocalInternalIp());
			sess.ioSession = ioSession;
			sessionManager.setSession(sess);
			
			log.debug(fromuid + "session is created ,localip=" + sess.getPushServerIp());
		}
	}
	
	private class GatewayTask implements Runnable {
		private IoSession session;
		private Meyou.MeyouPacket packet;
		
		public GatewayTask(Meyou.MeyouPacket packet, IoSession session) {
			this.packet = packet;
			this.session = session;
		}
		//
		public void run() {
			try {
				String query = packet.getUri().toStringUtf8();
				byte[] uriData = null;
				if(query != null){
					uriData = Base64.decodeBase64(query.getBytes());
				}
				
			    List<Meyou.MeyouPacket> packets = processMeyoutPacket(packet, uriData, session);
			    
			    if(packets != null && !packets.isEmpty()) {
			    	for(Meyou.MeyouPacket packet : packets) {
			    		session.write(packet);
			    	}
			    }
			} 
			catch (Exception e) {
				log.error(e.getMessage(), e);
				Meyou.MeyouPacket meyouPacket = Meyou.MeyouPacket.newBuilder()
					.setCode(401)
					.setText("meyou packet parse error caused by " + e.getMessage())
					.setSort(MeyouSort.notice)
					.build();
				session.write(meyouPacket);
			}
		}
	}
}
