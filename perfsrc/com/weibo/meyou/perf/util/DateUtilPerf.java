package com.weibo.meyou.perf.util;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DateUtilPerf {

	@SuppressWarnings("unchecked")
	private static ThreadLocal<DateFormat> formatter = new ThreadLocal() {
		protected DateFormat initialValue() {
			return new SimpleDateFormat("E MMM dd HH:mm:ss Z yyyy",
					Locale.ENGLISH);
		}
	};

	@SuppressWarnings("unchecked")
	private static ThreadLocal<DateFormat> dateTimeSdf = new ThreadLocal() {
		protected DateFormat initialValue() {
			return new SimpleDateFormat("MM-dd HH:mm:ss:SSS");
		}
	};

	public static String formatDate(Date date, String defaultValue) {
		if (date == null)
			return defaultValue;
		try {
			return ((DateFormat) formatter.get()).format(date);
		} catch (RuntimeException e) {
		}

		return null;
	}

	public static Date parseDate(String dateStr, Date defaultValue) {
		if (dateStr == null)
			return defaultValue;
		try {
			return ((DateFormat) formatter.get()).parse(dateStr);
		} catch (ParseException e) {
		}
		return defaultValue;
	}

	public static String formateDateTime(Date date) {
		return ((DateFormat) dateTimeSdf.get()).format(date);
	}
	
	public static Date parseDateTime(String timeStr, Date defaultValue) {
		if (timeStr == null)
			return defaultValue;
		try {
			return ((DateFormat) dateTimeSdf.get()).parse(timeStr);
		} catch (ParseException e) {
		}
		return defaultValue;
	}
	
	public static void main (String[] args){
		Long time = System.currentTimeMillis();
		
		Date date = new Date(time);
		System.out.println("Current time is: "+DateUtilPerf.formatDate(date, ""));
	}

}