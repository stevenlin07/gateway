/*   
 * @Title: WesyncException.java 
 * @Package com.weibo.meyou.sdk.support 
 * @Description: TODO(用一句话描述该文件做什么) 
 * @author LiuZhao  
 * @date 2012-8-31 上午10:11:25 
 * @version V1.0   
 */
package com.weibo.meyou.perf.sdk.message;

/*
 * @ClassName: WesyncException 
 * @Description: Wesync通讯SDK的异常处理
 * @author LiuZhao
 * @date 2012-8-31 上午10:11:25  
 */
public class WesyncExceptionPerf extends Exception {

    /*
     * @Fields serialVersionUID : (序列号) 
     */ 
    private static final long serialVersionUID = -2934675198929596571L;

    final public static int RequestTimeout = 601 ; // 请求超时
    final public static int AuthFailed = 602 ;     // 认证错误
    final public static int InputParamError = 603; // 输入参数错误
    final public static int CodeException = 604; // 编码错误
    final public static int BadFilePath = 605;// 文件路径错误失败
    final public static int BadFileRead = 606; // 文件读取失败
    final public static int BadFileWrite = 607; // 文件写入失败
    final public static int InterruptedException = 608; // SDK 被中断的异常
    final public static int ResponseError = 609; // 返回结果错误
    final public static int NetworkInterruption = 610; // 网络中断

    private int statusCode = -1;
	
    public WesyncExceptionPerf(String msg) {
	super(msg);
    }

    public WesyncExceptionPerf(Exception cause) {
	super(cause);
    }

    public WesyncExceptionPerf(String msg, int statusCode) {
	super(msg);
	this.statusCode = statusCode;
    }

    public WesyncExceptionPerf(String msg, Exception cause) {
	super(msg, cause);
    }

    public WesyncExceptionPerf(String msg, Exception cause, int statusCode) {
	super(msg, cause);
	this.statusCode = statusCode;
    }

    public int getStatusCode() {
	return this.statusCode;
    }
	    
	    
    public WesyncExceptionPerf() {
	super(); 
    }

    public WesyncExceptionPerf(String detailMessage, Throwable throwable) {
	super(detailMessage, throwable);
    }

    public WesyncExceptionPerf(Throwable throwable) {
	super(throwable);
    }

    public WesyncExceptionPerf(int statusCode) {
	super();
	this.statusCode = statusCode;
    }

    public void setStatusCode(int statusCode) {
	this.statusCode = statusCode;
    }
}
