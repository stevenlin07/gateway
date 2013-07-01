package com.weibo.meyou.perf.sdk.protocol;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;

/**
 * @author Eric Liang
 */
public final class WeSyncURIPerf {
	public byte protocolVersion;
	public byte command;
	public byte clientVersionMajor;
	public byte clientVersionMinor; 
	public String guid;
	public String deviceType;
	
	public static int MAX_ARGUMENT_NUMBER = 8;
	public String[] args = new String[MAX_ARGUMENT_NUMBER];
	
	public boolean equals(final Object obj){
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		
		WeSyncURIPerf uri = (WeSyncURIPerf) obj;
		return uri.protocolVersion == this.protocolVersion
				&& uri.command == this.command
				&& uri.clientVersionMajor == this.clientVersionMajor
				&& uri.clientVersionMinor == this.clientVersionMinor
				&& uri.guid.equals(this.guid)
				&& uri.deviceType.equals(this.deviceType)
				&& Arrays.equals(uri.args, this.args);
	}
	
	public static WeSyncURIPerf fromBytes(byte[] uriData){
		WeSyncURIPerf uri = new WeSyncURIPerf();
		int i = 0;
		
		uri.protocolVersion = uriData[i++];
		uri.command = uriData[i++];
		uri.clientVersionMajor = uriData[i++];
		uri.clientVersionMinor = uriData[i++];
		
		byte guidLen = uriData[i++];
		uri.guid = new String(uriData, i, guidLen);
		i+=guidLen;
		
		byte dtLen = uriData[i++];
		uri.deviceType = new String(uriData, i, dtLen);
		i+=dtLen;
		
		while(i<uriData.length){
			byte tag = uriData[i++];
			byte len = uriData[i++];
			if( i+len > uriData.length ) break;
			
			String value = new String(uriData, i, len);
			i+=len;
			
			//FIXME
			if(tag >= MAX_ARGUMENT_NUMBER) throw new RuntimeException();
			
			uri.args[tag] = value;
		}
		return uri;
	}
	
	public static byte[] toBytes(WeSyncURIPerf uri) throws IOException{
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		bos.write(uri.protocolVersion);
		bos.write(uri.command);
		bos.write(uri.clientVersionMajor);
		bos.write(uri.clientVersionMinor);
		
		int guidLen = uri.guid.getBytes().length;
		//FIXME exception
		if(guidLen > 0xFF) throw new RuntimeException();
		bos.write((byte)guidLen);
		bos.write(uri.guid.getBytes());
		
		int dtLen = uri.deviceType.getBytes().length;
		if(dtLen > 0xFF) throw new RuntimeException();
		bos.write((byte)dtLen);
		bos.write(uri.deviceType.getBytes());
		
		for(int i=0;i<uri.args.length; i++){
			if(null == uri.args[i]) continue;
			
			bos.write( (byte)i );
			int len = uri.args[i].getBytes().length;
			if(len > 0xFF) throw new RuntimeException();
			bos.write( (byte)len);
			bos.write( uri.args[i].getBytes() );
		}

		bos.close();
		return bos.toByteArray();
	}
}
