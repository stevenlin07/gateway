package com.weibo.meyou.perf.sdk.protocol;

/**
 * File id rules
 * @author Eric Liang
 */
public final class FolderIDPerf {
    
	public enum Type{
		Root, //the root, one per user
		Property, //contact, user info, etc. which can't be removed or cleared by user
		Conversation, //the conversation message store
		Group, //store group members information
		Unknown
	}
	
	public final static char FOLDER_SPLIT = '-';
	private static String ROOT_TAG = "root";
	private static String PROP_TAG = "prop";
	private static String CONV_TAG = "conv";
	private static String GROU_TAG = "grou";
	
	public String prefix="";
	public Type type;
	public String suffix="";
	
	public FolderIDPerf(String folderId){
		type = Type.Unknown;
		int idx1 = folderId.indexOf(FOLDER_SPLIT);
		if( idx1 >= 0 ){
			prefix = folderId.substring(0, idx1);

			int idx2 = folderId.indexOf(FOLDER_SPLIT, idx1+1);
			if (idx2 < 0) {
				if (folderId.substring(idx1 + 1).equals(ROOT_TAG)) {
					type = Type.Root;
				}
			} else {
				suffix = folderId.substring(idx2+1);
				String tag = folderId.substring(idx1+1, idx2);
				if (tag.equals(CONV_TAG)) type = Type.Conversation;
				else if (tag.equals(PROP_TAG)) type = Type.Property;
				else if (tag.equals(GROU_TAG)) type = Type.Group;
				else ;
			}
		}
	}
	
	public static String onRoot(String username) {
		return username+FOLDER_SPLIT+ROOT_TAG;
	}

	public static String onProperty(String username, String propName) {
		return createFolderId(username, PROP_TAG, propName);
	}

	public static String onConversation(String username, String userChatWith) {
		return createFolderId(username, CONV_TAG, userChatWith);
	}
	
	public static String onGroup(String username, String groupId){
		return createFolderId(username, GROU_TAG, groupId);
	}
	
	public static String getUsername(String folderId){
		return getPrefix(folderId);
	}
	public static String getUsername(FolderIDPerf fid){
		return fid.prefix;
	}
	
	public static boolean isBelongTo(String folderId, String username){
		if( username.equals( getUsername(folderId) )) return true;
		return false;
	}
	public static boolean isBelongTo(FolderIDPerf folderId, String username){
		if( username.equals( getUsername(folderId) )) return true;
		return false;
	}
	
	public static Type getType(String folderId){
		int idx1 = folderId.indexOf(FOLDER_SPLIT);
		if( idx1 < 0 ) return Type.Unknown;
		
		int idx2 = folderId.indexOf(FOLDER_SPLIT, idx1+1);
		if( idx2 < 0 ) {
			if( folderId.substring(idx1+1).equals(ROOT_TAG) ) return Type.Root;
		}else{
			String tag = folderId.substring(idx1+1, idx2);
			if( tag.equals(CONV_TAG) ) return Type.Conversation;
			if( tag.equals(PROP_TAG) ) return Type.Property;
			if( tag.equals(GROU_TAG) ) return Type.Group;
		}
		
		return Type.Unknown;
	}
	
	public static String getUserChatWith(String folderId){
		return getSuffix( new FolderIDPerf(folderId), Type.Conversation );
	}
	public static String getProperty(String folderId){
		return getSuffix( new FolderIDPerf(folderId), Type.Property );
	}
	
	public static String getGroup(String folderId){
		return getSuffix( new FolderIDPerf(folderId), Type.Group );
	}
	
	//Usually the FolderID instance will be more efficient than the static way.
	public static Type getType(FolderIDPerf fid){
		return fid.type;
	}
	public static String getUserChatWith(FolderIDPerf fid){
		return getSuffix(fid, Type.Conversation);
	}
	public static String getProperty(FolderIDPerf fid){
		return getSuffix(fid, Type.Property);
	}
	public static String getGroup(FolderIDPerf fid){
		return getSuffix(fid, Type.Group);
	}
	
	//Private utilities
	private static String createFolderId(String prefix, String tag, String suffix){
		return prefix+FOLDER_SPLIT+tag+FOLDER_SPLIT+suffix;
	}
	
	private static String getPrefix(String folderId){
		int idx = folderId.indexOf(FOLDER_SPLIT);
		if( idx <= 0 ) return "";
		return folderId.substring(0, idx);
	}
	private static String getSuffix(FolderIDPerf fid, Type check){
		if( getType(fid).equals(check) ) return fid.suffix;
		return "";
	}
}
