package com.weibo.wesync.notify.service;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ObjectOutputStream;
import org.apache.log4j.Logger;
import org.apache.mina.common.IoSession;

import com.weibo.wesync.notify.protocols.Meyou;
import com.weibo.wesync.notify.utils.ConfigConstants;
import com.weibo.wesync.notify.utils.Util;

import cn.sina.api.commons.util.IpConvert;

/**
 * 
 * @author wujichao
 *
 */
public class NotifySession implements Externalizable {
	private static Logger log = Logger.getLogger(NotifySession.class);
	private String uid;
	private String uuid;
	private long timestamp;
	private int pushServerIp;
	
	public IoSession ioSession;
	
	public NotifySession() {
		timestamp = System.currentTimeMillis();
	}
	
	public String getUid() {
		return uid;
	}

	public void setUid(String uid) {
		this.uid = uid;
	}

	public String getUuid() {
		return uuid;
	}

	public void setUuid(String uuid) {
		this.uuid = uuid;
	}

	public void resetTimestamp() {
		timestamp = System.currentTimeMillis();
	}

	public long getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}
	
	public void pushToClient(Meyou.MeyouPacket packet) {
		long t1 = System.currentTimeMillis();
		ioSession.write(packet);
		long t2 = System.currentTimeMillis();
		if((t2 - t1) > Util.getConfigProp(ConfigConstants.NOTICE_RPC_SLOW, 100)) {
			log.warn("Send notice by RPC to " + getPushServerIp() + " is slow, packet=" + packet);
		}
	}
	
	public String toString() {
		return "[uid:" + uid + ",uuid:" + uuid + ",gwip=" + getPushServerIp() + 
			",lastupdatetime=" + timestamp + "]";
	}

	public String getPushServerIp() {
		return IpConvert.intToIp(pushServerIp);
	}

	public void setPushServerIp(String ip) {
		this.pushServerIp = IpConvert.ipToInt(ip);
	}
	
	public void setExternalBytes(byte[] objectBytes) {
		ObjectInputStream ois = null;
		
		try {
			ois = new ObjectInputStream(new ByteArrayInputStream(objectBytes));
			readExternal(ois);
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
		finally {
			if(ois != null) {
				try {
					ois.close();
				} catch (IOException e) {
					// ignore
				}
			}
		}
	}
	
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		uid = String.valueOf(in.readLong());
		pushServerIp = in.readInt();
		uuid = Util.readSafeUtf8(in);
	}
	
	public byte[] getExternalBytes() {
		ByteArrayOutputStream bout = null;

		try {
			bout = new ByteArrayOutputStream();
			ObjectOutput out = new ObjectOutputStream(bout);
			writeExternal(out);
			out.flush();
			return bout.toByteArray();
		} catch (IOException e) {
			log.error(e.getMessage(), e);
		}
		finally {
			if(bout != null) {
				try {
					bout.close();
				} catch (IOException e) {
					// ignore
				}
			}
		}
		
		return null;
	}
	
	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeLong(Long.parseLong(uid));
		out.writeInt(pushServerIp);
		Util.writeSafeUtf8(out, uuid);
	}
}
