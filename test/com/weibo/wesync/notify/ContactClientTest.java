package com.weibo.wesync.notify;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import org.apache.commons.httpclient.HttpClient;
import org.codehaus.jackson.map.JsonNode;
import org.junit.Before;
import org.junit.Test;

import cn.sina.api.commons.util.JsonBuilder;
import cn.sina.api.commons.util.JsonWrapper;

import com.google.protobuf.ByteString;
import com.weibo.contact.model.Contact;
import com.weibo.contact.wesync.ContactProcessor.ContactCommand;
import com.weibo.wesync.client.HTTPClientTest;
import com.weibo.wesync.notify.protocols.Meyou;
import com.weibo.wesync.notify.protocols.MeyouSort;

public class ContactClientTest {
	private String url = "http://123.125.106.28:8888/wesync?";
	String fromuid = "1263058720";
//	String fromuid = "2420223547";
	String touid = "system";
	boolean needHandshake = true;
	
	Random random = new Random();
	
	@Before
	public void setUp() throws Exception {
	}

	@Test
	public void testSendMessage() {
		try {
			Meyou.MeyouPacket meyouPacket = Meyou.MeyouPacket.newBuilder()
					.setCallbackId("12345abcdeer")
					.setSort(MeyouSort.contact)
					.setContent(ByteString.copyFromUtf8(uploadContact()))
					.build();
			
			HttpClient httpClient = HTTPClientTest.getHttpClient();
			MeyouGatewayResult ret = WeSyncTestUtil.post(url, null, fromuid, meyouPacket, httpClient);
			System.out.println(ret.code);
			// how to know send ok?
		} 
		catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public String uploadContact() {
		List<Contact> tels = new ArrayList<Contact>();
		tels.add(new Contact(134234l, 15901114871l, "08615901114871", "remark", new Date()));
		tels.add(new Contact(134234l, 13693198391l, "13693198391", "remark", new Date()));
		
		JsonBuilder builder = new JsonBuilder();
		builder.append("type", ContactCommand.UPLOAD.get());
		builder.append("data", new JsonBuilder(toJson(tels), true));
		
		return builder.flip().toString();
	}
	
	//[{\"tel\":\"+8613809876399\",\"name\":\"微米\"},{\"tel\":\"08615909876300\",\"name\":\"微米2\"}]
	private String toJson(List<Contact> rets) {
		JsonBuilder builder = new JsonBuilder();

		StringBuilder sb = new StringBuilder("[");
		if (rets != null && !rets.isEmpty()) {
			for (Contact c : rets) {
				JsonBuilder tmpBuilder = new JsonBuilder();
				tmpBuilder.append("tel", c.tel);
				tmpBuilder.append("name", c.remark);
				sb.append(tmpBuilder.flip().toString()).append(",");
			}
			sb.setLength(sb.length() - 1);
		}
		sb.append("]");
		builder.append("tels", new JsonBuilder(sb.toString(), true));

		return builder.flip().toString();
	}

	public static void main(String[] args) throws Exception {

		String msg = new ContactClientTest().uploadContact();
		System.out.println(msg);
		
//		String msg2 = "{\"type\":\"upload\",\"data\":{\"tels\":[\"08615901114871\",\"18034534535\",\"8613634534535\"]}}";
		
		JsonWrapper json = new JsonWrapper(msg);
		JsonWrapper node = json.getNode("data");

		List<String> tels = new ArrayList<String>();
		JsonNode jsonNode = node.getJsonNode("tels");
		if (jsonNode != null) {
			Iterator<JsonNode> it = jsonNode.getElements();
			while (it.hasNext()) {
				tels.add(it.next().getTextValue());
			}
		}
		for (String tel : tels) {
			System.out.println(tel);
		}
	}
}
