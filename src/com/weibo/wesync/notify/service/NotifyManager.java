package com.weibo.wesync.notify.service;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.mortbay.log.Log;

import cn.sina.api.commons.util.JsonWrapper;

import com.google.inject.Inject;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.weibo.meyou.notice.device.service.IDeviceService;
import com.weibo.meyou.notice.iospush.apns.IosOfflineNoticePushManagers;
import com.weibo.meyou.notice.model.Device;
import com.weibo.meyou.notice.service.NoticeData;
import com.weibo.meyou.notice.utils.OpenAPIDataUtils;
import com.weibo.meyou.notice.utils.StorageUtil;
import com.weibo.meyou.notice.utils.UserSetting;
import com.weibo.wejoy.data.constant.DataConstants;
import com.weibo.wesync.NoticeService;
import com.weibo.wesync.data.MetaMessageType;
import com.weibo.wesync.data.WeSyncMessage.Meta;
import com.weibo.wesync.data.WeSyncMessage.Notice;
import com.weibo.wesync.data.WeSyncMessage.SyncReq;
import com.weibo.wesync.data.WeSyncMessage.Unread;
import com.weibo.wesync.notify.protocols.Meyou;
import com.weibo.wesync.notify.protocols.MeyouSort;
import com.weibo.wesync.notify.service.rpc.RpcPoolClient;
import com.weibo.wesync.notify.service.rpc.RpcServiceManager;
import com.weibo.wesync.notify.utils.ConfigConstants;
import com.weibo.wesync.notify.utils.StatLog;
import com.weibo.wesync.notify.utils.Util;
/**
 * 
 * @auth jichao1@staff.sina.com.cn 
 */
public class NotifyManager implements NoticeService {
	private static Logger log = Logger.getLogger(NotifyManager.class);
	private NotifySessionManager noticeSessionManager;
	private static final int OK_CODE = 200;
	private static final String OK_TEXT = "OK";
	private IDeviceService deviceService;
	private IosOfflineNoticePushManagers iosApnPushManagers = new IosOfflineNoticePushManagers();
	private ExecutorService pushExecutor = null;
	private LinkedBlockingQueue<Runnable> pushTasks = null;
	
	public void setDeviceService(IDeviceService deviceService){
		this.deviceService = deviceService;
	}
	
	@Inject
	public NotifyManager(NotifySessionManager noticeSessionManager, IDeviceService deviceService){
		this.noticeSessionManager = noticeSessionManager;
		this.deviceService = deviceService;
		
		pushTasks = new LinkedBlockingQueue<Runnable>();
		int size = Util.getConfigProp(ConfigConstants.NOTICE_THREAD_POOL_SIZE, 16);
		pushExecutor = new ThreadPoolExecutor(size, size, 0L, TimeUnit.MILLISECONDS, pushTasks);
		log.info("实例化了###########################################");
		new PushTasksChecker().start();
	}
	
	public void send(String toUserName, Notice notice) {
		PushTask task = new PushTask();
		task.toUserName = toUserName;
		log.warn("[SEND NOTICE TO "+toUserName+"]");
		task.notice = notice;
		pushExecutor.execute(task);
	}
	
	private class PushTask implements Runnable {
		Notice notice;
		String toUserName;
		
