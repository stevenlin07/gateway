/*   
 * @Title: MessageBlockingQueue.java 
 * @Package com.weibo.meyou.sdk.store 
 * @Description: TODO(用一句话描述该文件做什么) 
 * @author LiuZhao  
 * @date 2012-9-9 下午4:36:30 
 * @version V1.0   
 */
package com.weibo.meyou.perf.util;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;



/*
 * @ClassName: MessageBlockingQueue 
 * @Description: TODO(这里用一句话描述这个类的作用) 
 * @author LiuZhao
 * @date 2012-9-9 下午4:36:30  
 */
public class MessageBlockingQueuePerf implements java.io.Serializable{
    
    /*
     * @Fields serialVersionUID : TODO(用一句话描述这个变量表示什么) 
     */ 
    private static final long serialVersionUID = 1L;
    private Queue<MessageEntityPerf> fileQueue = new LinkedList<MessageEntityPerf>();
    private Queue<MessageEntityPerf> messageQueue = new LinkedList<MessageEntityPerf>();
    
    // 用于读取的独占锁
    private final ReentrantLock takeLoke = new ReentrantLock();
    // 用于写入的独占锁
    private final ReentrantLock putLock = new ReentrantLock();
    
    private final Condition notEmpty = takeLoke.newCondition();
    private final AtomicInteger count = new AtomicInteger(0);

    public void putMessage(MessageEntityPerf messageEntity) throws InterruptedException{
	final AtomicInteger count = this.count;	
	final ReentrantLock putLock = this.putLock;
	putLock.lockInterruptibly();
	messageQueue.add(messageEntity);
	count.getAndIncrement();
	putLock.unlock();
	singalNotEmpty();
    }
    
    public void putFileBlock(MessageEntityPerf messageEntity) throws InterruptedException {
	if(messageEntity == null) throw new NullPointerException();
	final AtomicInteger count = this.count;
	final ReentrantLock putLock = this.putLock;
	putLock.lockInterruptibly();
	fileQueue.add(messageEntity);
	count.getAndIncrement();
	putLock.unlock();
	singalNotEmpty();
    }
    
    private void singalNotEmpty(){
	final ReentrantLock takeLoke = this.takeLoke;
	takeLoke.lock();
	try{
	    notEmpty.signal();
	}finally{
	    takeLoke.unlock(); 
	}
    }

    public MessageEntityPerf getMessageEntity() throws InterruptedException{
	MessageEntityPerf messageEntity;
	int c = -1;
	final AtomicInteger count = this.count;
	final ReentrantLock takeLoke = this.takeLoke;
	takeLoke.lockInterruptibly();
	try{
	    while(count.get() == 0){
		notEmpty.await();
	    }
	    messageEntity = messageQueue.poll();
	    if(null == messageEntity){
		messageEntity = fileQueue.poll();
	    }
	    c = count.getAndDecrement();
	    if(c > 1){
		notEmpty.signal();
	    }
	}finally{
	    takeLoke.unlock();
	}
	return messageEntity;
    }
    
    public void clear(){
	fileQueue.clear();
	messageQueue.clear();
    }
}