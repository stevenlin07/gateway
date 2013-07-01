package com.weibo.meyou.perf.sdk.message;

/*
 * @ClassName: MeyouNotice 
 * @Description: SDK对客户端的通知结构
 * @author LiuZhao
 * @date 2012-9-12 上午12:29:36  
 */
public class MeyouNoticePerf {

    /*
     * @Fields noticeType : 通知的类型
     * @Fields object : 通知的对象
     */ 
    private NoticeTypePerf noticeType; 
    private Object object;
    private String withtag;
    
    public MeyouNoticePerf(NoticeTypePerf noticeType,Object object,String withtag){
	this.noticeType = noticeType;
	this.object = object;
	this.withtag = withtag;
    }
    
    public NoticeTypePerf getNoticeType() {
	return noticeType;
    }
    public void setNoticeType(NoticeTypePerf noticeType) {
	this.noticeType = noticeType;
    }
    public Object getObject() {
	return object;
    }
    public void setObject(Object object) {
	this.object = object;
    }

    public String getWithtag() {
	return withtag;
    }

    public void setWithtag(String withtag) {
	this.withtag = withtag;
    }
}
