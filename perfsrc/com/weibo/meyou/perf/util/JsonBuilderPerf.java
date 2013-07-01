package com.weibo.meyou.perf.util;

/*
 * @ClassName: JsonBuilder 
 * @Description: 从api-common中拆出来的JsonBuilder工具类
 * @author LiuZhao
 * @date 2012-9-13 下午11:26:21  
 */
public class JsonBuilderPerf{
    
    private StringBuilder sb;

    public JsonBuilderPerf(){
	this.sb = new StringBuilder();
	this.sb.append("{");
    }
    
    public JsonBuilderPerf(String value, boolean isRawData){
      this.sb = new StringBuilder();
      if (isRawData)
        this.sb.append(value);
      else
        this.sb.append("\"").append(toJsonStr(value)).append("\"");
    }
    
    public JsonBuilderPerf append(String name, String value) {
	if (name == null) {
	    return this;
	}
	if (this.sb.length() > 1)
	    this.sb.append(",");
	this.sb.append("\"").append(name).append("\":");
	if (value != null){
	    this.sb.append("\"").append(toJsonStr(value)).append("\"");
	}else {
	    this.sb.append("null");
	}
	return this;
    }

    public JsonBuilderPerf append(String name, boolean value) {
	if (name == null) {
	    return this;
	}
	if (this.sb.length() > 1)
	    this.sb.append(",");
	this.sb.append("\"").append(name).append("\":").append(value);
	return this;
    }

    public JsonBuilderPerf append(String name, int value) {
	if (this.sb.length() > 1)
	    this.sb.append(",");
	this.sb.append("\"").append(name).append("\":").append(value);
	return this;
    }

    public JsonBuilderPerf append(String name, long value) {
	if (this.sb.length() > 1)
	    this.sb.append(",");
	this.sb.append("\"").append(name).append("\":").append(value);
	return this;
    }
    
    public JsonBuilderPerf append(String name, double value) {
	if (this.sb.length() > 1)
	    this.sb.append(",");
	this.sb.append("\"").append(name).append("\":").append(value);
	return this;
    }

    public JsonBuilderPerf append(String name, JsonBuilderPerf value) {
	return appendJsonValue(name, value.toString());
    }

    public JsonBuilderPerf appendJsonValue(String name, String jsonValue){
	if ((name == null) || (jsonValue == null)) {
	    return this;
	}
	if (this.sb.length() > 1)
	    this.sb.append(",");
	this.sb.append("\"").append(name).append("\":").append(jsonValue);
	return this;
    }

    public JsonBuilderPerf flip() {
	this.sb.append('}');
	return this;
    }

    public JsonBuilderPerf reset() {
	this.sb.setLength(0);
	this.sb.append("{");
	return this;
    }

    public String toString(){
	return this.sb.toString();
    }

    public void setLength(int length) {
	this.sb.setLength(length);
    }

    public int length() {
	return this.sb.length();
    }
    
    public static String toJsonStr(String value){
	if (value == null) {
	    return null;
	}
	StringBuilder buf = new StringBuilder(value.length());
	for (int i = 0; i < value.length(); i++) {
	    char c = value.charAt(i);
	    switch (c) {
	    case '"':buf.append("\\\"");break;
	    case '\\':buf.append("\\\\");break;
	    case '\n':buf.append("\\n");break;
	    case '\r':buf.append("\\r");break;
	    case '\t':buf.append("\\t");break;
	    case '\f':buf.append("\\f");break;
	    case '\b':buf.append("\\b");break;
	    default:
		if ((c < ' ') || (c == ''))
		    buf.append(" ");
		else
		    buf.append(c);
		break;
	    }
	}
	return buf.toString();
    }
}
