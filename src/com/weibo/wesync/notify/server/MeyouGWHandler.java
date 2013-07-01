package com.weibo.wesync.notify.server;

import java.net.InetSocketAddress;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;
import org.apache.mina.common.IdleStatus;
import org.apache.mina.common.IoHandlerAdapter;
import org.apache.mina.common.IoSession;

import com.weibo.wesync.authFilter.AuthFilterServlet;
import com.weibo.wesync.authFilter.AuthResult;
import com.weibo.wesync.notify.protocols.Meyou;
import com.weibo.wesync.notify.protocols.Meyou.Attribute;

/**
 * 
 * @auth jichao1@staff.sina.com.cn 
 */
public class MeyouGWHandler extends IoHandlerAdapter {
	private static Logger log = Logger.getLogger(MeyouGWHandler.class);
	private final static int CLIENT_IDLE_TIME = 5 * 60 * 1000; 
    public static Map<Long, String> executingTasks = new ConcurrentHashMap<Long, String>();
    private MeyouHandlerManager handlerManager;
    private AuthFilterServlet authFilterServlet;
    
    public MeyouGWHandler(MeyouHandlerManager handlerManager) {
    	this.handlerManager = handlerManager;
    	authFilterServlet = new AuthFilterServlet();
    	
    	URL url = MeyouGWHandler.class.getClassLoader().getResource("private_pkcs8.pem");
    	authFilterServlet.init(url.getFile());
    }
    
	@Override
	public void sessionClosed(IoSession session) {
		if(log.isDebugEnabled())
			log.debug("io session closed:"+session);
		
		try {
			String u = (String) session.getAttribute("uid");
			if (u != null){
				this.handlerManager.sessionManager.removeSession(u);
			}
		} catch (Exception e) {
			log.error("Error: io session close:" + session, e);
		}finally{
			session.close();
		}
	}

	public void exceptionCaught(IoSession session, Throwable cause) {
		String u = (String) session.getAttribute("uid");
		if (u != null){
			this.handlerManager.sessionManager.removeSession(u);
		}
		log.error("IOSession caught exception:" + session ,cause);
		cause.printStackTrace();
		session.close();
	}

	public void messageReceived(IoSession session, Object message)
			throws Exception 
    {
		String uid = (String) session.getAttribute("uid");
		int code = 401;
		Meyou.MeyouPacket meyouPacket = (Meyou.MeyouPacket) message;

		if(uid == null) { // 说明需要进行认证
			List<Attribute> headers = meyouPacket.getHttpReq().getHeadersList();
			String authorization = null;
			
			for(Attribute header : headers) {
				if("authorization".equalsIgnoreCase(header.getName().toStringUtf8())) {
					authorization = header.getValue().toStringUtf8();
					break;
				}
			}
		
			AuthResult authResult = authFilterServlet.authBasic(authorization, ((InetSocketAddress) session.getRemoteAddress()).getHostName());
			uid = authResult.uid;
			code = authResult.code;
			session.setAttribute("uid", uid);
			session.setAttribute("meyouToken",authResult.meyouToken);
			handlerManager.prepareUser(uid);
		}
		
		if(uid != null) {
			handlerManager.process(meyouPacket, session);
		}
		else {
			Meyou.MeyouPacket resp = Meyou.MeyouPacket.newBuilder()
				.setSort(meyouPacket.getSort())
				.setCallbackId(meyouPacket.getCallbackId())
				.setCode(code)
				.setText("auth failed")
				.build();
			session.write(resp);
		}
	}

	public void messageSend(IoSession session, Object message) throws Exception {
		super.messageSent(session, message);
	}

	@Override
	public void sessionOpened(IoSession session) throws Exception {
		try {
			session.setAttribute("uid", null);
		} 
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void sessionIdle(IoSession session, IdleStatus status) {
		log.info("io session idle:"+session);
	}
}