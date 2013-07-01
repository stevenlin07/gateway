/*   
 * @Title: WesyncHandler.java 
 * @Package com.weibo.meyou.sdk.test 
 * @Description: TODO(用一句话描述该文件做什么) 
 * @author LiuZhao  
 * @date 2012-9-4 下午9:12:30 
 * @version V1.0   
 */
package com.weibo.meyou.perf.handler;

import com.weibo.meyou.perf.util.HttpUtilPerf;
import com.weibo.meyou.perf.util.ManagerCenterPerf;
import com.weibo.meyou.perf.util.WesyncStorePerf;

/*
 * @ClassName: WesyncHandler 
 * @Description: TODO(这里用一句话描述这个类的作用) 
 * @author LiuZhao
 * @date 2012-9-4 下午9:12:30  
 */
public abstract class BasicRespHandlerPerf implements ResponseHandlerPerf{  
    protected WesyncStorePerf wesyncStore = null;
    protected ManagerCenterPerf managerCenter = null;
    protected HttpUtilPerf httpUtil = null;
    protected static String TAG_SYNC_KEY = "0"; 
    
    public BasicRespHandlerPerf(WesyncStorePerf wesyncStore, ManagerCenterPerf managerCenter, HttpUtilPerf httpUtil){
		this.wesyncStore = wesyncStore;
		this.managerCenter = managerCenter;
		this.httpUtil = httpUtil;
    }
}
