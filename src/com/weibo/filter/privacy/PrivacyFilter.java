package com.weibo.filter.privacy;

import org.apache.log4j.Logger;

import com.weibo.wesync.PrivacyService;
import com.weibo.wesync.data.WeSyncMessage.Meta;
import com.weibo.wesync.notify.utils.Util;

public class PrivacyFilter implements PrivacyService {

	private static Logger log = Logger.getLogger(PrivacyFilter.class);

	/**
	 * 是否允许消息通过，true，允许，false，不允许 (non-Javadoc)
	 * 
	 * @see com.weibo.wesync.PrivacyService#isMessagePermitted(java.lang.String,
	 *      java.lang.String, com.weibo.wesync.data.WeSyncMessage.Meta)
	 */
	@Override
	public boolean isMessagePermitted(String fromuid, String touid, Meta meta) {
		boolean flag = true; // 采用默认通过的策略
		if (!"true".equals(Util.getConfigProp("needfilter", "true"))) { // 权限部分：开关，true开启
			return true;
		}
		log.warn("[isMessagePermitted][NEED][CHECK]");
		if ("true".equals(Util.getConfigProp("identification", "true"))
				&& !IndetificationFilter.isMessagePermitted(fromuid, touid,
						meta)) { // 权限过滤
			log.warn("[isMessagePermitted][Indetification][FORBID]");
			return false;
		}
		log.warn("[isMessagePermitted][Indetification][PASS]");

		if ("true".equals(Util.getConfigProp("keyword", "true"))
				&& !KeywordFilter.isMessagePermitted(fromuid, touid, meta)) { // 内容过滤
			log.warn("[isMessagePermitted][KEYWORD][FORBID]");
			return false;
		}
		log.warn("[isMessagePermitted][KEYWORD][PASS]");
		return flag;
	}

}
