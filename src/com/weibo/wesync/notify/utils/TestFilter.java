package com.weibo.wesync.notify.utils;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.apache.log4j.Logger;

public class TestFilter implements Filter {
	private static Logger log = Logger.getLogger(TestFilter.class);
	
	@Override
	public void destroy() {
		log.info("TestFilter:destory");
	}

	@Override
	public void doFilter(ServletRequest arg0, ServletResponse arg1,
			FilterChain arg2) throws IOException, ServletException {
		log.info("TestFilter:doFilter");
	}

	@Override
	public void init(FilterConfig arg0) throws ServletException {
		log.info("TestFilter:init");		
	}

}
