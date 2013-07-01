package com.weibo.gw.test;

import org.junit.Test;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.weibo.wesync.notify.module.NotifyModule;
import com.weibo.wesync.notify.service.NotifySessionManager;

public class TestNotifyModule {

	@Test
	public void testNotifyModule(){
//			DataStore dataStore = (DataStore) GuiceProvider.getInstance(DataStore.class);
//			Injector injector = Guice.createInjector(new NotifyModule(), new DbModule(), new McModule(), new McqModule(), new RedisModule(), new ServiceModule());
//			Injector injector = Guice.createInjector(new NotifyModule());
//			injector.getAllBindings();
			
			
//			DataStore dataStore = injector.getInstance(DataStore.class);
//			System.out.println(dataStore.getClass().getName());
//			DataService dataService = injector.getInstance(DataService.class);
//			System.out.println(dataService.getClass().getName());
			
//			MessageDao mDao = injector.getInstance(MessageDao.class);
//			System.out.println(mDao.getClass().getName());
			
			Injector injector = Guice.createInjector(new NotifyModule());
			NotifySessionManager sessionManager = injector.getInstance(NotifySessionManager.class);
//			sessionManager.init();
			System.out.println("over");
	}
}
