package com.weibo.wesync.notify.service.app;

import org.apache.commons.httpclient.params.HttpMethodParams;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.commons.httpclient.methods.multipart.MultipartRequestEntity;
import org.apache.commons.httpclient.methods.multipart.Part;
import org.apache.commons.httpclient.methods.multipart.StringPart;
import org.apache.commons.httpclient.params.HttpConnectionManagerParams;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.weibo.wesync.notify.utils.Util;

import cn.sina.api.commons.util.ByteArrayPart;

/**
 * 
 * @auth jichao1@staff.sina.com.cn
 */
public class AppUtil {
	protected static final Log log = LogFactory.getLog(AppUtil.class);
	private static ExecutorService apiExecutor = Executors
			.newFixedThreadPool(32);
	private static MultiThreadedHttpConnectionManager connectionManager = new MultiThreadedHttpConnectionManager();
	private static HttpClient httpClient = null;
	private static AppUtil instance;

	public static AppUtil getInstance() {
		if (instance == null) {
			synchronized (AppUtil.class) {
				if (instance == null) {
					instance = new AppUtil();
				}
			}
		}
		return instance;
	}

	public String requestGetUrl(String url, Map<String, String> headers) {
		return requestGetUrl(url, headers, null);
	}

	public String requestGetUrl(String url, Map<String, String> headers,
			Map<String, String> params) {
		FutureTask<String> requestGetUrlTask = new FutureTask<String>(
				new RequestGetUrlTask(url, headers, params));
		apiExecutor.execute(requestGetUrlTask);
		String result = null;
		try {
			result = requestGetUrlTask.get(Integer.valueOf(Util.getConfigProp(
					"request_group_urltask_timeout", "500")),
					TimeUnit.MILLISECONDS);
			return result;
		} catch (Exception e) {
			log.debug("requestGetUrl [" + url + ", " + getParamToStr(headers)
					+ ", " + getParamToStr(params) + "] failed caused by "
					+ e.getMessage());
			return null;
		}
	}

	private String getParamToStr(Map<String, String> params) {
		if (params == null || params.isEmpty()) {
			return null;
		}
		StringBuilder sb = new StringBuilder("{");
		for (Map.Entry<String, String> entry : params.entrySet()) {
			sb.append("[key=").append(entry.getKey()).append(",val=")
					.append(entry.getValue()).append("],");
		}

		sb.append("}");
		return sb.toString();
	}

	private static String requestGetUrl0(String url,
			Map<String, String> headers, Map<String, String> params) {
		HttpClient httpClient = getHttpClient();
		GetMethod getMethod = new GetMethod(url);

		if (headers != null && !headers.isEmpty()) {
			for (Map.Entry<String, String> entry : headers.entrySet()) {
				getMethod.setRequestHeader(entry.getKey(), entry.getValue());
			}
		}

		addQueryStrings(getMethod, params);

		String result = null;

		try {
			int code = httpClient.executeMethod(getMethod);
			result = extractResponseBody(getMethod);
			if (code != 200) {
				log.warn("GroupApi request url failed caused [code: " + code
						+ "] by " + result);
			}
		} catch (Exception e) {
			log.error(
					"Error: when getReponseBody from url:"
							+ getMethod.getPath(), e);
		} finally {
			getMethod.releaseConnection();
		}

		return result;
	}

	public String requestPostUrl(String url, Map<String, String> headers,
			Map<String, String> params) {
		FutureTask<String> requestPostUrlTask = new FutureTask<String>(
				new RequestPostUrlTask(url, headers, params));
		apiExecutor.execute(requestPostUrlTask);
		String result = null;

		try {
			int timeout = url.indexOf("login") >= 0 ? Integer.parseInt(Util
					.getConfigProp("login_req_timeout", "1500")) : Integer
					.parseInt(Util.getConfigProp("http_req_timeout", "500"));
			result = requestPostUrlTask.get(timeout, TimeUnit.MILLISECONDS);
			return result;
		} catch (Exception e) {
			log.warn(
					"requestPostUrl " + url + ", " + getParamToStr(headers)
							+ ", " + getParamToStr(params)
							+ " failed caused by " + e.getMessage(), e);
			return null;
		}
	}

