package com.weibo.wesync.notify.module;

import org.apache.log4j.Logger;
import org.dom4j.Element;

import cn.sina.api.commons.cache.driver.VikaCacheClient;

import com.google.inject.Key;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Names;
import com.weibo.filter.privacy.PrivacyFilter;
import com.weibo.meyou.notice.device.service.DeviceService;
import com.weibo.meyou.notice.device.service.IDeviceService;
import com.weibo.meyou.notice.device.storage.CacheService;
import com.weibo.meyou.notice.device.storage.IDeviceDao;
import com.weibo.meyou.notice.module.NoticeGuiceProvider;
import com.weibo.wejoy.data.module.GuiceProvider;
import com.weibo.wejoy.data.storage.MemCacheStorageImpl;
import com.weibo.wesync.DataService;
import com.weibo.wesync.FakeGroupMessageService;
import com.weibo.wesync.GroupMessageService;
import com.weibo.wesync.NoticeService;
import com.weibo.wesync.PrivacyService;
import com.weibo.wesync.WeSyncService;
import com.weibo.wesync.WeSyncServiceImpl;
import com.weibo.wesync.notify.service.NotifyManager;
import com.weibo.wesync.notify.service.NotifySessionManager;
import com.weibo.wesync.notify.xml.XmlUtil;
//import com.weibo.wesync.GroupMessageService;

public class NotifyModule extends AbstractWesyncModule {
	private String configPath = "notify-resource.xml";
	private static Logger log = Logger.getLogger(NotifyModule.class);
	
	@Override
	public String getConfigPath() {
		return configPath;
	}

	public void doOtherInitialization() {
		IDeviceDao deviceDao = NoticeGuiceProvider.INJECTOR.getInstance(Key.get(IDeviceDao.class, Names.named("deviceDao")));		
		CacheService cacheServ = NoticeGuiceProvider.INJECTOR.getInstance(Key.get(CacheService.class, Names.named("deviceMc")));
		DeviceService ds = new DeviceService(deviceDao, cacheServ);
		bind(IDeviceService.class).toInstance(ds);
		bind(DataService.class).toInstance(GuiceProvider.getInstance(DataService.class));	
		bind(NoticeService.class).to(NotifyManager.class);
		bind(PrivacyService.class).to(PrivacyFilter.class);//绑定拦截器
		bind(WeSyncService.class).to(WeSyncServiceImpl.class);
		
		
//
//		// for test
//		bind(DataStore.class).to(FakeDataStore.class);
//		bind(DataService.class).toInstance(new DataServiceImpl(new FakeDataStore()));
		
		bind(GroupMessageService.class).to(FakeGroupMessageService.class);
	}
	
	@Provides
	@Singleton
	NotifySessionManager provideNotifySessionManager() {
		NotifySessionManager manager = new NotifySessionManager();
		MemCacheStorageImpl<Object> mcs = new MemCacheStorageImpl<Object>();
		
		try {
			Element elem = XmlUtil.getRootElement(document);
			String expire = XmlUtil.getAttByName(elem, "expire");
			mcs.setExpire(Long.valueOf(expire));
			
			VikaCacheClient cacheClientMaster = new VikaCacheClient();
			initConfig(cacheClientMaster, XmlUtil.getElementByName(elem, "master"));
			mcs.setCacheClientMaster(cacheClientMaster);

			VikaCacheClient cacheClientSlave = new VikaCacheClient();
			initConfig(cacheClientSlave, XmlUtil.getElementByName(elem, "slave"));
			mcs.setCacheClientSlave(cacheClientSlave);
			
			manager.setNoticeGWMemCache(mcs);
		}
		catch(Exception e) {
			log.error(e.getMessage(), e);
		}
		
		return manager;
	}
	
	
	private void initConfig(VikaCacheClient cacheClient, Element config) throws Exception {
		String minSpareConnections = XmlUtil.getAttByName(config, "minSpareConnections");
		String maxSpareConnections = XmlUtil.getAttByName(config, "maxSpareConnections");
		String consistentHashEnable = XmlUtil.getAttByName(config, "consistentHashEnable");
		String failover = XmlUtil.getAttByName(config, "failover");
		
		cacheClient.setMinSpareConnections(Integer.valueOf(minSpareConnections));
		cacheClient.setMaxSpareConnections(Integer.valueOf(maxSpareConnections));
		cacheClient.setConsistentHashEnable(Boolean.valueOf(consistentHashEnable));
		cacheClient.setFailover(Boolean.valueOf(failover));
		
		String serverPort = XmlUtil.getAttByName(config, "serverport");
		cacheClient.setServerPort(serverPort);
		
		String primitiveAsString = XmlUtil.getAttByName(config, "primitiveAsString");
		if(primitiveAsString != null){
			boolean isPrimitivieAsString = Boolean.valueOf(primitiveAsString);			
			cacheClient.setPrimitiveAsString(isPrimitivieAsString);
		}
		
		cacheClient.init();
	}
}
