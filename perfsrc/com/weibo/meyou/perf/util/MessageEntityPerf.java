/*   
 * @Title: MessageEntity.java 
 * @Package com.weibo.meyou.sdk.store 
 * @Description: TODO(用一句话描述该文件做什么) 
 * @author LiuZhao  
 * @date 2012-9-4 下午4:15:02 
 * @version V1.0   
 */
package com.weibo.meyou.perf.util;

/*
 * @ClassName: MessageEntity 
 * @Description: 用来组装MessageEntity信息体，包含URL和ByteArrayEntity-->>HttpEngine
 * @author LiuZhao
 * @date 2012-9-4 下午4:15:02  
 */
public class MessageEntityPerf {
    
    private String uristr;
    private byte[] httpBody;
    
    public MessageEntityPerf(String uristr,byte[] httpBody){
	this.uristr = uristr;
	this.httpBody = httpBody;
    }

    public String getUristr() {
	return uristr;
    }

    public byte[] getHttpEntity() {
	return httpBody;
    }
}
