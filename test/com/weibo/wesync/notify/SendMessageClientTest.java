package com.weibo.wesync.notify;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

import com.google.protobuf.ByteString;
import com.weibo.wesync.data.MetaMessageType;
import com.weibo.wesync.data.WeSyncMessage.FolderCreateResp;
import com.weibo.wesync.data.WeSyncMessage.Meta;
import com.weibo.wesync.data.WeSyncMessage.SyncReq;
import com.weibo.wesync.data.WeSyncMessage.SyncResp;
import com.weibo.wesync.notify.protocols.Meyou;
import com.weibo.wesync.notify.protocols.MeyouSort;
import com.weibo.wesync.client.HTTPClientTest;
import com.weibo.wesync.Command;
import com.weibo.wesync.WeSyncURI;
import org.apache.commons.httpclient.HttpClient;
import cn.sina.api.commons.util.JsonWrapper;

public class SendMessageClientTest {
	LocalStoreMock localstore;
	private String url = "http://123.125.106.28:8082/wesync?";
	String fromuid = "2420223547";
	String touid = "2615079113";

	boolean needHandshake = true;
	
	@Before
	public void setUp() throws Exception {
		localstore = new LocalStoreMock();
		//
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
				asssetTrue(json.getInt("ret") == 200);
				System.out.println(fromuid + " handeshake finished");
				
				// don't handeshake touid here because I need to get notice in another program
//				packet = Meyou.MeyouPacket.newBuilder()
//						.setCallbackId("12345abcde")
//						.setSort(MeyouSort.handshake)
//						.build();
//				
//				httpClient = HTTPClientTest.getHttpClient();
//				ret = WeSyncTestUtil.post(url, null, touid, packet, httpClient);
//				assertTrue(ret.code == 200);
//				System.out.println(touid + " handeshake finished");
			}
			
			String folderId = null;
			if((folderId = localstore.getFolderId(fromuid, touid)) == null) {
				WeSyncURI uri = WeSyncTestUtil.getWeSyncUri(com.weibo.wesync.Command.FolderCreate);
				com.google.protobuf.GeneratedMessage req = WeSyncTestUtil.getGreateFolderReq(fromuid, touid);
				Meyou.MeyouPacket packet = WeSyncTestUtil.getMeyouPacket(req);
				
				HttpClient httpClient = HTTPClientTest.getHttpClient();
				MeyouGatewayResult ret = WeSyncTestUtil.post(url, uri, fromuid, packet, httpClient);
				System.out.println(ret.code);
				assertTrue(ret.code == 200);
				
				FolderCreateResp resp = FolderCreateResp.parseFrom(ret.packet.getContent().toByteArray());
				folderId = resp.getFolderId();
				System.out.println("folderId=" + folderId);
				assertNotNull(folderId);
				
				System.out.println("create conversation folder finished");
				localstore.setFolderId(fromuid, touid, folderId);
			}
			
			String synckey = null;
			if((synckey = localstore.getSynckey(fromuid, folderId)) == null) {
				WeSyncURI uri = WeSyncTestUtil.getWeSyncUri(Command.Sync);
				com.google.protobuf.GeneratedMessage req = WeSyncTestUtil.getSyncKeyReq(folderId);
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
				localstore.setSynckey(fromuid, folderId, synckey);
			}
			
			Meta metatext = Meta.newBuilder()
					.setType( ByteString.copyFrom( new byte[]{MetaMessageType.text.toByte()} ))
					.setId(getNoticeId(fromuid, touid))
					.setContent(ByteString.copyFromUtf8("me&you 你好。"))
					.setFrom(fromuid)
					.setTo(touid)
					.setTime( (int) (System.currentTimeMillis() / 1000L) )
					.build();
			
			WeSyncURI uri = WeSyncTestUtil.getWeSyncUri(Command.Sync);
			SyncReq req= SyncReq.newBuilder()
					.setFolderId(folderId)
					.addClientChanges(metatext)
					.setKey(synckey)
					.build();
			Meyou.MeyouPacket packet = WeSyncTestUtil.getMeyouPacket(req);
			
			HttpClient httpClient = HTTPClientTest.getHttpClient();
			MeyouGatewayResult ret = WeSyncTestUtil.post(url, uri, fromuid, packet, httpClient);
			System.out.println(ret.code);
			assertTrue(ret.code == 200);
			
			SyncResp syncResp = SyncResp.parseFrom(ret.packet.getContent().toByteArray());
			synckey = syncResp.getNextKey();
			System.out.println(synckey);
			assertNotNull(synckey);
			// how to know send ok?
		} 
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void asssetTrue(boolean b) {
		// TODO Auto-generated method stub
		
	}

	private String getNoticeId(String fromuid, String touid) {
		return fromuid + touid + System.currentTimeMillis();
	}
}
