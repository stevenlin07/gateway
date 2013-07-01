package com.weibo.wesync.notify;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.ByteArrayRequestEntity;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.RequestEntity;

import com.weibo.wesync.Command;
import com.weibo.wesync.WeSyncService;
import com.weibo.wesync.WeSyncURI;
import com.weibo.wesync.data.WeSyncMessage.FolderCreateReq;
import com.weibo.wesync.data.WeSyncMessage.Meta;
import com.weibo.wesync.data.WeSyncMessage.SyncReq;
import com.weibo.wesync.data.WeSyncMessage.SyncResp;
import com.weibo.wesync.notify.protocols.Meyou;
import com.weibo.wesync.notify.protocols.MeyouSort;
import com.google.protobuf.GeneratedMessage;
import org.apache.commons.codec.binary.Base64;

public class WeSyncTestUtil {
	public static String TAG_SYNC_KEY = "0";
	private static String username = "muzixinly@163.com";
	private static String password = "332672759";
	
	private static WeSyncURI getWeSyncURI(){
		WeSyncURI uri = new WeSyncURI();
		uri.protocolVersion = 10;
		uri.guid = "serverguid";
		uri.deviceType = "server";		
		return uri;
	}
	
	public static GeneratedMessage getGreateFolderReq(String username, String userChatWith) throws IOException {
		FolderCreateReq req = FolderCreateReq.newBuilder()
				.setUserChatWith(userChatWith)
				.build();
		
		return req;
	}
	
	public static GeneratedMessage getSyncKeyReq(String folderId) throws IOException {
		SyncReq reqBuilder = SyncReq.newBuilder()
				.setFolderId(folderId)
				// if you don't really want to sync all content from server, set to false
				.setIsFullSync(false)
				// the syncKey tell server what the returned synckey should be
				.setKey(TAG_SYNC_KEY)
				.build();
		return reqBuilder;
	}
	
	public static WeSyncURI getWeSyncUri(com.weibo.wesync.Command cmd) {
		WeSyncURI uri = getWeSyncURI();
		uri.command = cmd.toByte();
		return uri;
	}
	
	public static String initSyncKey(String fromuid, String folderId, WeSyncService weSync) throws IOException {
		WeSyncURI uri = WeSyncTestUtil.getWeSyncURI();
		uri.command = Command.Sync.toByte();
		
		SyncReq.Builder reqBuilder = SyncReq.newBuilder()
				.setFolderId(folderId)
				// if you don't really want to sync all content from server, set to false
				.setIsFullSync(false)
				// the syncKey tell server what the returned synckey should be
				.setKey(TAG_SYNC_KEY);
		byte[] respData = weSync.request(fromuid, WeSyncURI.toBytes(uri), reqBuilder.build().toByteArray());
		SyncResp syncResp = SyncResp.parseFrom(respData);
		String nextSyncKey = syncResp.getNextKey();
		return nextSyncKey;
	}
	
	public static String syncMeta(String fromuid, String folderId, String synckey, Meta metatext, 
		WeSyncService weSync) throws IOException 
	{
		WeSyncURI uri = WeSyncTestUtil.getWeSyncURI();
		uri.command = Command.Sync.toByte();
		
		SyncReq.Builder reqBuilder = SyncReq.newBuilder()
				.setFolderId(folderId)
				.addClientChanges(metatext)
				.setKey(synckey);
		
		byte[] respData = weSync.request(fromuid, WeSyncURI.toBytes(uri), reqBuilder.build().toByteArray());
		SyncResp syncResp = SyncResp.parseFrom(respData);
		String nextSyncKey = syncResp.getNextKey();
		return nextSyncKey;
	}
	
	public static SyncResp sync(String fromuid, String folderId, String synckey, WeSyncService weSync) throws IOException 
	{
		WeSyncURI uri = WeSyncTestUtil.getWeSyncURI();
		uri.command = Command.Sync.toByte();
			
		SyncReq.Builder reqBuilder = SyncReq.newBuilder()
				.setFolderId(folderId)
				.setKey(synckey);
			
		byte[] respData = weSync.request(fromuid, WeSyncURI.toBytes(uri), reqBuilder.build().toByteArray());
		SyncResp syncResp = SyncResp.parseFrom(respData);
		return syncResp;
	}
	
	public static Meyou.MeyouPacket getMeyouPacket(com.google.protobuf.GeneratedMessage msg) {
		Meyou.MeyouPacket meyouPacket = Meyou.MeyouPacket.newBuilder()
			.setCallbackId("12345abcde")
			.setSort(MeyouSort.wesync)
			.setContent(msg.toByteString())
			.build();
		return meyouPacket;
	}
	
