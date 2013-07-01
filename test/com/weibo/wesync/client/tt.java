package com.weibo.wesync.client;

import cn.sina.api.commons.util.JsonWrapper;


public class tt {

	/**
	 * @param args
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception {
		// TODO Auto-generated method stub
		String ret = "{\"apistatus\":1,\"a\":{\"uid\":\"1051\"}}";
		JsonWrapper json = new JsonWrapper(ret);
		String apistatus = json.get("apistatus");
		JsonWrapper ad = json.getNode("a");
		System.out.println(ad);
		if("1".equals(apistatus)){
			String ret1 = json.get("result");
			System.out.println(ret1);
			JsonWrapper result = new JsonWrapper(ret);
			System.out.println(result);
		}
	}

}
