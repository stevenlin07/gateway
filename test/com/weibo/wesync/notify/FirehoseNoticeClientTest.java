package com.weibo.wesync.notify;

import static org.junit.Assert.*;

import java.util.List;

import org.apache.commons.httpclient.HttpClient;
import org.junit.Test;

import cn.sina.api.commons.util.JsonWrapper;

import com.weibo.wesync.Command;
import com.weibo.wesync.WeSyncURI;
import com.weibo.wesync.client.HTTPClientTest;
import com.weibo.wesync.data.FolderID;
import com.weibo.wesync.data.WeSyncMessage.FolderSyncReq;
import com.weibo.wesync.data.WeSyncMessage.FolderSyncResp;
import com.weibo.wesync.data.WeSyncMessage.GetItemUnreadReq;
import com.weibo.wesync.data.WeSyncMessage.GetItemUnreadResp;
import com.weibo.wesync.data.WeSyncMessage.Meta;
import com.weibo.wesync.data.WeSyncMessage.SyncReq;
import com.weibo.wesync.data.WeSyncMessage.SyncResp;
import com.weibo.wesync.data.WeSyncMessage.Unread;
import com.weibo.wesync.notify.protocols.Meyou;
import com.weibo.wesync.notify.protocols.MeyouSort;

public class FirehoseNoticeClientTest {
	private String url = "http://123.125.106.28:8087/wesync?";
	LocalStoreMock localstore = new LocalStoreMock();
	boolean needHandshake = true;
	
	@Test
	public void test() {
		String me = "1101000002";
		
		try {
			// handshake
			if(needHandshake) {
				Meyou.MeyouPacket packet = Meyou.MeyouPacket.newBuilder()
						.setCallbackId("12345abcde")
						.setSort(MeyouSort.handshake)
						.build();
				
				HttpClient httpClient = HTTPClientTest.getHttpClient();
				MeyouGatewayResult ret = WeSyncTestUtil.post(url, null, me, packet, httpClient);
				assertTrue(ret.code == 200);
				System.out.println(me + " handeshake finished");
			}
			
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
			
			for(String recvfolderId : childfolders) {
				System.out.println("sync folder: " + recvfolderId);
				uri = WeSyncTestUtil.getWeSyncUri(Command.GetItemUnread);
				GetItemUnreadReq unreadreq = GetItemUnreadReq.newBuilder()
					.addFolderId(recvfolderId)
					.build();
				packet = WeSyncTestUtil.getMeyouPacket(unreadreq);
				ret = WeSyncTestUtil.post(url, uri, me, packet, httpClient);
				
				GetItemUnreadResp unread = GetItemUnreadResp.parseFrom(ret.packet.getContent().toByteArray());
				String syncKey = null;
				List<String> changingfolders = new java.util.ArrayList<String>();
				
				for(int i = 0; i < unread.getUnreadList().size(); i++) {
					Unread u = unread.getUnreadList().get(i);
					
					if(!changingfolders.contains(u.getFolderId())) {
						changingfolders.add(u.getFolderId());
					}
					else {
						System.out.println("dup folder : " + u.getFolderId());
					}
				}
				
				String oldSynckey = null;
				
				for(String folder : changingfolders) {	
					if((syncKey = localstore.getSynckey(me,folder)) == null) {
						// init sync key but don't sync anything really
						uri = WeSyncTestUtil.getWeSyncUri(Command.Sync);
						
						SyncReq syncreq = SyncReq.newBuilder()
								.setFolderId(folder)
								.setKey(WeSyncTestUtil.TAG_SYNC_KEY)
								.build();
						packet = WeSyncTestUtil.getMeyouPacket(syncreq);
						ret = WeSyncTestUtil.post(url, uri, me, packet, httpClient);
						
						SyncResp syncResp0 = SyncResp.parseFrom(ret.packet.getContent().toByteArray());
						syncKey = syncResp0.getNextKey();
						localstore.setSynckey(me, folder, syncKey);
						assertNotNull(syncKey);
					}
					
					while(oldSynckey == null || !oldSynckey.equals(syncKey)) {
						// use the synckey to sync the real content
						uri = WeSyncTestUtil.getWeSyncUri(Command.Sync);
						SyncReq syncreq = SyncReq.newBuilder()
								.setFolderId(folder)
								.setKey(syncKey)
								.build();
						packet = WeSyncTestUtil.getMeyouPacket(syncreq);
						ret = WeSyncTestUtil.post(url, uri, me, packet, httpClient);
						
						SyncResp syncResp0 = SyncResp.parseFrom(ret.packet.getContent().toByteArray());
						oldSynckey = syncKey;
						syncKey = syncResp0.getNextKey();
						System.out.println("get new synckey=" + syncKey);
						localstore.setSynckey(me, folder, syncKey);
						List<Meta> msgsFromServer = syncResp0.getServerChangesList();
						
						for(Meta meta : msgsFromServer) {
							System.out.println(meta.getContent().toStringUtf8());
						}
					}
				}
			}
			
		} 
		catch (Exception e) {
			e.printStackTrace();
		}
	}
}
