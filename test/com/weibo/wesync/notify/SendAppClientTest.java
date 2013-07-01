package com.weibo.wesync.notify;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.apache.commons.httpclient.HttpClient;
import org.junit.Before;
import org.junit.Test;

import cn.sina.api.commons.util.JsonBuilder;
import cn.sina.api.commons.util.JsonWrapper;

import com.google.protobuf.ByteString;
import com.weibo.wesync.Command;
import com.weibo.wesync.WeSyncURI;
import com.weibo.wesync.client.HTTPClientTest;
import com.weibo.wesync.data.FolderID;
import com.weibo.wesync.data.MetaMessageType;
import com.weibo.wesync.data.WeSyncMessage.FolderCreateResp;
import com.weibo.wesync.data.WeSyncMessage.Meta;
import com.weibo.wesync.data.WeSyncMessage.SyncReq;
import com.weibo.wesync.data.WeSyncMessage.SyncResp;
import com.weibo.wesync.notify.protocols.Meyou;
import com.weibo.wesync.notify.protocols.MeyouSort;

public class SendAppClientTest {
	LocalStoreMock localstore;
	private String url = "http://123.125.106.28:8082/wesync?";
	String fromuid = "2565640713";
	String touid = "system";
	boolean needHandshake = true;
	
	@Before
	public void setUp() throws Exception {
		localstore = new LocalStoreMock();
	}

	@Test
	public void testSendMessage() {
		try {
			// handshake
			if(needHandshake) {
				Meyou.MeyouPacket packet = Meyou.MeyouPacket.newBuilder()
						.setCallbackId("12345abcde")
						.setSort(MeyouSort.handshake)
						.build();
				
				HttpClient httpClient = HTTPClientTest.getHttpClient();
				MeyouGatewayResult ret = WeSyncTestUtil.post(url, null, fromuid, packet, httpClient);
				assertTrue(ret.code == 200);
				String jsonret = new String(ret.packet.getContent().toByteArray(), "utf-8");
				JsonWrapper json = new JsonWrapper(jsonret);
				System.out.println("handeshake resp code:" + json.getInt("ret"));
				System.out.println(fromuid + " handeshake finished");
			}
			
			String propId = FolderID.onConversation(fromuid, touid);
			String synckey = null;
			
			if((synckey = localstore.getSynckey(fromuid, propId)) == null) {
				WeSyncURI uri = WeSyncTestUtil.getWeSyncUri(Command.Sync);
				com.google.protobuf.GeneratedMessage req = WeSyncTestUtil.getSyncKeyReq(propId);
				Meyou.MeyouPacket packet = WeSyncTestUtil.getMeyouPacket(req);
				
				HttpClient httpClient = HTTPClientTest.getHttpClient();
				MeyouGatewayResult ret = WeSyncTestUtil.post(url, uri, fromuid, packet, httpClient);
				System.out.println(ret.code);
				assertTrue(ret.code == 200);
				
				SyncResp syncResp = SyncResp.parseFrom(ret.packet.getContent().toByteArray());
				synckey = syncResp.getNextKey();
				System.out.println(synckey);
				assertNotNull(synckey);
				
				System.out.println("init synckey finished");
				localstore.setSynckey(fromuid, propId, synckey);
			}
			
			JsonBuilder json = getBuddies();
			
			Meta metatext = Meta.newBuilder()
					.setFrom(fromuid)
					.setTo(touid)
					.setType( ByteString.copyFrom( new byte[]{MetaMessageType.operation.toByte()} ))
					.setContent( ByteString.copyFromUtf8(json.flip().toString()))
					.setId(String.valueOf(System.currentTimeMillis()))
					.build();
			
			WeSyncURI uri = WeSyncTestUtil.getWeSyncUri(Command.Sync);
			SyncReq req= SyncReq.newBuilder()
					.setFolderId(propId)
					.addClientChanges(metatext)
					.setKey(synckey)
					.build();
			Meyou.MeyouPacket packet = WeSyncTestUtil.getMeyouPacket(req);
			
			HttpClient httpClient = HTTPClientTest.getHttpClient();
			MeyouGatewayResult ret = WeSyncTestUtil.post(url, uri, fromuid, packet, httpClient);
			System.out.println(ret.code);
			// how to know send ok?
		} 
		catch (Exception e) {
			e.printStackTrace();
		}
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
		return json;
	}
	
	private JsonBuilder addBuddy() {
		JsonBuilder json = new JsonBuilder();
		json.append("appname", "addbuddy");
		json.append("fromuid", "2565640713");
		JsonBuilder param = new JsonBuilder();
		param.append("uid", "1793692835");		
		param.append("screen_name", "my密友");	
		param.append("question", "中文邀请");	
		param.append("answer", "1");		
		param.append("invite_description", "Hi this is me");		
		json.append("parameter", param.flip());
		return json;
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