	public static MeyouGatewayResult postNoRelease(String url, WeSyncURI uri, String uid, Meyou.MeyouPacket packet, 
		HttpClient httpClient) throws HttpException, IOException 
	{
		int code = 503;
		Map<String,String> headers = new HashMap<String, String>();
		
		// so far no auth so need to set uid in header 
		headers.put("uid", uid);
		Map<String, String> params = new HashMap<String, String>();
		RequestEntity entity = new ByteArrayRequestEntity(packet.toByteArray());
		String url0 = uri == null ? url : url + getWeSyncUriString(uri);
		PostMethod post = new PostMethod(url0);
		post.setRequestEntity(entity);
		MeyouGatewayResult result = new MeyouGatewayResult();
		
		if (headers != null && !headers.isEmpty()) {
			for (Map.Entry<String, String> entry : headers.entrySet()) {
				post.setRequestHeader(entry.getKey(),entry.getValue());
			}
		}
		
		code = httpClient.executeMethod(post);
		result.code = code;
		
		if(code == 200) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			result.packet = extractResponseBody(post);
		}
		
		return result;
	}
	
	public static MeyouGatewayResult post(String url, WeSyncURI uri, String uid, Meyou.MeyouPacket packet, 
			HttpClient httpClient) throws Exception 
		{
			int code = 503;
			Map<String,String> headers = new HashMap<String, String>();
			password = RsaTool.encryptPassword(password);
			// so far no auth so need to set uid in header 
			headers.put("uid", uid);
			Map<String, String> params = new HashMap<String, String>();
			RequestEntity entity = new ByteArrayRequestEntity(packet.toByteArray());
			String url0 = uri == null ? url : url + getWeSyncUriString(uri);
			PostMethod post = new PostMethod(url0);
			
			String usrpwd = Base64.encodeBase64String((username + ":" + password).getBytes());
//    		post.addRequestHeader("authorization", "Basic " + usrpwd);
    		post.addRequestHeader("authorization", "Basic " + usrpwd.trim());
			
			post.setRequestEntity(entity);
			MeyouGatewayResult result = new MeyouGatewayResult();
			
			try {
				if (headers != null && !headers.isEmpty()) {
		  			for (Map.Entry<String, String> entry : headers.entrySet()) {
		  				post.setRequestHeader(entry.getKey(),entry.getValue());
					}
		  		}
				
				code = httpClient.executeMethod(post);
				result.code = code;
				
				if(code == 200) {
					result.packet = extractResponseBody(post);
				}
			}
			finally {
//				post.releaseConnection();
			}
			
			return result;
		}
	
	public static String getWeSyncUriString(WeSyncURI uri) throws IOException {
		byte[] uridata = WeSyncURI.toBytes(uri);
		String uristr = com.sun.org.apache.xml.internal.security.utils.Base64.encode(uridata);
		return uristr;
	}
	
	private static Meyou.MeyouPacket extractResponseBody(HttpMethod httpMethod) throws IOException {
		int contentLength  = getResponseContentLength(httpMethod);
		InputStream instream = httpMethod.getResponseBodyAsStream();
		ByteArrayOutputStream outstream = contentLength < 0 ?
				new ByteArrayOutputStream() :
				new ByteArrayOutputStream(contentLength);
		int len = 0;
		int start = 0;

		if(contentLength > 0) {
			byte[] buffer = new byte[contentLength];
			
			while(start < contentLength && (len = instream.read(buffer, start, contentLength)) > 0) {
				outstream.write(buffer, 0, len);
				start += len;
			}
			Meyou.MeyouPacket packet0 = Meyou.MeyouPacket.parseFrom(outstream.toByteArray());
			return packet0;
		}
		
		return null;
	}
	
	public static int getResponseContentLength(HttpMethod httpMethod) {
    	Header[] headers = httpMethod.getResponseHeaders("Content-Length");
    	if(headers.length == 0) {
    		return -1;
    	}
    	if(headers.length > 1) {
    		System.out.println("Multiple content-length headers detected" + ",url:" + httpMethod.getPath());
    	}

    	for(int i = headers.length - 1; i >= 0; i--) {
    		Header header = headers[i];
    		try {
				return Integer.parseInt(header.getValue());
			}
    		catch (NumberFormatException e) {
				System.out.println("Invalid content-length value:" + e.getMessage() + ",url:" + httpMethod.getPath());
			}
    	}

        return -1;
    }
}
