package com.weibo.wesync.notify;

import java.io.IOException;
import java.util.Random;

import org.apache.commons.httpclient.HttpClient;
import org.junit.Before;
import org.junit.Test;

import com.google.protobuf.ByteString;
import com.weibo.wesync.Command;
import com.weibo.wesync.WeSyncURI;
import com.weibo.wesync.client.HTTPClientTest;
import com.weibo.wesync.data.FolderID;
import com.weibo.wesync.data.MetaMessageType;
import com.weibo.wesync.data.WeSyncMessage.FolderCreateReq;
import com.weibo.wesync.data.WeSyncMessage.Meta;
import com.weibo.wesync.data.WeSyncMessage.SyncReq;
import com.weibo.wesync.notify.protocols.Meyou;
import com.weibo.wesync.notify.protocols.MeyouSort;

public class GroupMgrClientTest {
	private String url = "http://123.125.106.28:9999/wesync?";
//	String fromuid = "2420223547";
	 String fromuid = "2420223547";
	String touid = "groupmgr";
	boolean needHandshake = true;

	Random random = new Random();

	@Before
	public void setUp() throws Exception {
	}

	@Test
	public void testSendMessage() {
		try {
			createFolder();

			sendMeta();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void createFolder() throws Exception {
		WeSyncURI uri = getWeSyncURI();
		uri.command = Command.FolderCreate.toByte();

		FolderCreateReq folderCreateReq = FolderCreateReq.newBuilder().setUserChatWith(touid).build();

		Meyou.MeyouPacket meyouPacket = Meyou.MeyouPacket.newBuilder().setCallbackId("12345abcdeer12").setSort(MeyouSort.wesync)
				.setContent(ByteString.copyFrom(folderCreateReq.toByteArray())).build();

		HttpClient httpClient = HTTPClientTest.getHttpClient();
		MeyouGatewayResult ret = WeSyncTestUtil.post(url, uri, fromuid, meyouPacket, httpClient);

		System.out.println("code=" + ret.code);
	}

	public void sendMeta() throws Exception {
		WeSyncURI uri = getWeSyncURI();
		uri.command = Command.Sync.toByte();

		Meyou.MeyouPacket meyouPacket = Meyou.MeyouPacket.newBuilder().setCallbackId("12345abcdeer13").setSort(MeyouSort.wesync)
				.setContent(ByteString.copyFrom(createSyncReq().toByteArray())).build();

		HttpClient httpClient = HTTPClientTest.getHttpClient();
		MeyouGatewayResult ret = WeSyncTestUtil.post(url, uri, fromuid, meyouPacket, httpClient);

		System.out.println("code=" + ret.code);
	}

	public SyncReq createSyncReq() throws IOException {
		SyncReq.Builder reqBuilder = SyncReq.newBuilder().setFolderId(FolderID.onConversation(fromuid, touid)).setIsFullSync(false)
				.setIsForward(false).setKey("0");

		// add client changes here
		reqBuilder.addClientChanges(createMeta());
		return reqBuilder.build();
	}

	public Meta createMeta() {
		Meta.Builder msgBuilder = Meta.newBuilder().setFrom(fromuid).setTo(touid)
				.setType(ByteString.copyFrom(new byte[] { MetaMessageType.text.toByte() }))
				.setContent(ByteString.copyFromUtf8(createGroupInfo()));

		// wesync协议会重新消息id
		String msgId = String.valueOf(System.currentTimeMillis());
		msgBuilder.setId(msgId);
		return msgBuilder.build();
	}

	public void createSyncReq(Meta meta) {
		WeSyncURI uri = getWeSyncURI();
		uri.command = Command.Sync.toByte();

		SyncReq.Builder reqBuilder = SyncReq.newBuilder().setFolderId(FolderID.onConversation(fromuid, touid)).setIsFullSync(false)
				.setIsForward(false).setKey("0");

		// add client changes here
		reqBuilder.addClientChanges(meta);
	}

	private WeSyncURI getWeSyncURI() {
		WeSyncURI uri = new WeSyncURI();
		uri.protocolVersion = 10;
		uri.guid = "1234567890abcdefg";
		uri.deviceType = "iphone";
		return uri;
	}

	public String createGroupInfo() {
		String from = fromuid;
		String to = "groupmgr";

		String content = "{\"type\":\"creategroup\", \"creator\": \""+from+"\", \"members\": [\"453454353\",\"5463534534\"]}";

		return content;
	}

	public static void main(String[] args) throws Exception {

		String info = new GroupMgrClientTest().createGroupInfo();
		System.out.println(info);

	}
}
