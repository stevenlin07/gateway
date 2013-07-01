package com.weibo.wesync.notify.server.handler;

import org.apache.mina.common.IoSession;
import com.weibo.wesync.notify.protocols.Meyou;

/**
 * 对于过于频繁的连接返回401 xxx
 * @author wujichao
 *
 */
public class ConnProtectHandler implements INotifyHandler {
	public void process(Meyou.MeyouPacket meyouPacket, IoSession session) {
//		if(System.currentTimeMillis() - sess.getTimestamp() < ) ???
 	}
}
