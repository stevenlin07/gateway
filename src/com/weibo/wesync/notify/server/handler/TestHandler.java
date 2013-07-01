package com.weibo.wesync.notify.server.handler;

import org.apache.mina.common.IoSession;
import org.mortbay.log.Log;

import com.weibo.wesync.notify.protocols.Meyou;
public class TestHandler implements INotifyHandler {

	@Override
	public void process(Meyou.MeyouPacket meyouPacket, IoSession session) {
		synchronized(meyouPacket.getUid().intern()) {
			for(int i = 0; i < 10; i++) {
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				session.write("200 OK test_test_中文测试 [" + i + "]");
				Log.info("test handler worked.");
			}
		}
	}
}