	public String postMultipartUrl(String url, Map<String, String> headers,
			Map<String, Object> nameValues) {
		FutureTask<String> postMultipartUrlTask = new FutureTask<String>(
				new PostMultipartUrlTask(url, headers, nameValues));
		apiExecutor.execute(postMultipartUrlTask);
		String result = null;

		try {
			result = postMultipartUrlTask.get(Integer.parseInt(Util
					.getConfigProp("http_req_timeout", "5000")),
					TimeUnit.MILLISECONDS);
			return result;
		} catch (Exception e) {
			log.warn(
					"postMultipartUrlTask " + url + " failed caused by "
							+ e.getMessage(), e);
			return null;
		}
	}

	private String requestPostUrl0(String url, Map<String, String> headers,
			Map<String, String> params) {
		HttpClient httpClient = getHttpClient();
		PostMethod post = new PostMethod(url);

		if (params != null && !params.isEmpty()) {
			List<NameValuePair> list = new ArrayList<NameValuePair>(
					params.size());

			for (Map.Entry<String, String> entry : params.entrySet()) {
				if (entry.getKey() != null && !entry.getKey().isEmpty()) {
					if ("requestString".equals(entry.getKey())) {
						try {
							post.setRequestEntity(new StringRequestEntity(entry
									.getValue(),
									"application/x-www-form-urlencoded",
									"utf-8"));
						} catch (UnsupportedEncodingException e) {
							log.warn("requestPostUrl0, post.setRequestEntity failed caused by "
									+ e.getMessage());
						}
					} else {
						list.add(new NameValuePair(entry.getKey(), entry
								.getValue()));
					}
				} else {
					try {
						post.setRequestEntity(new StringRequestEntity(entry
								.getValue(), "text/xml", "utf-8"));
					} catch (UnsupportedEncodingException e) {
						log.warn("requestPostUrl0, post.setRequestEntity failed caused by "
								+ e.getMessage());
					}
				}
			}
			if (!list.isEmpty())
				post.setRequestBody(list.toArray(new NameValuePair[list.size()]));
		}

		if (headers != null && !headers.isEmpty()) {
			for (Map.Entry<String, String> entry : headers.entrySet()) {
				post.setRequestHeader(entry.getKey(), entry.getValue());
			}
		}

		String result = null;
		try {
			HttpMethodParams param = post.getParams();
			param.setContentCharset("UTF-8");
			long t1 = System.currentTimeMillis();
			int code = httpClient.executeMethod(post);
			long t2 = System.currentTimeMillis();

			if (t2 - t1 > Integer.parseInt(Util.getConfigProp("http_req_slow",
					"200"))) {
				log.warn("http req " + url + " is slow using " + (t2 - t1));
			}

			result = extractResponseBody(post);

			if (code != 200) {
				log.warn("request url [" + url + "] failed caused [code: "
						+ code + "] by " + result);
			}
		} catch (Exception e) {
			log.error("Error: when getReponseBody from url:" + post.getPath(),
					e);
		} finally {
			post.releaseConnection();
		}

		return result;
	}

	private String postMultipartUrl0(String url, Map<String, String> headers,
			Map<String, Object> nameValues) {
		HttpClient httpClient = getHttpClient();
		PostMethod post = new PostMethod(url);

		if (headers != null && !headers.isEmpty()) {
			for (Map.Entry<String, String> entry : headers.entrySet()) {
				post.setRequestHeader(entry.getKey(), entry.getValue());
			}
		}

		Part[] parts = null;
		if (nameValues != null && !nameValues.isEmpty()) {
			parts = new Part[nameValues.size()];
			int i = 0;
			for (Map.Entry<String, Object> entry : nameValues.entrySet()) {
				if (entry.getValue() instanceof ByteArrayPart) {
					ByteArrayPart data = (ByteArrayPart) entry.getValue();
					parts[i++] = data;
				} else {
					parts[i++] = new StringPart(entry.getKey(), entry
							.getValue().toString(), "utf-8");
				}
			}
		}
		post.setRequestEntity(new MultipartRequestEntity(parts, post
				.getParams()));

		String result = null;
		try {
			int code = httpClient.executeMethod(post);
			result = extractResponseBody(post);
			if (code != 200) {
				log.warn("request url [" + url + "] failed caused [code: "
						+ code + "] by " + result);
			}
		} catch (Exception e) {
			log.error("Error: when getReponseBody from url:" + post.getPath(),
					e);
		} finally {
			post.releaseConnection();
		}

		return result;
	}

