package com.weibo.wesync.notify;

import java.util.HashMap;
import java.util.Map;

public class LocalStoreMock {
	Map<String, String> localSyncKeys = new HashMap<String, String>();
	Map<String, String> localFolderIds = new HashMap<String, String>();
	
	String getFolderId(String uid, String userChatWith) {
		return localFolderIds.get(uid + "_" + userChatWith);
	}
	
	void setFolderId(String uid, String userChatWith, String folderId) {
		localFolderIds.put(uid + "_" + userChatWith, folderId);
	}
	
	String getSynckey(String uid, String folderId) {
		return localSyncKeys.get(uid + "_" + folderId);
	}
	
	void setSynckey(String uid, String folderId, String synckey) {
		localSyncKeys.put(uid + "_" + folderId, synckey);
	}
}
