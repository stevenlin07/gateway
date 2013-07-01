package com.weibo.wesync.notify;

import static org.junit.Assert.*;

import java.util.List;

import org.apache.commons.httpclient.HttpClient;
import org.junit.Before;
import org.junit.Test;

import com.google.protobuf.ByteString;
import com.weibo.wesync.Command;
import com.weibo.wesync.WeSyncURI;
import com.weibo.wesync.client.HTTPClientTest;
import com.weibo.wesync.data.FolderID;
import com.weibo.wesync.data.MetaMessageType;
import com.weibo.wesync.data.WeSyncMessage.FolderCreateResp;
import com.weibo.wesync.data.WeSyncMessage.FolderSyncReq;
import com.weibo.wesync.data.WeSyncMessage.FolderSyncResp;
import com.weibo.wesync.data.WeSyncMessage.GetItemUnreadReq;
import com.weibo.wesync.data.WeSyncMessage.GetItemUnreadResp;
import com.weibo.wesync.data.WeSyncMessage.Meta;
import com.weibo.wesync.data.WeSyncMessage.SyncReq;
import com.weibo.wesync.data.WeSyncMessage.SyncResp;
import com.weibo.wesync.data.WeSyncMessage.Unread;
import com.weibo.wesync.notify.protocols.Meyou;

public class ReceiveMessageClientTest {
	LocalStoreMock localstore;
	private String url = "http://123.125.106.28:8093/wesync?";
	
	@Before
	public void setUp() throws Exception {
	}

	@Test
	public void testSendMessage() {
		String me = "1010000007";//NotifyTestModule.touid;
		
		try {
			String rootId = FolderID.onRoot(me);
			WeSyncURI uri = WeSyncTestUtil.getWeSyncUri(Command.FolderSync);
			FolderSyncReq req = FolderSyncReq.newBuilder().setId(rootId).setKey(WeSyncTestUtil.TAG_SYNC_KEY).build();
			Meyou.MeyouPacket packet = WeSyncTestUtil.getMeyouPacket(req);
			
			HttpClient httpClient = HTTPClientTest.getHttpClient();
			MeyouGatewayResult ret = WeSyncTestUtil.post(url, uri, me, packet, httpClient);
			System.out.println(ret.code);
			assertTrue(ret.code == 200);
			
			FolderSyncResp syncResp = FolderSyncResp.parseFrom(ret.packet.getContent().toByteArray());
			List<String> childfolders = syncResp.getChildIdList();
			assertTrue(childfolders.size() > 0);
			String recvfolderId = syncResp.getChildId(0);
			List folders = new java.util.ArrayList();
			folders.add(recvfolderId);
			
			uri = WeSyncTestUtil.getWeSyncUri(Command.GetItemUnread);
			GetItemUnreadReq.Builder reqBuilder = GetItemUnreadReq.newBuilder();
			reqBuilder.addFolderId(recvfolderId);
			packet = WeSyncTestUtil.getMeyouPacket(reqBuilder.build());
			ret = WeSyncTestUtil.post(url, uri, me, packet, httpClient);
			
			GetItemUnreadResp unread = GetItemUnreadResp.parseFrom(ret.packet.getContent().toByteArray());
			String syncKey = null;
					
			for(int i = 0; i < unread.getUnreadList().size(); i++) {
				Unread u = unread.getUnreadList().get(i);
				
				if(i == 0) {
					// init sync key but don't sync anything really
					uri = WeSyncTestUtil.getWeSyncUri(Command.Sync);
						
					SyncReq syncreq = SyncReq.newBuilder()
							.setFolderId(u.getFolderId())
							.setKey(WeSyncTestUtil.TAG_SYNC_KEY)
							.build();
					packet = WeSyncTestUtil.getMeyouPacket(syncreq);
					ret = WeSyncTestUtil.post(url, uri, me, packet, httpClient);
					
					SyncResp syncResp0 = SyncResp.parseFrom(ret.packet.getContent().toByteArray());
					syncKey = syncResp0.getNextKey();
					assertNotNull(syncKey);
				}
				
				// use the synckey to sync the real content
				uri = WeSyncTestUtil.getWeSyncUri(Command.Sync);
				SyncReq syncreq = SyncReq.newBuilder()
						.setFolderId(u.getFolderId())
						.setKey(syncKey)
						.build();
				packet = WeSyncTestUtil.getMeyouPacket(syncreq);
				ret = WeSyncTestUtil.post(url, uri, me, packet, httpClient);
				
				SyncResp syncResp0 = SyncResp.parseFrom(ret.packet.getContent().toByteArray());
				syncKey = syncResp0.getNextKey();
				List<Meta> msgsFromServer = syncResp0.getServerChangesList();
				
				for(Meta meta : msgsFromServer) {
					System.out.println(meta.getContent().toStringUtf8());
				}
			}
		} 
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	private String getNoticeId(String fromuid, String touid) {
		return fromuid + touid + System.currentTimeMillis();
	}
}
