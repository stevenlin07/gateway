package com.weibo.wesync.notify.server.handler;

import org.apache.mina.common.IoSession;
import com.weibo.wesync.notify.protocols.Meyou;

public class HeartbeatHandler implements INotifyHandler {

	@Override
	public void process(Meyou.MeyouPacket meyouPacket, IoSession session) {
//		sess.resetTimestamp();
	}
}
