package com.weibo.wesync.notify.service.app;

public enum RequestType {
	POST(0),
	GET(1),
	MULTIPART(2),
	unknow(99);

	private final int value;

	private RequestType(int value) {
		this.value = value;
	}

	public int get() {
		return value;
	}
	
	public static RequestType valueOf(int value){
		for( RequestType type: RequestType.values() ){
			if ( value == type.value ) return type;
		}
		return unknow;
	}
}
