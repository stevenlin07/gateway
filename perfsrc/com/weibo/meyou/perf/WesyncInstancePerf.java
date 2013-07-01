package com.weibo.meyou.perf;

import com.weibo.meyou.perf.util.HttpUtilPerf;
import com.weibo.meyou.perf.util.WesyncUtilPerf;

/*
 * @ClassName: WesyncInstance 
 * @Description: 对外接口
 * "apiservice" "user" "invite"对应接口的大类型
 * @author LiuZhao
 * @date 2012-9-10 下午5:01:20  
 */
public class WesyncInstancePerf extends WesyncApiImplPerf{
	
	public WesyncInstancePerf(){
		this.httpUtil = new HttpUtilPerf(wesyncStore, managerCenter);
	}
    
    // 网络管理接口，是嵌入在Broadcast中的网络情况
    public void netManager(boolean newConnected,int netType){ // 1 = WIFI,2 = MOBILE
	if(!flag) return; 
	connected = newConnected;
	if(newConnected){ // 如果是判断得到网络是联通的
	    if(httpUtil.startConnection())  // 如果网络连接操作成功，即可进行发送未读的请求
		WesyncUtilPerf.getUnreadItem(wesyncStore, httpUtil, managerCenter);	        
	}else{ // 如果接收广播，网络是中断的
	    httpUtil.stopConnection();	    
	}
    }
 
}