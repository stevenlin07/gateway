package com.weibo.wesync.notify.server.handler;

import org.apache.log4j.Logger;
import org.apache.mina.common.IoSession;
import com.weibo.wesync.notify.protocols.Meyou;
import com.weibo.wesync.notify.service.NotifySessionManager;

public class CloseHandler implements INotifyHandler {
	private static Logger log = Logger.getLogger(CloseHandler.class);
	NotifySessionManager sessionManager;
	
	@Override
	public void process(Meyou.MeyouPacket meyouPacket, IoSession session) {
		String fromuid = (String) session.getAttribute("uid");
		sessionManager.removeSession(fromuid);
		log.debug(fromuid + " receive close req, session has been removed.");
	}
}