	private static void addQueryStrings(HttpMethod method,
			Map<String, String> queryStrs) {
		if (queryStrs != null && !queryStrs.isEmpty()) {
			NameValuePair[] querys = new NameValuePair[queryStrs.size()];

			int i = 0;
			for (Map.Entry<String, String> entry : queryStrs.entrySet()) {
				querys[i++] = new NameValuePair(entry.getKey(),
						entry.getValue());
			}

			method.setQueryString(querys);
		}
	}

	private static String extractResponseBody(HttpMethod httpMethod)
			throws IOException {
		InputStream instream = httpMethod.getResponseBodyAsStream();
		if (instream != null) {
			int contentLength = getResponseContentLength(httpMethod);
			ByteArrayOutputStream outstream = contentLength < 0 ? new ByteArrayOutputStream()
					: new ByteArrayOutputStream(contentLength);
			byte[] buffer = new byte[1024];
			int len = 0;
			while ((len = instream.read(buffer)) > 0) {
				outstream.write(buffer, 0, len);
			}
			String content = new String(outstream.toByteArray(), "utf-8");
			outstream.close();
			return content;
		}
		return null;
	}

	private static int getResponseContentLength(HttpMethod httpMethod) {
		Header[] headers = httpMethod.getResponseHeaders("Content-Length");
		if (headers.length == 0) {
			return -1;
		}
		if (headers.length > 1) {
			log.info("Multiple content-length headers detected" + ",url:"
					+ httpMethod.getPath());
		}

		for (int i = headers.length - 1; i >= 0; i--) {
			Header header = headers[i];
			try {
				return Integer.parseInt(header.getValue());
			} catch (NumberFormatException e) {
				log.error("Invalid content-length value:" + e.getMessage()
						+ ",url:" + httpMethod.getPath());
			}
		}

		return -1;
	}

	private class RequestPostUrlTask implements Callable<String> {
		String url;
		Map<String, String> headers;
		Map<String, String> params;

		public RequestPostUrlTask(String url, Map<String, String> headers,
				Map<String, String> params) {
			this.url = url;
			this.headers = headers;
			this.params = params;
		}

		public String call() {
			return requestPostUrl0(url, headers, params);
		}
	}

	private class PostMultipartUrlTask implements Callable<String> {
		String url;
		Map<String, String> headers;
		Map<String, Object> nameValues;

		public PostMultipartUrlTask(String url, Map<String, String> headers,
				Map<String, Object> nameValues) {
			this.url = url;
			this.headers = headers;
			this.nameValues = nameValues;
		}

		public String call() {
			return postMultipartUrl0(url, headers, nameValues);
		}
	}

	private class RequestGetUrlTask implements Callable<String> {
		String url;
		Map<String, String> headers;
		Map<String, String> params;

		public RequestGetUrlTask(String url, Map<String, String> headers,
				Map<String, String> params) {
			this.url = url;
			this.headers = headers;
			this.params = params;
		}

		public String call() {
			return requestGetUrl0(url, headers, params);
		}
	}

	public static synchronized HttpClient getHttpClient() {
		if (httpClient == null) {
			HttpConnectionManagerParams params = connectionManager.getParams();
			params.setDefaultMaxConnectionsPerHost(Integer.parseInt(Util
					.getConfigProp("httpclient_max_conn_per_host", "300")));
			params.setConnectionTimeout(Integer.parseInt(Util.getConfigProp(
					"httpclient_conn_timeout", "2000")));
			params.setSoTimeout(Integer.parseInt(Util.getConfigProp(
					"httpclient_so_timeout", "2000")));
			httpClient = new HttpClient(connectionManager);
			if (log.isDebugEnabled()) {
				log.debug("httpClient init....");
			}
		}
		return httpClient;
	}
}
