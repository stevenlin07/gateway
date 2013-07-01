package com.weibo.meyou.perf.sdk.protocol;

import com.weibo.meyou.perf.sdk.protocol.WeSyncMessagePerf.Meta;


/**
 * File id rules
 * @author Eric Liang
 */
public final class FileIDPerf {
	public static char FILE_SPLIT = '-';
	
	public static String generateId(Meta meta, String suffix){
		return generateId(meta.getFrom(), meta.getTo(), suffix);
	}
	public static String generateId(String username, String userChatWith, String suffix){
		return username+FILE_SPLIT+userChatWith+FILE_SPLIT+suffix;
	}
	
	public static boolean isOwner(String username, String fileId){
		return fileId.startsWith(username+FILE_SPLIT);
	}
	
	public static boolean isReceiver(String username, String fileId){
		String receiver = getReceiver(fileId);
		if( null == receiver ) return false;
		
		return receiver.equals(username);
	}
	
	public static String getReceiver(String fileId){
		int idx1 = fileId.indexOf(FILE_SPLIT);
		if( idx1 < 0 ) return null;
		
		int idx2 = fileId.indexOf(FILE_SPLIT, idx1+1);
		if( idx2 < 0 ) return null;
		
		return fileId.substring(idx1+1, idx2);
	}
}
