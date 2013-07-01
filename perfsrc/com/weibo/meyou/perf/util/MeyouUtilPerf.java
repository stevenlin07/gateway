package com.weibo.meyou.perf.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

import com.google.protobuf.ByteString;
import com.weibo.meyou.perf.sdk.message.WesyncExceptionPerf;
import com.weibo.meyou.perf.sdk.protocol.MeyouPerf;


public class MeyouUtilPerf {
    
    public static String getMsgId() { // 可由客户端指定，保持唯一，请小于15位
	return String.valueOf(System.currentTimeMillis());
    }
    
    public static void dealException(WesyncExceptionPerf e){
	int statusCode = e.getStatusCode();
	switch(statusCode){
	case WesyncExceptionPerf.InputParamError: // 输入参数错误
	    System.out.println(e.getMessage());
	    break;
	case WesyncExceptionPerf.CodeException: // 编码处理错误
	    System.out.println(e.getMessage());
	    System.out.println(e.getCause());
	    break;
	case WesyncExceptionPerf.InterruptedException: // 请求被中断
	    System.out.println(e.getMessage());
	    System.out.println(e.getCause());
	    break;
	case WesyncExceptionPerf.RequestTimeout: // 会话请求超时
	    System.out.println(e.getMessage()+" 请求超时");
	    break;
	case WesyncExceptionPerf.BadFileRead: // 文件读取失败
	    System.out.println(e.getMessage());
	    System.out.println(e.getCause());
	    break;
	case WesyncExceptionPerf.ResponseError: // 文件读取失败
	    System.out.println(e.getMessage());
	    System.out.println(e.getCause());
	    break;   
	case WesyncExceptionPerf.AuthFailed: // 认证失败
	    System.out.println(e.getMessage());
	    break;  
	case WesyncExceptionPerf.NetworkInterruption: // 网络中断
	    System.out.println(e.getMessage());
	    break;
	default:
	    System.out.println(e.getMessage());
	    System.out.println(e.getCause());
	}	
    }
    
    
    public static byte[] generateRequestEntity(String callbackId,int sort,ByteString content){
	MeyouPerf.MeyouPacket meyouPacket = null;
	if(content!=null){
	    meyouPacket = MeyouPerf.MeyouPacket.newBuilder()
		    .setCallbackId(callbackId)
		    .setSort(sort)
		    .setContent(content).build();
	}else{
	    meyouPacket = MeyouPerf.MeyouPacket.newBuilder()
		    .setCallbackId(callbackId)
		    .setSort(sort)
		    .build();
	}
	return meyouPacket.toByteArray();
    }
    

    public static MeyouPerf.MeyouPacket generateRequestPacket(String callbackId,int sort,byte[] content){
    	MeyouPerf.MeyouPacket meyouPacket = null;
		if(content!=null){
		    meyouPacket = MeyouPerf.MeyouPacket.newBuilder()
			    .setCallbackId(callbackId)
			    .setSort(sort)
			    .setContent(ByteString.copyFrom(content)).build();
		}else{
		    meyouPacket = MeyouPerf.MeyouPacket.newBuilder()
			    .setCallbackId(callbackId)
			    .setSort(sort)
			    .build();
		}
		return meyouPacket;
    }
    
    public static byte[] generateRequestEntityExt(String callbackId,int sort,
	    ByteString content,String picfilePath) throws WesyncExceptionPerf{
	File picFile = new File(picfilePath);
	byte[] buffer = new byte[(int) picFile.length()];
	RandomAccessFile file;
	try {
	    file = new RandomAccessFile(picFile, "rw");
	    file.read(buffer);
	    file.close();
	    MeyouPerf.MeyouPacket meyouPacket = null;
	    meyouPacket = MeyouPerf.MeyouPacket.newBuilder()
		    .setCallbackId(callbackId)
		    .setSort(sort)
		    .setContent(content)
		    .setAttache(ByteString.copyFrom(buffer))		   
		    .build();
	    byte[] entity = meyouPacket.toByteArray();
	    return entity;
	} catch (FileNotFoundException e) {
	    throw new WesyncExceptionPerf(e);
	} catch (IOException e) {
	    throw new WesyncExceptionPerf(e);
	} 
    }
    public static String generateCallbackId(String commandStr){
	return commandStr;
    }
    
}
