package com.weibo.wesync.notify.server.filter;

import org.apache.mina.filter.codec.ProtocolCodecFactory;
import org.apache.mina.filter.codec.ProtocolDecoder;
import org.apache.mina.filter.codec.ProtocolEncoder;
/**
 * 
 * @auth jichao1@staff.sina.com.cn 
 */
public class MeyouHttpCodecFactory implements ProtocolCodecFactory  {
	private ProtocolEncoder encoder;
    private ProtocolDecoder decoder;
	
    public MeyouHttpCodecFactory() {
    	decoder = new MeyouHttpDecoder();
    	encoder = new MeyouHttpEncoder();
    }
    
	@Override
	public ProtocolDecoder getDecoder() throws Exception {
		return decoder;
	}

	@Override
	public ProtocolEncoder getEncoder() throws Exception {
		// TODO Auto-generated method stub
		return encoder;
	}
}
