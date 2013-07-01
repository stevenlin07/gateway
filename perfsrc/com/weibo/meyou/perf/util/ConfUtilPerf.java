package com.weibo.meyou.perf.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import com.weibo.meyou.perf.sdk.message.WesyncExceptionPerf;

public class ConfUtilPerf {
	private static ConfUtilPerf confUtilPerf;
	private String audio4kFilePath;
	private String audio8kFilePath;
	private String audio16kFilePath;
	private String imagefilepath;
	
	public static final String AUDIO4K_FILEPATH_KEY = "audio4kFilePath";
	public static final String AUDIO8K_FILEPATH_KEY = "audio8kFilePath";
	public static final String AUDIO16K_FILEPATH_KEY = "audio16kFilePath";
	public static final String IMAGE_FILEPATH_KEY = "imagefilepath";
	
	
	private ConfUtilPerf(){
	}
	
	public static ConfUtilPerf getInstance(){
		if(confUtilPerf == null){
			confUtilPerf = new ConfUtilPerf();
		}
		
		return confUtilPerf;
	}
	
	public void init() throws WesyncExceptionPerf{
		InputStream is = this.getClass().getClassLoader().getResourceAsStream("conf.properties");
		Properties props = new Properties();
		try {
			props.load(is);
			audio4kFilePath = props.getProperty(AUDIO4K_FILEPATH_KEY);
			audio8kFilePath = props.getProperty(AUDIO8K_FILEPATH_KEY);
			audio16kFilePath = props.getProperty(AUDIO16K_FILEPATH_KEY);
			imagefilepath = props.getProperty(IMAGE_FILEPATH_KEY);
		} catch (IOException e) {
			throw new WesyncExceptionPerf("配置文件conf.properties不存在",e);
		} finally{
		    try {
				is.close();
			} catch (IOException e) {
				throw new WesyncExceptionPerf("配置文件关闭错误",e);
			}	    
		}
	}


	public String getAudio4kFilePath() {
		return audio4kFilePath;
	}

	public String getAudio8kFilePath() {
		return audio8kFilePath;
	}

	public String getAudio16kFilePath() {
		return audio16kFilePath;
	}

	public String getImagefilepath() {
		return imagefilepath;
	}
	
}
