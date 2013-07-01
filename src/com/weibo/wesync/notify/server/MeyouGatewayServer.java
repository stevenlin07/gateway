package com.weibo.wesync.notify.server;

import java.util.List;

import org.apache.log4j.Logger;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.weibo.meyou.notice.device.service.IDeviceService;
import com.weibo.wejoy.data.module.GuiceProvider;
import com.weibo.wejoy.data.processor.McqProcessor;
import com.weibo.wejoy.data.processor.MeyouMcqProcessor;
import com.weibo.wejoy.data.storage.MemCacheStorage;
import com.weibo.wejoy.wesync.listener.GroupChatService;
import com.weibo.wesync.DataService;
import com.weibo.wesync.WeSyncService;
import com.weibo.wesync.data.DataStore;
import com.weibo.wesync.gateway.business.WesyncListenerBusiness;
import com.weibo.wesync.gateway.business.WesyncListenerBusiness4Wejoy;
import com.weibo.wesync.notify.module.NotifyModule;
import com.weibo.wesync.notify.service.NotifySessionManager;
import com.weibo.wesync.notify.service.rpc.RpcServiceManager;
/**
 * 
 * @auth jichao1@staff.sina.com.cn 
 */
public class MeyouGatewayServer {
	private static Logger log = Logger.getLogger(MeyouGatewayServer.class);
	private static final long TIMEOUT = 60 * 4 * 1000L; // 4 mintues

	private MeyouHandlerManager handlerManager = null;
	private NotifySessionManager sessionManager;
	List<String> sysAppIdList;
	
	private void start() {		
		Injector injector = Guice.createInjector(new NotifyModule());		
		sessionManager = injector.getInstance(NotifySessionManager.class);
		sessionManager.init();
		
		WeSyncService weSync = injector.getInstance(WeSyncService.class);
		WesyncListenerBusiness wesyncListenerBusiness = new WesyncListenerBusiness4Wejoy(new GroupChatService(weSync));
		
		handlerManager = new MeyouHandlerManager(weSync, wesyncListenerBusiness);
		handlerManager.setSessionManager(sessionManager);
		
		DataService dataService = injector.getInstance(DataService.class);
		DataStore dataStore = GuiceProvider.getInstance(DataStore.class);
		
//	    PluginHandler groupHandler = injector.getInstance(GroupHandler.class);
//	    weSync.registerPlugin(groupHandler);
//	    weSync.addPropertySupport(Group.PROP_MEMBERS);
//	    weSync.addPropertySupport(Group.PROP_HISTORY);
				
		RpcServiceManager rpcManager = RpcServiceManager.getInstance();
		rpcManager.setWeSync(weSync);
    	rpcManager.start();
		
		MemCacheStorage noticeGWMemCache = sessionManager.getNoticeGWMemCache();
		IDeviceService deviceService = injector.getInstance(IDeviceService.class);
		
//		FirehoseManager firehoseManager = FirehoseManager.getInstance();
//		firehoseManager.dataService = dataService;
//		firehoseManager.weSync = weSync;
//		firehoseManager.dataStore = dataStore;
//		firehoseManager.noticeGWMemCache = noticeGWMemCache;
//		firehoseManager.deviceService = deviceService;
//		firehoseManager.start();
    	
		MeyouHttpServer server = MeyouHttpServer.getInstance();
		server.handlerManager = handlerManager;
		server.weSync = weSync;
    	server.start();    	    
    	
	}
	
	public static void main(String args[]) {
		MeyouGatewayServer server = new MeyouGatewayServer();
		server.start();
		
		MeyouMcqProcessor normalProcessor = (MeyouMcqProcessor)GuiceProvider.getInstance(McqProcessor.class, "normalProcessor");
	    normalProcessor.init();
	    MeyouMcqProcessor.setSystemInitSuccess();
	    MeyouMcqProcessor mediumProcessor = (MeyouMcqProcessor)GuiceProvider.getInstance(McqProcessor.class, "mediumProcessor");
	    mediumProcessor.init();
	    MeyouMcqProcessor.setSystemInitSuccess();
	    MeyouMcqProcessor largeProcessor = (MeyouMcqProcessor)GuiceProvider.getInstance(McqProcessor.class, "largeProcessor");
	    largeProcessor.init();
	    MeyouMcqProcessor.setSystemInitSuccess();
		
		try {
			Thread.currentThread().join();
		} catch (InterruptedException e) {
			log.error(e.getMessage(), e);
		}
	}
	
}
