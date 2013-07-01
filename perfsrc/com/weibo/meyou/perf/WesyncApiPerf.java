package com.weibo.meyou.perf;

import java.util.List;

import com.weibo.meyou.perf.sdk.message.WesyncExceptionPerf;
import com.weibo.meyou.perf.sdk.protocol.MetaMessageTypePerf;

/*
 * @ClassName: WesyncApi 
 * @Description: WesyncApi的接口列表
 * byte[] padding 客户端可以添加自己希望添加的内容
 * @param int timeout 超时时间 (可选)，如果设置的认证超时时间小于60s，则按照默认的超时时间
 * @param padding 附加信息 (可选) 客户端可以向其中添加附加信息，可以填 null
 * @author LiuZhao
 * @date 2012-9-10 下午4:53:42  
 */
public  interface WesyncApiPerf {

    /*
     * @Title: authUser
     * @Description: 认证用户身份（同步接口：只有身份认证通过后，才能进行后续操作）
     * @param String username 用户名 (必填)
     * @param String password 密码 (必填)
     * @return boolean 认证成功返回true
     * @throws WesyncException
     */
    boolean authUser(String username,String password,int timeout)  throws WesyncExceptionPerf;
    
    /*
     * @Title: sendText 
     * @Description: 发送文本消息，发送消息依赖于会话的同步过程
     * @param String msgId 消息ID  (必填)
     * @param String touid 接收方 (必填) / GROUPID
     * @param String text 发送的文本内容  (必填)
     * @return boolean 是否进入发送队列
     * @throws WesyncException 
     */
    boolean sendText(String msgId,String touid,String text,byte[] padding,
	    int timeout) throws WesyncExceptionPerf;
       
    /*
     * @Title: sendAudio
     * @Description: 发送语音文件，不分片，无缩略图
     * @param String msgId 消息ID (必填) / GROUPID
     * @param String touid 接收方 (必填)
     * @param byte[] audioData 语音数据 (必填)
     * @return boolean 是否进入发送队列
     * @throws WesyncException
     */
    boolean sendAudio(String msgId,String touid,byte[] audioData,
	    byte[] padding,int timeout) throws WesyncExceptionPerf;

    /*
     * @Title: sendFile 
     * @Description: 发送文件--图片/文本/视频
     * @param String msgId 消息ID (必填) / GROUPID
     * @param String touid 接收方 (必填)
     * @param String filepath 文件的保存路径 (必填)
     * @param WesyncFileType type 文件类型 (必填)
     * @param String 文件名 (必填)
     * @param String thumbnailPath 缩略图的存储路径 (可选) null
     * @param int[] index 包含要上传的分片编号组成的数组 (可选) null
     * @return int 返回分片的总数
     * @throws WesyncException
     */
    int sendFile(String msgId,String touid,String filepath,String filename,MetaMessageTypePerf type,
	    int[] index,byte[] padding,byte[] thumbnail,int timeout) throws WesyncExceptionPerf;

    /*
     * @Title: downloadFile 
     * @Description: 下载指定文件，需要指定，需要的文件ID，下载到的路径
     * @param String fileId 需要的文件ID (必填) 
     * @param String downloadPath 指定下载到的路径 (必填)
     * @param int fileLength 文件长度单位 byte   (必填)  
     * @param int[] index 包含要下载的分片编号组成的数组 (可选) null
     * @throws WesyncException
     */
    boolean downloadFile(String fileId,String downloadPath,int fileLength,int index[],
	    int timeout) throws WesyncExceptionPerf;
    
    /*
     * @Title: logout 
     * @Description: 退出SDK，清理掉所有资源
     * @throws WesyncException
     */   
    boolean logout() throws WesyncExceptionPerf;
    
    
    /*
     * @Title: uploadContact 
     * @Description: 上传通讯录
     * @param List<String> contact 通讯录的列表
     * @throws WesyncException
     */
    void uploadContact(List<String> contact,int timeout) throws WesyncExceptionPerf;
}
