package com.weibo.wesync.notify.utils;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import org.apache.log4j.Logger;
/**
 *  author : jichao1@staff.sina.com.cn
 *  
 */
public class DebugTools implements Runnable {
	private Logger log=Logger.getLogger(DebugTools.class);
	private static DebugTools instance=new DebugTools();
	private volatile boolean isDebugEnabled=false;	
	private volatile List<Object> debugList=new ArrayList<Object>();
	public static DebugTools getInstance() {
		return instance;
	}
	
	private DebugTools(){
		Thread t=new Thread(this);
		t.start();
	}

	public boolean isDebugEnabled() {
		return isDebugEnabled;
	}
	
	public boolean cantains(String content){
		boolean ret=false;
		for(int i=0;i<debugList.size();i++){
			if(content.contains(debugList.get(i).toString())){
				ret=true;
				break;
			}
		}
		
		return ret;
	}
	
	public boolean onDebugList(Object...args) {
		boolean ret=false;
		for(int i=0;i<args.length;i++){
			if(debugList.contains(args[i].toString())){
				ret=true;
				break;
			}
		}
		return ret;
	}

	public void run() {
		while(true){
			Properties props=new Properties();
//			InputStream input = DebugTools.class.getResourceAsStream("./firehose_debug_tools.prop");
			
			InputStream input = null;
			try {
				input = new FileInputStream("firehose_debug_tools.prop");
				props.load(input);
			} catch (IOException e1) {
				log.error("load debug info properties failed",e1);
			}finally{
				if(input!=null){
					try {
						input.close();
					} catch (IOException e) {
					}
				}
			}
			
			
			String debugOn = props.getProperty("firehose_debug_on");
			if(debugOn!=null && debugOn.equals("true")){
				isDebugEnabled=true;
			}
			
			String debugWords=props.getProperty("firehose_debug_list");
			if(debugWords!=null && !debugWords.equals("")){
				Object[] words = debugWords.split("_");
				debugList=Arrays.asList(words);
			}
			
			log.info("reload debug tools info,debug on="+isDebugEnabled+",debugList="+debugWords+",isOnDebugList="+onDebugList(1779195673L));
			try {
				Thread.sleep(1000 * 30);
			} catch (InterruptedException e) {
			}
		}
	}
	
	public static void main(String[] args) throws Exception{
		DebugTools dt=DebugTools.getInstance();
		Thread.sleep(1000);
		System.out.println(dt.isDebugEnabled());
		System.out.println(dt.cantains("sdfsdfsadfa,w23sdfdsdf 1779195673dfsdfsdf"));;
		System.out.println(dt.onDebugList(1779195673L));
	}
}