		public void run() {
			long t1 = System.currentTimeMillis();
			try {
				StatLog.inc("Wesync_notice");
				
				NotifySession localsession = noticeSessionManager.getLocalSession(toUserName);
				NotifySession remotesession = null;
				String localInternalIp = noticeSessionManager.getLocalIp();
				
				// 用户的长连接在本网关上，可以在线推送消息
				if(localsession != null) {
					Meyou.MeyouPacket resultPacket = Meyou.MeyouPacket.newBuilder()
							.setSort(MeyouSort.wesync)
							.setContent(ByteString.copyFrom(notice.toByteArray()))
							.setCode(OK_CODE)
							.setText(OK_TEXT)
							.build();
					
					if ( null != localsession.ioSession) {
						localsession.pushToClient(resultPacket);
						log.debug(toUserName + " notice sent by NotifyManager");
					}
					else {
						log.error(toUserName + " send notice [" + notice + "] failed caused by iosession is null");
					}
				}
				else if((remotesession = noticeSessionManager.getRemoteSession(toUserName)) != null) {
					// 用户的长连接不在本网关上，但remote session在本网关上，这是不对的，
					// 造成这种情况的可能原因是服务器重启后，远程session中的IP虽然和本机一致，但用户并没有连接到本网关上，
					// 这种情况下需要干掉远程session，使得后续服务都走offline push
					if(localInternalIp.equals(remotesession.getPushServerIp())) {
						log.warn("No valid iosession acquired, server can not send message to "+ toUserName);
						noticeSessionManager.removeRemoteSession(toUserName);
						
						// consider as offline, try to push by APNs
						pushNoticeByAPNs(toUserName, notice);
					}
					// 用户连接不在本网关上，send to target push gateway by RPC
					else {
						Log.debug("Notify [" + notice + "] to " + toUserName + "is not local pushed, send to " + 
								remotesession.getPushServerIp());
						RpcPoolClient client = RpcServiceManager.getInstance().getRpcClient(remotesession.getPushServerIp());
						
						if ((client != null) && client.isConnected()) {
							long t3 = System.currentTimeMillis();
							client.send(toUserName, ByteBuffer.wrap(notice.toByteArray()));
							long t4 = System.currentTimeMillis();
							
							if((t4 - t3) > Util.getConfigProp(ConfigConstants.NOTICE_RPC_SLOW, 100)) {
								log.warn("Send notice by RPC to " + remotesession.getPushServerIp() +
										" is slow, using=" + (t4 - t3) + " notice=" + notice);
							}
						} 
						// TO DO 没有连接上，是否就说明target srver挂了呢？？
						else {
							log.error(toUserName + " RPC send failed caused by remote server " + remotesession.getPushServerIp() +
									(client == null ? " is null" : "is not Connected") + ", remove remote session");
							// 没有新服务器在旧IP上重启
							noticeSessionManager.removeRemoteSession(toUserName);
						}
					}
				}
				else {
					// offline, try to push by APNs
					pushNoticeByAPNs(toUserName, notice);
				}
				
			}
			catch(Exception e) {
				log.error(e.getMessage(), e);
			}
			
			long t2 = System.currentTimeMillis();
			
			if((t2 - t1) > Util.getConfigProp(ConfigConstants.NOTICE_PUSH_SLOW, 100)) {
				log.warn(toUserName + " send notice is slow, using=" + (t2 - t1));
			}
		}
	}
	
