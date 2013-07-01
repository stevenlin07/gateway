package com.weibo.wesync.client;

import org.apache.commons.httpclient.methods.ByteArrayRequestEntity;
import org.apache.commons.httpclient.methods.RequestEntity;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.junit.Test;

import com.sun.org.apache.xml.internal.security.utils.Base64;
import com.weibo.wesync.Command;
import com.weibo.wesync.WeSyncURI;
import com.weibo.wesync.data.WeSyncMessage.FolderCreateReq;
import com.weibo.wesync.notify.protocols.Meyou;
import com.weibo.wesync.notify.protocols.MeyouSort;

public class HTTPClientTest extends TestCase {
	private static HttpClient httpClient = null;
	
	public String requestPostUrl0(String url, Map<String,String> headers, Map<String, String> params) {
		HttpClient httpClient = getHttpClient();
		PostMethod post = new PostMethod(url);
		
		if (params != null && !params.isEmpty()) {
  			List<NameValuePair> list = new ArrayList<NameValuePair>(params.size());

  			for (Map.Entry<String, String> entry : params.entrySet()) {
  				if (entry.getKey() != null && !entry.getKey().isEmpty()) {
  					list.add(new NameValuePair(entry.getKey(), entry.getValue()));
  				}
				else {
					try {
						post.setRequestEntity(new StringRequestEntity(entry.getValue(), "text/xml", "utf-8"));
					}
  					catch (UnsupportedEncodingException e) {
						e.printStackTrace();
					}
				}
			}
  			if (!list.isEmpty())
  				post.setRequestBody(list.toArray(new NameValuePair[list.size()]));
  		}

    	if (headers != null && !headers.isEmpty()) {
  			for (Map.Entry<String, String> entry : headers.entrySet()) {
  				post.setRequestHeader(entry.getKey(),entry.getValue());
			}
  		}

    	// set keep alive
    	post.setRequestHeader("Connection", "Keep-Alive");
    	
		String result = null;
		
		for(int i = 0; i < 1; i++) {
			try {
				int code = httpClient.executeMethod(post);
				extractResponseBody(post);
				
//				Thread.sleep(1000);
				
				if(code != 200) {
//				log.warn("GroupApi request url failed caused [code: " + code +
//					"] by " + result);
					return null;
				}
			}
			catch(Exception e) {
				e.printStackTrace();
			}
			finally{
				post.releaseConnection();
			}
			System.out.println(result);
		}
		return result;
	}
	
	private static void extractResponseBody(HttpMethod httpMethod) throws IOException {
		InputStream instream = httpMethod.getResponseBodyAsStream();
		
		while(true) {
			if(instream != null) {
				int contentLength = getResponseContentLength(httpMethod);
				ByteArrayOutputStream outstream = contentLength < 0 ?
						new ByteArrayOutputStream() :
							new ByteArrayOutputStream(contentLength);
						
						byte[] buffer = new byte[1024];
						int len = 0;
						while((len = instream.read(buffer)) > 0){
							outstream.write(buffer, 0, len);
						}
			
				String content = new String(outstream.toByteArray(), "utf-8");
//			outstream.close();
				System.out.println(content);
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}
	
	private static int getResponseContentLength(HttpMethod httpMethod) {
    	Header[] headers = httpMethod.getResponseHeaders("Content-Length");
    	if(headers.length == 0) {
    		return -1;
    	}
    	if(headers.length > 1) {
//    		log.info("Multiple content-length headers detected" + ",url:" + httpMethod.getPath());
    	}

    	for(int i = headers.length - 1; i >= 0; i--) {
    		Header header = headers[i];
    		try {
				return Integer.parseInt(header.getValue());
			}
    		catch (NumberFormatException e) {
//				log.error("Invalid content-length value:" + e.getMessage() + ",url:" + httpMethod.getPath());
			}
    	}

        return -1;
    }
	
	public static HttpClient getHttpClient(){
		if(httpClient == null) {
			synchronized(HTTPClientTest.class) {
				if(httpClient == null){
					MultiThreadedHttpConnectionManager httpConnManager = new MultiThreadedHttpConnectionManager();
					httpConnManager.setMaxConnectionsPerHost(100);
					httpClient = new HttpClient(httpConnManager);
				}
			}
		}
		
		return httpClient;
	}

	private WeSyncURI getWeSyncURI(){
		WeSyncURI uri = new WeSyncURI();
		uri.protocolVersion = 10;
		uri.guid = "1234567890abcdefg";
		uri.deviceType = "iphone";		
		return uri;
	}
	
	@Test
	public void testClient() {
		HTTPClientTest hct = new HTTPClientTest();
		WeSyncURI uri = getWeSyncURI();
		uri.command = Command.FolderCreate.toByte();
		String uristr = null;
		try {
			byte[] uridata = WeSyncURI.toBytes(uri);
			uristr = Base64.encode(uridata);
			byte[] uridata0 = Base64.decode(uristr);
			WeSyncURI uri0 = WeSyncURI.fromBytes(uridata0);
			assertTrue(uri0.command == uri.command);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		//TODO removed? uri.args[1] = syncKey;
		
		FolderCreateReq req = FolderCreateReq.newBuilder()
			.setUserChatWith("test_folder")
			.build();
		Meyou.MeyouPacket meyouPacket = Meyou.MeyouPacket.newBuilder()
			.setCallbackId("12345abcde")
			.setSort(MeyouSort.wesync)
			.setContent(req.toByteString()).build();
		
		String url = "http://123.125.106.28:8087/wesync?" + uristr;
		System.out.println(url);
		Map<String,String> headers = new HashMap<String, String>();
		Map<String, String> params = new HashMap<String, String>();
		hct.requestPostUrl0(url, headers, params);
		RequestEntity entity = new ByteArrayRequestEntity(meyouPacket.toByteArray());
		HttpClient httpClient = getHttpClient();
		PostMethod post = new PostMethod(url);
		post.setRequestEntity(entity);
		
		try {
			if (headers != null && !headers.isEmpty()) {
	  			for (Map.Entry<String, String> entry : headers.entrySet()) {
	  				post.setRequestHeader(entry.getKey(),entry.getValue());
				}
	  		}
			int code = httpClient.executeMethod(post);
		}
		catch(Exception e) {
			e.printStackTrace();
		}
		finally {
			post.releaseConnection();
		}
	}
	
	public static void main(String[] args) {
		HTTPClientTest hct = new HTTPClientTest();
		hct.testClient();
	}
}
