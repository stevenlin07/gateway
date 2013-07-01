package com.weibo.meyou.perf.sdk.protocol;

public class MeyouSortPerf {
	public static final int heartbeat = 0x001;
	public static final int close = 0x011;
	public static final int wesync = 0x101;
	public static final int handshake = 0x111;
	public static final int appservice = 0x131;
	public static final int notice = 0x112; // wesync notice
	public static final int meyou_gateway_notice = 0x114; // meyou gateway notice
	public static final int contact = 0x141;
}
