package com.weibo.wesync.notify.utils;

import java.text.DecimalFormat;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.log4j.Logger;

public class StatLog implements Runnable {
	private static Logger log = Logger.getLogger("debug_stat");
	
	private static AtomicLong count = new AtomicLong(0);
	private static AtomicLong errorCount = new AtomicLong(0);
	private static volatile Map<String, AtomicLong> statVars = new Hashtable<String, AtomicLong>();
	private static Map<String, AtomicLong> lastStatVars = new Hashtable<String, AtomicLong>();
	private static Map<String, AtomicLong> maxStatVars = new Hashtable<String, AtomicLong>();
	private static AtomicBoolean outOfMemory = new AtomicBoolean(false);
	private static volatile AtomicLong lastModifyTime = null; 
	
	private static AtomicBoolean pausePrint = new AtomicBoolean(false);
		
	static {
		printStat(5000);
	}
	public static void setPausePrint(boolean print) {
		pausePrint.set(print);
	}
	
	public static long inc() {
		return count.incrementAndGet();
	}

	public static long get() {
		return count.get();
	}
	
	public static long dec() {
		return count.decrementAndGet();
	}
	
	public static void registerVar(String var) {
		statVars.put(var, new AtomicLong(0));
		lastStatVars.put(var, new AtomicLong(0));
		maxStatVars.put(var, new AtomicLong(0));
	}

	public static long inc(String var) {
		AtomicLong c = statVars.get(var);
		long r = 0;
		if (c != null)
			r = c.incrementAndGet();
		else
			statVars.put(var, new AtomicLong(1));
			
		if (r < 0) {
			r = 0;
			c.set(0);
		}
		return r;
	}

	public static long inc(String var, int value) {
		AtomicLong c = statVars.get(var);
		long r = 0;
		if (c != null)
			r = c.addAndGet(value);
		else
			statVars.put(var, new AtomicLong(value));

		if (r < 0) {
			r = 0;
			c.set(0);
		}
		return r;
	}
	
	public static long dec(String var) {
		AtomicLong c = statVars.get(var);
		if (c != null)
			return c.decrementAndGet();
		else
			return 0;
	}
	
	public static long inc(int delta) {
		return count.addAndGet(delta);
	}

	public static long incError() {
		return errorCount.incrementAndGet();
	}

	public static long decError() {
		return errorCount.decrementAndGet();
	}

	public static long getError() {
		return errorCount.get();
	}
	
	
	public static long incError(int delta) {
		return errorCount.addAndGet(delta);
	}
	
	private static long startTime;
	private static long interval;
	
	public StatLog(long startTime2, long interval2) {
		startTime = startTime2;
		interval = interval2;
	}
	
	public static void resetStartTime(long newTime) {
		startTime = newTime;
	}
	
	/**
	 * print stat info on the screen, this method will block until total is reached,
	 * @param total, -1 for infinity
	 * @param interval how long (second) to print a stat log
	 */
	public static StatLog printStat(long interval) {
		log.info("Start IM Server stat log.");
		StatLog t = new StatLog(System.currentTimeMillis(), interval);
		new Thread(t).start();
		return t;
	}
	
	public void run() {
		try {
			Thread.sleep(20000);			
		} catch (Exception e) {
		}
		
		long lastCount = 0;
		long cnt = 0;
		long lastTime = 0;
		long max = 0;
		while (true) {
			// 定时加载log4j配置文件，方便测试时修改配置
			reloadLog4jProperties();
			
			// FIXME
			int newInterval = 0;
			if (newInterval > 0) {
				long newTime = newInterval * 1000;
				if (newTime != interval && newTime < 600000) {
					log.info("Change log interval to " + newTime);
					interval = newTime;
				}
			}
			try {
				synchronized (this) {
					wait(interval);
				}
				// Thread.sleep(interval);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			
			if (pausePrint.get())
				continue;
			
			long time2 = System.currentTimeMillis();
			if (time2 == 0)
				break;
			if (time2 == startTime)
				continue;
			cnt = count.get();
			long cur = (cnt - lastCount) * 1000l / (time2 - lastTime);
			if (cur > max)  max = cur;
			
			log.info("---------------------------");
			log.info("JAVA HEAP: " + memoryReport() + ", UP TIME: " + ((time2 - startTime) / 1000) + ", min: " + ((time2 - startTime) / 60000));
			SortedSet<String> keys = new TreeSet<String>(statVars.keySet());
			StringBuilder sb = new StringBuilder("[");
			boolean firstLoop = true;
			for (Iterator<String> iterator = keys.iterator(); iterator.hasNext();) {
				String var = iterator.next();
				AtomicLong c = statVars.get(var);
				AtomicLong last1 = lastStatVars.get(var);
				AtomicLong m1 = maxStatVars.get(var);
				if (m1 == null)
					continue;
				
				long cnt1 = c.get();
				if (cnt1 == 0)
					continue;
				long max1 = m1.get();
				long lastCount1 = last1.get();
				
				long avg1 = cnt1 * 1000l / (time2 - startTime);
				long cur1 = (cnt1 - lastCount1) * 1000l / (time2 - lastTime);
				if (cur1 > max1)  max1 = (int) cur1;

				if (!firstLoop)
					sb.append(",");
				else
					firstLoop = false;

				// json-style output
				sb.append("{\"").append(var).append("\":[").append(cnt1).append(",")
					.append(avg1).append(",").append(cur1).append(",").append(max1).append("]}");
				
				m1.set(max1);
				last1.set(cnt1);
			}
			sb.append("]");
			log.info(sb.toString());
			
			lastTime = time2;
			lastCount = cnt;
		}
		
		log.info("Stat log stop");
	}
	
	public static String memoryReport() {
		Runtime runtime = Runtime.getRuntime();

		double freeMemory = (double) runtime.freeMemory() / (1024 * 1024);
		double maxMemory = (double) runtime.maxMemory() / (1024 * 1024);
		double totalMemory = (double) runtime.totalMemory() / (1024 * 1024);
		double usedMemory = totalMemory - freeMemory;
		double percentFree = ((maxMemory - usedMemory) / maxMemory) * 100.0;
		if (percentFree < 10) {
			outOfMemory.set(true);
			log.error("Detected OutOfMemory potentia memory > 90%, stop broadcast presence !!!!!!");
			
		} else if (outOfMemory.get() == true && percentFree > 20) {
			outOfMemory.set(false);
			log.error("Detected memory return to normal, memory < 80%, resume broadcast presence.");
		}
		double percentUsed = 100 - percentFree;
		// int percent = 100 - (int) Math.round(percentFree);

		DecimalFormat mbFormat = new DecimalFormat("#0.00");
        DecimalFormat percentFormat = new DecimalFormat("#0.0");
		
        StringBuilder sb = new StringBuilder(" ");
		sb.append(mbFormat.format(usedMemory)).append("MB of ").append(mbFormat.format(maxMemory))
		.append(" MB (").append(percentFormat.format(percentUsed)).append("%) used");
		return sb.toString();
	}

	public static boolean isOutOfMemory() {
		return outOfMemory.get();
	}
	
	private void reloadLog4jProperties() {
		// TO DO
	}

	public static long get(String var) {
		AtomicLong value = statVars.get(var);
		return (value == null ? 0 : value.get());
	}
}

