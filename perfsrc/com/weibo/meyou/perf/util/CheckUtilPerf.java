package com.weibo.meyou.perf.util;

import java.util.List;
import java.util.Map;

import com.weibo.meyou.perf.sdk.message.WesyncExceptionPerf;


/*
 * @ClassName: CheckInputUtil 
 * @Description: TODO(这里用一句话描述这个类的作用) 
 * @author LiuZhao
 * @date 2012-10-24 上午12:59:06  
 */
public class CheckUtilPerf {

    private static int defaultTimeout = 60; // 默认的超时时间
    private static int inputLengthMax = 50 * 1024; // 默认的超时时间
    private static int uidLengthMax = 15;
    
    public static int StringCheck(String str,String paramName)  throws WesyncExceptionPerf{
	if(null == str || "".equals(str.trim()))
	    throw new WesyncExceptionPerf("必填参数"+paramName+"不能为空",WesyncExceptionPerf.InputParamError); 
	return str.length();
    }
    
    public static int GroupListCheck(List<String> groupMembers,String paramName,String fromuid)  throws WesyncExceptionPerf{
 	if(null == groupMembers || 0 >= groupMembers.size())
 	    throw new WesyncExceptionPerf("必填参数"+paramName+"不能为空",WesyncExceptionPerf.InputParamError); 
 	int metaLength = 0;
 	for(String uid:groupMembers){
	    if(fromuid.equals(uid)){
		throw new WesyncExceptionPerf("输入参数错误：不能将自己加入群/通讯录中",WesyncExceptionPerf.InputParamError); 
	    }
	    metaLength = metaLength + uid.length();
	}	
 	return metaLength;
     }
    
    public static int IntCheck(int input,String paramName)  throws WesyncExceptionPerf{
	if( 0 >= input)
	    throw new WesyncExceptionPerf("必填参数"+paramName+"不能为空",WesyncExceptionPerf.InputParamError); 
	return 1;
    }
    
    public static int ByteArrayCheck(byte[] bytearray,String paramName)  throws WesyncExceptionPerf{
	if(null == bytearray || bytearray.length <= 0)
	    throw new WesyncExceptionPerf("必填参数"+paramName+"不能为空",WesyncExceptionPerf.InputParamError); 
	return bytearray.length;
    }
    
    public static int TimeoutCheck(int timeout,int param)  throws WesyncExceptionPerf{
	if(timeout < (defaultTimeout * param)){
	    timeout = defaultTimeout * param;
	}
	return timeout;
    }
    
    public static void AuthCheck(boolean flag)  throws WesyncExceptionPerf{
	if(!flag)
	    throw new WesyncExceptionPerf("用户尚未进行认证",WesyncExceptionPerf.AuthFailed);
    }
    
    public static int TouidCheck(String touid,String fromuid)  throws WesyncExceptionPerf{
	if(null == touid || "".equals(touid.trim()))
	    throw new WesyncExceptionPerf("必填参数touid不能为空",WesyncExceptionPerf.InputParamError); 
	if(fromuid.equals(touid))
	    throw new WesyncExceptionPerf("用户不能给自己发送消息",WesyncExceptionPerf.InputParamError);
	if(touid.length() >= uidLengthMax)
	    throw new WesyncExceptionPerf("touid过长",WesyncExceptionPerf.InputParamError); 
	return touid.length();
    }
    
    public static void InputLengthCheck(int inputLength,byte[] padding)  throws WesyncExceptionPerf{
	if(null != padding){
	    inputLength = inputLength + padding.length;
	}
	if(inputLengthMax < inputLength){
	    throw new WesyncExceptionPerf("输入参数错误：参数输入内容超过限制，建议不超过50K",WesyncExceptionPerf.InputParamError);
	}	
    }
    
    public static void MapCheck(Map<String, String> paramerters,String paramName)  throws WesyncExceptionPerf{
 	if(null == paramerters )
 	    throw new WesyncExceptionPerf("必填参数"+paramName+"不能为空",WesyncExceptionPerf.InputParamError); 
     }
}