	/*
	 * 向注册了APNs服务的用户推送离线消息。
	 */
	private void pushNoticeByAPNs(String toUserName, Notice notice) {
		long t1 = System.currentTimeMillis();
		
		try {
			long toUid = Long.valueOf(toUserName);
			Device device = deviceService.getDeviceByUserId(toUid);
			List<Unread> unreadList = notice.getUnreadList();
			
			for (Unread ur : unreadList) {
				NoticeData noticeData = new NoticeData();
				Meta meta = ur.getContent();
				JsonWrapper userShowJson = OpenAPIDataUtils.instance.callUserShow(Long.parseLong(meta.getFrom()));
				String nick = userShowJson.get("screen_name");
				MetaMessageType metaType = MetaMessageType.valueOf(meta.getType().byteAt(0));
				String msg = null;
				
				if(metaType == MetaMessageType.text) {
					if(meta.getContent() != null){
						msg = new String(meta.getContent().toByteArray(), DataConstants.DEFAULT_CHARSET);
						noticeData.content = nick + ":" + msg;                    	
					}
				} 
				else if(metaType == MetaMessageType.image){
					noticeData.content = nick + "发来一张图片";
					msg = "MetaMessageType.image";
				} 
				else if(metaType == MetaMessageType.audio){
					noticeData.content = nick + "发来一段语音";
					msg = "MetaMessageType.audio";
				}
				
				noticeData.touid = String.valueOf(toUid);
				noticeData.addEntry(NoticeData.ENTRY_KEY_SHOW_NUM, String.valueOf(ur.getNum()));
				
				log.debug(toUid + " send IOS offline push notice by APNs device=" +
						(device == null ? "null" : device.getAppkey() + "|"+ device.getDeviceId())
						+ " notice=" + getDebugInfo(notice) + " msg=" + msg);
				
				if(device != null){     //user has registered ios
					if(StorageUtil.LANSHAN_APPKEY.equals(String.valueOf(device.getAppkey()))){
						if(UserSetting.isChatNumSet(device.getSwitchInfo())){
							//推送通知到zuju（只推送密友聊天通知）
							noticeData.type = NoticeData.NoticeType.Chat4Zuju;
							iosApnPushManagers.sendNotification(device, noticeData);
						}
					} else {
						if(UserSetting.isChatTextSet(device.getSwitchInfo())){
							noticeData.addEntry("fuid", meta.getFrom());
							noticeData.addEntry("tuid", toUserName);
							noticeData.type = NoticeData.NoticeType.Chat;
						} else {
							log.debug(toUid + " set not push chat notice, msg:" + msg + "; notice type:" + noticeData.type);
							noticeData.content = null;
							//here don't call noticeData.clearEntries() cause we need unread chat num later 
							noticeData.type = NoticeData.NoticeType.UnReadNum;                        		
						}
						
						//推送通知到meyou
						iosApnPushManagers.sendNotification(device, noticeData);
					}
				} else {
					log.debug(toUid + " is discarded because user is offline notice " + getDebugInfo(notice));
				}
			}
		}
		catch(Exception e) {
			log.error(e.getMessage(), e);
		}
		
		long t2 = System.currentTimeMillis();
		
		if((t2 - t1) > Util.getConfigProp(ConfigConstants.NOTICE_APN_SLOW, 100)) {
			log.warn(toUserName + " send notice by APNs is slow, using=" + (t2 - t1));
		}
	}
	
	/*
	 * don't display too much binary data such as thumbnail, audio
	 */
	private String getDebugInfo(Notice notice) {
		try {
			Notice.Builder noticeBuilder = Notice.newBuilder();
			List<Unread> unreads = notice.getUnreadList();
			
			for(int i = 0; unreads != null && i < unreads.size(); i++) {
				noticeBuilder.addUnread(unreads.get(i));
			}
			
			if(notice.getExpectAckCount() > 0) {
				noticeBuilder.addExpectAck(notice.getExpectAck(0));
			}
			
			List<Meta> messages = notice.getMessageList();
			int index = 0;
			
			for(Meta message : messages) {
				Meta.Builder metabuilder = Meta.newBuilder();
				metabuilder.mergeFrom(message.toByteArray());
				MetaMessageType metatype = MetaMessageType.valueOf(message.getType().byteAt(0));
				
				if(metatype.equals(MetaMessageType.audio)) {
					metabuilder.setContent(ByteString.copyFromUtf8(""));
				}
				
				if(metatype.equals(MetaMessageType.image)) {
					metabuilder.setThumbnail(ByteString.copyFromUtf8(""));
				}
				
				noticeBuilder.addMessage(metabuilder.build());
			}
			
			return noticeBuilder.build().toString();
		} catch (InvalidProtocolBufferException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return notice.toString();
	}
	
	private class PushTasksChecker extends Thread {
		public void run() {
			try {
				while(true) {
					int interval = Util.getConfigProp(ConfigConstants.PUSHTASK_CHECK_INTERVAL, 5 * 60 * 1000);
					int warnNumber = Util.getConfigProp(ConfigConstants.PUSHTASK_CHECK_INTERVAL, 10000);
					
					if(pushTasks.size() > warnNumber) {
						log.warn("push task is so many = " + pushTasks.size());
					}
					Thread.sleep(interval);
				}
			}
			catch(Exception e) {
				log.warn(e.getMessage(), e);
			}
		}
	}
}
