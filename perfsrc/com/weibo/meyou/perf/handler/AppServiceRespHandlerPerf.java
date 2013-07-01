package com.weibo.meyou.perf.handler;

import java.util.Timer;

import com.weibo.meyou.perf.sdk.protocol.MeyouPerf.MeyouPacket;
import com.weibo.meyou.perf.util.HttpUtilPerf;
import com.weibo.meyou.perf.util.ManagerCenterPerf;
import com.weibo.meyou.perf.util.WesyncStorePerf;

/*
 * @ClassName: AppServiceRespHandler 
 * @Description: 处理微博业务接口的调用返回结果
 * @author LiuZhao
 * @date 2012-9-10 下午9:10:14  
 */
public class AppServiceRespHandlerPerf extends BasicRespHandlerPerf {

	public AppServiceRespHandlerPerf(WesyncStorePerf wesyncStore, ManagerCenterPerf managerCenter, HttpUtilPerf httpUtil){
		super(wesyncStore, managerCenter, httpUtil);
	}
	
    @Override
    public void handle(MeyouPacket meyouPacket) {
	String withtag = meyouPacket.getCallbackId();	
	if(wesyncStore.tagTimeList.containsKey(withtag)){// 说明接收成功
	    Timer t = wesyncStore.tagTimeList.get(withtag);
	    t.cancel();
	    wesyncStore.tagTimeList.remove(withtag);
//	    String content = meyouPacket.getContent().toStringUtf8();
//	    MeyouNoticePerf notice = new MeyouNoticePerf(NoticeTypePerf.weibo,content,withtag);
//	    NotifyCenterPerf.clientNotifyChannel.add(notice);	
	}
    }
}
