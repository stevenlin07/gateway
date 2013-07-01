package com.weibo.meyou.perf.util;

import java.io.IOException;

import com.weibo.meyou.perf.sdk.message.WesyncExceptionPerf;
import com.weibo.meyou.perf.sdk.protocol.CommandPerf;
import com.weibo.meyou.perf.sdk.protocol.WeSyncURIPerf;


/*
 * @ClassName: ManagerCenter 
 * @Description: 用来管理整个SDK的所有可配置的参数，以及SDK所包含的由外部输入的信息
 * 1.用户的基本信息：用户名,密码,uid,token，
 * 2.服务信息:服务器的IP，端口，host
 * 3.网络状况管理
 * 4.认证时间管理
 * 5.心跳的管理（根据网络状况进行管理）
 * @author LiuZhao
 * @date 2012-9-13 下午1:28:05  
 */
public class ManagerCenterPerf {

    private String username ;
    private String password ;
    private String uid ;
    private String token;
    // 根据各种分析，实在无法避免....需要知道sliceSize，因为反向无法还原
    private int sliceSize = 50;	// 发送文件分片
    private int metaMaxLength = 50 * 1024; // meta的最大长度限制 50K
    // 
    private String serverIp= "10.210.230.33";  // "long.meyou.weibo.com" "123.126.42.25" "10.210.230.55"
    private int[] serverPort= new int[]{7070, 8080};
    private int portchoose = 0;
    private String appKey = "2502274771"; //LanShan-zuju-app："2502274771"，Meyou-Android："3065004722"
    private byte protocolVersion = 10;
    private DeviceInfoPerf deviceInfo ;
    static boolean debug = false;
    
    public ManagerCenterPerf(){
	
    }
    
    // 获取加密后的用户名和密码组成的字符串，并对字符串进行Base64编码
    public String getUsernamePassword(){	
	String upw = this.username + ":"+ this.password;
	String result = Base64Perf.encodeBase64String(upw.getBytes());
	return result;
    }
    
    // 设置用户名密码，输入用户名密码之后就对其进行加密，在管理中心存储的实际上是加过密的密码
    public void setUsernamePassword(String username,String password) throws WesyncExceptionPerf {
	this.username = username;
	try {
	    this.password = RsaToolPerf.encryptPassword(password);
	} catch (WesyncExceptionPerf e) {
	    throw new WesyncExceptionPerf(e.getMessage(),e,WesyncExceptionPerf.CodeException);
	}	     
    }
    
    public void clear(){ // 清除管理中心的所有信息
//	DebugOutput.println("Clear managerCenter information");
//	HttpUtil.stop();
	this.password = null;
	this.username = null;
	this.uid = null;
	this.token = null;
	this.deviceInfo = null;
	portchoose = 0;
    }
     
    // --------------- base information -----
    
    
    public String getServerIp() {
	return serverIp;
    }
    public void setServerIp(String serverIp) {
	this.serverIp = serverIp;
    }
    public int getServerPort() {
	return serverPort[portchoose];
    }
    public void setServerPort(int portchoose) {
	this.portchoose = portchoose;
    }
    
    public int[] getServerPorts(){
    	return serverPort;
    }
    
    public void setServerPort(int[] ports){
    	this.serverPort = ports;
    }
    
    public int getSliceSize() {
	return sliceSize;
    }
    public void setSliceSize(int sliceSize) {
	this.sliceSize = sliceSize;
    }

    public int getMetaMaxLength() {
	return metaMaxLength;
    }
    
    public String getUid() {
	return uid;
    }
    public void setUid(String uid) {
	this.uid = uid;
    }
    public String getToken() {
	return token;
    }
    public void setToken(String token) {
	this.token = token;
    }
    public void checkDeviceInfo() throws WesyncExceptionPerf {
	if(null == deviceInfo)
	    throw new WesyncExceptionPerf("请先设置设备信息",WesyncExceptionPerf.InputParamError); 
    }
    
    public void setDeviceInfo(DeviceInfoPerf deviceInfo,WesyncStorePerf wesyncStore) throws WesyncExceptionPerf {
	checkDeviceInfo(deviceInfo);
	WeSyncURIPerf uri = new WeSyncURIPerf();
	uri.protocolVersion = this.protocolVersion;
	uri.guid = deviceInfo.guid;
	uri.deviceType = deviceInfo.deviceType;
	uri.clientVersionMajor = deviceInfo.clientVersionMajor;
	uri.clientVersionMinor = deviceInfo.clientVersionMinor;
	for(CommandPerf command:CommandPerf.values()){
	    uri.command = command.toByte();
	    wesyncStore.uriMap.put(command.toString(), getWeSyncURIStr(uri));
	}
	this.deviceInfo = deviceInfo;
    }
    
    private String getWeSyncURIStr(WeSyncURIPerf uri) throws WesyncExceptionPerf{	
	String uristr = null;
	try {
	    byte[] uridata = WeSyncURIPerf.toBytes(uri);	    
	    uristr = new String(Base64Perf.encodeBase64(uridata));
	} catch (IOException e) {
	    throw new WesyncExceptionPerf("设备信息不规范",e,WesyncExceptionPerf.CodeException);
	}
	return uristr;
    }
    
    private void checkDeviceInfo(DeviceInfoPerf deviceInfo) throws WesyncExceptionPerf{
	CheckUtilPerf.StringCheck(deviceInfo.guid, "guid");
	CheckUtilPerf.StringCheck(deviceInfo.deviceType, "deviceType");
    }
    
    public String getAppKey() {
	return appKey;
    }
    public byte getProtocolVersion() {
	return protocolVersion;
    }
}
