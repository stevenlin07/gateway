/*   
 * @Title: ResponseHandler.java 
 * @Package com.weibo.meyou.sdk.test 
 * @Description: TODO(用一句话描述该文件做什么) 
 * @author LiuZhao  
 * @date 2012-9-4 下午8:16:28 
 * @version V1.0   
 */
package com.weibo.meyou.perf.handler;

import com.weibo.meyou.perf.sdk.protocol.MeyouPerf;

/*
 * @ClassName: ResponseHandler 
 * @Description: 响应的处理
 * @author LiuZhao
 * @date 2012-9-4 下午8:16:28  
 */
public interface ResponseHandlerPerf {
    public void handle(MeyouPerf.MeyouPacket meyouPacket);
}
