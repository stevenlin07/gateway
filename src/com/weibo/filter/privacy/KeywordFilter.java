package com.weibo.filter.privacy;

import com.weibo.wesync.data.WeSyncMessage.Meta;

public class KeywordFilter {

	public static boolean isMessagePermitted(String fromuid, String touid, Meta meta) {
		return true;
	}
}
