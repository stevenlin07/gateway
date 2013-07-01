package com.weibo.meyou.perf.sdk.message;

/*
 * @ClassName: MYMessage 
 * @Description: 所有消息的父类
 * @author LiuZhao
 * @date 2012-9-19 下午4:52:19  
 */
public class MYMessagePerf {
    public String msgId;
    public String fromuid;
    public String touid;
    public long time;
    public byte[] padding;
}