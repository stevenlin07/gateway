package com.weibo.meyou.perf.sdk.protocol;


/**
 * @author Eric Liang
 */
public final class SyncKeyPerf {
	public static char SYNC_KEY_SPLIT = '.';
	public static char SYNC_KEY_TAG_FULL = 'F'; //differ with Change tag:+/-
	
	public static String syncKeyOnChild(String folderId, String childId){
		if( null == childId ) return emptySyncKey(folderId, true);
		else return folderId+SYNC_KEY_SPLIT+SYNC_KEY_TAG_FULL+childId;
	} 
	
	public static String emptySyncKey(String folderId, boolean isFullSync ){
		if( isFullSync ){
			return folderId+SYNC_KEY_SPLIT+SYNC_KEY_TAG_FULL;	
		}else{
			return folderId+SYNC_KEY_SPLIT;	
		}
	}
	
	public static boolean isFullSyncKey(String syncKey){
		int idx = syncKey.lastIndexOf(SYNC_KEY_SPLIT);
		if (idx != syncKey.length() - 1
				&& syncKey.substring(idx + 1, idx + 2).indexOf(SYNC_KEY_TAG_FULL) == 0) {
			return true;
		}
		return false;
	}
	
	public static String getChildBySyncKey( String syncKey ){
		String changeStr = getChangeString( syncKey );
		if( null == changeStr || changeStr.length() <= 1 ) return null;
		else return changeStr.substring(1);
	}
	
	public static String getChangeString( String syncKey ){
		if( null == syncKey ) return null;
		
		int idx = syncKey.lastIndexOf(SYNC_KEY_SPLIT);
		if( idx < 0 ) return null;
		
		return syncKey.substring(idx + 1);
	}
	
	public static boolean isEmpty( String syncKey ){
		int idx = syncKey.lastIndexOf(SYNC_KEY_SPLIT);
		if (idx >= syncKey.length() - 2) {
			return true;
		}
		return false;
	}
}
