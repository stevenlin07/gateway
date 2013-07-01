package com.weibo.wesync.client;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.http.*;
import org.apache.http.client.*;
import org.apache.http.client.fluent.Async;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.methods.*;
import org.apache.http.impl.client.*;
import org.apache.http.impl.conn.*;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.apache.http.client.fluent.Content;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.conn.scheme.Scheme;

import com.weibo.wesync.notify.protocols.Meyou;
import com.weibo.wesync.notify.protocols.MeyouSort;
/**
 * An example that performs GETs from multiple threads.
 *
 */
public class NHttpClient {

    public static void main(String[] args) throws Exception {
    	Meyou.MeyouPacket packet = Meyou.MeyouPacket.newBuilder()
				.setCallbackId("12345abcde")
				.setSort(MeyouSort.notice)
				.build();
    	
    	ExecutorService threadpool = Executors.newFixedThreadPool(2);
    	Async async = Async.newInstance().use(threadpool);
    	Request[] requests = new Request[] {
//    	Request.Post("http://123.125.106.28:8093/wesync?").bodyByteArray(packet.toByteArray()).addHeader("uid", "1010000001"),
//    	Request.Post("http://123.125.106.28:8093/wesync?").bodyByteArray(packet.toByteArray()).addHeader("uid", "1010000001"),
//    	Request.Post("http://123.125.106.28:8093/wesync?").bodyByteArray(packet.toByteArray()).addHeader("uid", "1010000001"),
    	Request.Post("http://123.125.106.28:8093/wesync?").bodyByteArray(packet.toByteArray()).addHeader("uid", "1010000001")
    	};
    	
    	Queue<Future<Content>> queue = new LinkedList<Future<Content>>();
    	
    	for (final Request request: requests) {
    		Future<Content> future = async.execute(request, new FutureCallback<Content>() {
    			public void failed(final Exception ex) {
    				System.out.println(ex.getMessage() + ": " + request);
    			}
    			
    			public void completed(final Content content) {
    				System.out.println("Request completed: " + request);
    			}
    			public void cancelled() {
    			}
    		});
    		System.out.println("sent request and wait for resp");
    		queue.add(future);
    	}
    	// Process the queue
    }

    /**
     * A thread that performs a GET.
     */
    static class GetThread extends Thread {

        private final HttpClient httpClient;
        private final HttpContext context;
        private final HttpGet httpget;
        private final int id;

        public GetThread(HttpClient httpClient, HttpGet httpget, int id) {
            this.httpClient = httpClient;
            this.context = new BasicHttpContext();
            this.httpget = httpget;
            this.id = id;
        }

        /**
         * Executes the GetMethod and prints some status information.
         */
        @Override
        public void run() {

            System.out.println(id + " - about to get something from " + httpget.getURI());

            try {

                // execute the method
                HttpResponse response = httpClient.execute(httpget, context);

                System.out.println(id + " - get executed");
                // get the response body as an array of bytes
                HttpEntity entity = response.getEntity();
                if (entity != null) {
                    byte[] bytes = EntityUtils.toByteArray(entity);
                    System.out.println(id + " - " + bytes.length + " bytes read");
                }

            } catch (Exception e) {
                httpget.abort();
                System.out.println(id + " - error: " + e);
            }
        }

    }

}
