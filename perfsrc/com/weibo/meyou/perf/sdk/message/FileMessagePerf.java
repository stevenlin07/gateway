package com.weibo.meyou.perf.sdk.message;

import java.util.List;

import com.weibo.meyou.perf.sdk.protocol.MetaMessageTypePerf;



/*
 * @ClassName: FileMessage 
 * @Description: 处理视频，图片，普通文件
 * 扩展参数：
 * 文件的ID
 * 缩略图的数据
 * 文件的长度
 * @author LiuZhao
 * @date 2012-9-19 下午4:51:31  
 */
public class FileMessagePerf extends MYMessagePerf {
    public String fileId;
    public byte[] thumbData;
    public MetaMessageTypePerf type;
    public int fileLength;
    public int limit;
    public List<Integer> hasReveive;
    public String filename;
}
