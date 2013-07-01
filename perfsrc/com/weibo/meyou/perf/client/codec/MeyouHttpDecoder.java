package com.weibo.meyou.perf.client.codec;


import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.apache.mina.common.ByteBuffer;
import org.apache.mina.common.IoSession;
import org.apache.mina.filter.codec.CumulativeProtocolDecoder;
import org.apache.mina.filter.codec.ProtocolDecoderOutput;

import com.google.protobuf.ByteString;
import com.weibo.meyou.perf.sdk.protocol.MeyouPerf;


public class MeyouHttpDecoder extends CumulativeProtocolDecoder  {
	private static Logger log = Logger.getLogger(MeyouHttpDecoder.class);
	
	@Override
	protected boolean doDecode(IoSession session, ByteBuffer in, ProtocolDecoderOutput out) throws Exception 
	{
		int pos1 = in.position();
		int pos2 = 0;
		try {
			String uri = null;
			Map<String, String> headers = new HashMap<String, String>();
			ByteArrayOutputStream outstream = new ByteArrayOutputStream();
			
			byte previous0 = 0, previous1 = 0, previous2 = 0;
			
			while(in.hasRemaining()) {
				byte current = in.get();
				outstream.write(current);
				
				if (outstream.size() > 4096){      // 4K max Header length
					outstream.reset();
					log.error("Too big header is not permitted and kill the client: "+session.getRemoteAddress().toString());
					session.close();
					return true;
				}
				
				if(previous2 == '\r' && previous1 == '\n' && previous0 == '\r' && current == '\n') {
					String lengthStr = headers.get("Content-Length");
					if (lengthStr == null){
						outstream.reset();
						log.error(Thread.currentThread().getName()+" Illegal content: "+lengthStr);
						session.close();
						return true;
					}
					
					int contentLength = Integer.valueOf(lengthStr);
			        
					if (contentLength > 10000000 ){     //10M max Content length
						outstream.reset();
						log.error("Large http content is not permitted and kill the client: "+session.getRemoteAddress().toString());
						session.close();
						return true;
					}

					if(contentLength > in.remaining()) {
			        	in.position(pos1);
			        	return false;
			        }
			        
			        byte[] reqbody = new byte[contentLength];
			        in.get(reqbody);
			        
			        MeyouPerf.MeyouPacket meyouPacket = MeyouPerf.MeyouPacket.parseFrom(reqbody);
			        MeyouPerf.MeyouPacket.Builder packetBuilder = meyouPacket.toBuilder();
			        
			        if(uri != null) {
			        	packetBuilder.setUri(ByteString.copyFromUtf8(uri));
			        }
			        
			        MeyouPerf.HttpReq.Builder reqBuilder = MeyouPerf.HttpReq.newBuilder();
			        
			        for(Map.Entry<String, String> entry : headers.entrySet()) {
			        	MeyouPerf.Attribute att = MeyouPerf.Attribute.newBuilder()
			        		.setName(ByteString.copyFromUtf8(entry.getKey()))
			        		.setValue(ByteString.copyFromUtf8(entry.getValue()))
			        	    .build();
			        	reqBuilder.addHeaders(att);
			        }
			        
			        packetBuilder.setHttpReq(reqBuilder);
			        out.write(packetBuilder.build());
			        pos2 = in.position();
			        
			        if(pos2 > 0) {
						return true;
					}
				}
				else if(previous0 == '\r' && current == '\n') {
					String line = new String(outstream.toByteArray());
					outstream.reset();
					
					if(line.indexOf(": ") > 0){
						String[] tokens = line.split(": ");
						
			        	try {
		        			headers.put(tokens[0].trim(), tokens[1].trim());
		        		}
		        		catch(Exception e) {
		        			log.error(line + ", " + e.getMessage(), e);
		        		}
						
					}
				}
				
				if(previous1 > 0) previous2 = previous1;
				if(previous0 > 0) previous1 = previous0;
				previous0 = current;
			}   // end of while(in.hasRemaining())
			
			if(pos2 > 0) {
				return true;
			}
		}
		catch(Exception e) {
			log.error(e.getMessage(), e);
			return true;
		}
		
		in.position(pos1);
		return false;
	}
}
