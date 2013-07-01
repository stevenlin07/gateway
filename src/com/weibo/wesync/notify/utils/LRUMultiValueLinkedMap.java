package com.weibo.wesync.notify.utils;

import java.util.ArrayList;
import java.util.Arrays;

import org.apache.commons.collections.map.LinkedMap;

/**
 *  LRU Multivalue HashMap that Supports LRU、Multi Value(Version 1.0)
 *  
 *  Notice: 
 *  1. Not thread safe, used to thread local
 *  2. Value is a ArrayList, the precondition is that 
 *     most of the time is only a value, not multi value。
 *     if the multi value is ordinary state, please instead of arraylist to map 
 *     
 *  Performace:
 *  1. 100000 times : 132ms
 *     
 *  @author : XiaoJun,Hong 2011-5-3 xiaojun2@staff.sina.com.cn
 */
public class LRUMultiValueLinkedMap<K> extends LinkedMap {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6198314616024050812L;
	
	private int maxSize;

	public LRUMultiValueLinkedMap(){
		this(1000);
	}

	public LRUMultiValueLinkedMap(int maxSize){
		this.maxSize = maxSize;
	}
	
	public byte[] putItem(K key, byte[] value) {
		if(size() >= maxSize){
			if (!containsKey(key)) {
                removeLRU();
            }
		}
		
		ArrayList<byte[]> list = getValue(key);
        if (list == null) {
        	list = createCollection();
        }
        else{
	        for(byte[] item : list){
	        	if(Arrays.equals(item, value)){
	        		//repeat
	        		return value;
	        	}
	        }
        }
        
        list.add(value);
        
        super.put(key, list);
        
        return null;
    }
	
	@SuppressWarnings("unchecked")
	public ArrayList<byte[]> getValue(Object key) {
        return (ArrayList<byte[]>)get(key);
    }
	
	protected void removeLRU() {
        Object key = firstKey();

        remove(key);
    }
	
	private ArrayList<byte[]> createCollection(){
		return new ArrayList<byte[]>();
	}
}
