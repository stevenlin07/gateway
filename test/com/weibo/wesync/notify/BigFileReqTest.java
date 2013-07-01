package com.weibo.wesync.notify;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.net.InetSocketAddress;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;

import org.apache.commons.httpclient.HttpClient;
import org.junit.Before;
import org.junit.Test;

import cn.sina.api.commons.util.JsonBuilder;

import com.google.protobuf.ByteString;
import com.weibo.wesync.Command;
import com.weibo.wesync.WeSyncURI;
import com.weibo.wesync.client.HTTPClientTest;
import com.weibo.wesync.data.FolderID;
import com.weibo.wesync.data.MetaMessageType;
import com.weibo.wesync.data.WeSyncMessage.Meta;
import com.weibo.wesync.data.WeSyncMessage.SyncReq;
import com.weibo.wesync.data.WeSyncMessage.SyncResp;
import com.weibo.wesync.notify.protocols.Meyou;
import com.weibo.wesync.notify.protocols.MeyouSort;

public class BigFileReqTest {
	private String url = "http://123.126.42.24:8091/wesync?";
	String fromuid = "2565640713";
	String touid = "system";
	boolean needHandshake = true;
	
	@Before
	public void setUp() throws Exception {
	}

	@Test
	public void testSendMessage() {
		try {
			// send big file test
			Meyou.MeyouPacket meyouPacket = Meyou.MeyouPacket.newBuilder()
					.setCallbackId("12345abcde")
					.setSort(MeyouSort.appservice)
					.setContent(ByteString.copyFrom(getBigFile()))
					.build();
	
			byte[] b = meyouPacket.toByteArray();
			String[] sarray = new String[b.length];
			int i = 0;
			for(byte b0 : b) {
				sarray[i++] = Integer.toHexString(b0);
			}
			System.out.println(sarray);
			HttpClient httpClient = HTTPClientTest.getHttpClient();
			MeyouGatewayResult ret = WeSyncTestUtil.post(url, null, fromuid, meyouPacket, httpClient);
			System.out.println(ret.code);
			assertTrue(ret.code == 200);
		} 
		catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private byte[] getBigFile() {
		FileInputStream in = null;
		ByteArrayOutputStream outstream = new ByteArrayOutputStream();
		try {
			in = new FileInputStream("D:/weibo/meyou_gw/lib/commons-fileupload/commons-fileupload/1.2/commons-fileupload-1.2.jar");

			byte[] buf = new byte[1024];
			int readBytes = 0;
			int off = 0;
			
			while((readBytes = in.read(buf)) > 0) {
				outstream.write(buf, 0, readBytes);
				off += readBytes;
			}
		}
		catch(Exception e) {
			e.printStackTrace();
		}
		finally {
			try {
				in.close();
			} 
			catch (Exception e) {
				// ignore
			}
		}
		
		return outstream.toByteArray();
	}
	
	private JsonBuilder getBuddies() {
		JsonBuilder json = new JsonBuilder();
		json.append("appname", "getbuddies");
		json.append("fromuid", "2565640713");
		JsonBuilder param = new JsonBuilder();
		param.append("type", "0");		
		param.append("real_load", "0");
		param.append("trim_status", "1");	
		json.append("parameter", param.flip());
		return json.flip();
	}
	
	private JsonBuilder addBuddy() {
		JsonBuilder json = new JsonBuilder();
		json.append("appname", "addbuddy");
		json.append("fromuid", "1793692835");
		JsonBuilder param = new JsonBuilder();
		param.append("uid", "1846079315");		
		param.append("screen_name", "my密友");	
		param.append("question", "中文邀请");	
		param.append("answer", "1");		
		param.append("invite_description", "Hi this is me");		
		json.append("parameter", param.flip());
		return json.flip();
	}
	
	private JsonBuilder replyBuddy() {
		JsonBuilder json = new JsonBuilder();
		json.append("appname", "replybuddy");
		json.append("fromuid", "2565640713");
		JsonBuilder param = new JsonBuilder();
		param.append("id", "1793692835");		
		param.append("value", "1");	
		json.append("parameter", param.flip());
		return json;
	}
}
