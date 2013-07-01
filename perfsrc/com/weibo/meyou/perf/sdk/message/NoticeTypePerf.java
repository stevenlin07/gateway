/*   
 * @Title: NoticeType.java 
 * @Package com.weibo.meyou.sdk.store 
 * @Description: TODO(用一句话描述该文件做什么) 
 * @author LiuZhao  
 * @date 2012-9-12 上午12:34:13 
 * @version V1.0   
 */
package com.weibo.meyou.perf.sdk.message;

/*
 * @ClassName: NoticeType 
 * @Description: SDK对客户端的通知的分类
 * 分类
 * 1.异常类型：exception
 * @author LiuZhao
 * @date 2012-9-12 上午12:34:13  
 */
public enum NoticeTypePerf {
    conversation,
    exception,
    sendback,
    textmessage,
    filemessage,
    audiomessage,
    sendfile,
    downloadfile,
    weibo,
    contact,
    unknown,
    close
}
