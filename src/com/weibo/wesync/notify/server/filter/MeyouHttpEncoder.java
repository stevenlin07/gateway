package com.weibo.wesync.notify.server.filter;

import java.text.SimpleDateFormat;

import org.apache.mina.common.ByteBuffer;
import org.apache.mina.common.IoSession;
import org.apache.mina.filter.codec.ProtocolEncoderAdapter;
import org.apache.mina.filter.codec.ProtocolEncoderOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.weibo.wesync.notify.protocols.Meyou;

/**
 * 
 * @auth jichao1@staff.sina.com.cn 
 */
public class MeyouHttpEncoder extends ProtocolEncoderAdapter {
	private static final Logger log = LoggerFactory.getLogger(MeyouHttpEncoder.class);
	private SimpleDateFormat dateformat = new SimpleDateFormat("EE, dd MMM yyyy HH:mm:ss z");
	/**
	 * Creates the instance of the encoder.
	 */
	protected MeyouHttpEncoder() { }
	
	/**
	 * Encodes the protobuf {@link Message} provided into the wire format.
	 * 
	 * @param session The session (not used).
	 * @param message The protobuf {@link Message}.
	 * @param out The encoder output used to write buffer into.
	 */
	public void encode(IoSession session, Object message, ProtocolEncoderOutput out) {
		try {
			Meyou.MeyouPacket resp = (Meyou.MeyouPacket) message;
			int size = resp.getSerializedSize();
			String header = resp.getCode() == 200 ?
				getHttpRespHeader(size) :
				getHttpRespHeader(resp.getCode(), resp.getText(), size);
			ByteBuffer buffer = ByteBuffer.allocate(header.length() + size + 4);
			buffer.put(header.getBytes());
			buffer.put(getHttpClientBodyHead());
			
			if(size > 0) {
				byte[] body = resp.toByteArray();
				buffer.put(body);
			}
	
	//		buffer.put(getHttpClientTail());
			buffer.flip();
			out.write(buffer);
		}catch (Exception e){
			log.error("Fail to encode output message", e);
		}
	}

	private String getHttpRespHeader(int size) {
		return getHttpRespHeader(200, "OK", size);
	}
	
	private String getHttpRespHeader(int code, String text, int size) {
		StringBuilder sb = new StringBuilder();
		sb.append("HTTP/1.1 " + code + " " + text + "\r\n");
		sb.append("Content-Length: ").append(size);
		sb.append("\r\n");
		//sb.append("\r\nDate: " + dateformat.format(new Date()) + "\r\n");   
		//sb.append("Expires: Thu, 01-Jan-1970 00:00:00 GMT\r\n");
		sb.append("Server: Meyou(0.1)");
		return sb.toString();
	}
	
	// for standard http resp body
	private byte[] getHttpClientBodyHead() {
		byte[] tail = new byte[4];
		tail[0] = '\r';
		tail[1] = '\n';
		tail[2] = '\r';
		tail[3] = '\n';
		return tail;
	}
	
	// for chunk body
	private byte[] getHttpChunkBodyHead() {
		byte[] tail = new byte[7];
		tail[0] = 0x0d;
		tail[1] = 0x0a;
		tail[2] = 0x0a;
		tail[3] = 0x36;
		tail[4] = 0x30;
		tail[5] = 0x0d;
		tail[6] = 0x0a;
		return tail;
	}
	
	// for chunk body
	private byte[] getHttpChunkedBodyTail() {
		byte[] tail = new byte[7];
		tail[0] = 0x0d;
		tail[1] = 0x0a;
		tail[2] = 0x30;
		tail[3] = 0x0d;
		tail[4] = 0x0a;
		tail[5] = 0x0d;
		tail[6] = 0x0a;
		return tail;
	}
}