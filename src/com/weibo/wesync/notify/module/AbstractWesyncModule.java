package com.weibo.wesync.notify.module;

import java.io.InputStream;

import org.dom4j.Document;
import org.dom4j.io.SAXReader;

import cn.sina.api.commons.util.ApiLogger;

import com.google.inject.AbstractModule;

public abstract class AbstractWesyncModule extends AbstractModule {
	protected Document document;
	protected boolean needInitResource = true;

	@Override
	protected void configure() {
		
		initResource(getConfigPath());
		
		doOtherInitialization();
	}

	protected void initResource(String path) {
		//针对某些不需要加载配置文件的场景，例如service
		if(!getNeedInitResource()){
			return;
		}
		
		SAXReader xmlReader = new SAXReader();
		try {
			InputStream is = this.getClass().getClassLoader().getResourceAsStream(path);
			document = xmlReader.read(is);

			return;
		} catch (Exception e) {
			ApiLogger.error("initResource error, : " + e.getMessage());
			throw new RuntimeException("initResource error!");
		}
	}
	
	public abstract String getConfigPath();
	
	/**
	 * if you want initinalized other resource, place your code here.
	 */
	public void doOtherInitialization() {
		// Default do nothing
	}
	
	public boolean getNeedInitResource(){
		return needInitResource;
	}
}
