package com.weibo.wesync.client;

/*
 * ====================================================================
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 *
 */

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.util.concurrent.CountDownLatch;

import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.DefaultConnectionReuseStrategy;
import org.apache.http.impl.nio.DefaultHttpClientIODispatch;
import org.apache.http.impl.nio.pool.BasicNIOConnPool;
import org.apache.http.impl.nio.reactor.DefaultConnectingIOReactor;
import org.apache.http.impl.nio.reactor.IOReactorConfig;
import org.apache.http.message.BasicHttpEntityEnclosingRequest;
import org.apache.http.message.BasicHttpRequest;
import org.apache.http.nio.NHttpClientConnection;
import org.apache.http.nio.protocol.BasicAsyncRequestProducer;
import org.apache.http.nio.protocol.BasicAsyncResponseConsumer;
import org.apache.http.nio.protocol.HttpAsyncRequestExecutor;
import org.apache.http.nio.protocol.HttpAsyncRequester;
import org.apache.http.nio.reactor.ConnectingIOReactor;
import org.apache.http.nio.reactor.IOEventDispatch;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.params.HttpParams;
import org.apache.http.params.SyncBasicHttpParams;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpProcessor;
import org.apache.http.protocol.ImmutableHttpProcessor;
import org.apache.http.protocol.RequestConnControl;
import org.apache.http.protocol.RequestContent;
import org.apache.http.protocol.RequestExpectContinue;
import org.apache.http.protocol.RequestTargetHost;
import org.apache.http.protocol.RequestUserAgent;

import com.weibo.wesync.notify.protocols.Meyou;
import com.weibo.wesync.notify.protocols.MeyouSort;
import com.weibo.wesync.notify.utils.Util;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpState;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;

import java.security.interfaces.RSAPublicKey;
import org.apache.commons.codec.binary.Base64;

/**
 * Asynchronous HTTP/1.1 client.
 */
public class NHttpClient2 {
	private static String username = "wuji28@gmail.com";
	private static String password = "141414";

    public static void main(String[] args) throws Exception {
    	RSAPublicKey publicKey = RSAEncrypt.loadPublicKey("D:\\weibo\\meyou_gw\\conf\\public.pem");
        //加密  
    	byte[] cipher = RSAEncrypt.encrypt(publicKey, password.getBytes());  
    	password = RSAEncrypt.toHexString(cipher);
    	
    	
        // HTTP parameters for the client
        HttpParams params = new SyncBasicHttpParams();
        params
            .setIntParameter(CoreConnectionPNames.SO_TIMEOUT, 30000)
            .setIntParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, 30000)
            .setIntParameter(CoreConnectionPNames.SOCKET_BUFFER_SIZE, 8 * 1024)
            .setBooleanParameter(CoreConnectionPNames.TCP_NODELAY, true);

        // Create HTTP protocol processing chain
        HttpProcessor httpproc = new ImmutableHttpProcessor(new HttpRequestInterceptor[] {
                // Use standard client-side protocol interceptors
                new RequestContent(),
                new RequestTargetHost(),
                new RequestConnControl(),
                new RequestUserAgent(),
                new RequestExpectContinue()});
        // Create client-side HTTP protocol handler
        HttpAsyncRequestExecutor protocolHandler = new HttpAsyncRequestExecutor();
        // Create client-side I/O event dispatch
        final IOEventDispatch ioEventDispatch = new DefaultHttpClientIODispatch(protocolHandler, params);
        // Create client-side I/O reactor
        IOReactorConfig config = new IOReactorConfig();
        config.setIoThreadCount(1);
        final ConnectingIOReactor ioReactor = new DefaultConnectingIOReactor(config);
        // Create HTTP connection pool
        BasicNIOConnPool pool = new BasicNIOConnPool(ioReactor, params);
        // Limit total number of connections to just two
        pool.setDefaultMaxPerRoute(2);
        pool.setMaxTotal(1);
        
        // Run the I/O reactor in a separate thread
        Thread t = new Thread(new Runnable() {

            public void run() {
                try {
                    // Ready to go!
                    ioReactor.execute(ioEventDispatch);
                } catch (InterruptedIOException ex) {
                    System.err.println("Interrupted");
                } catch (IOException e) {
                    System.err.println("I/O error: " + e.getMessage());
                }
                System.out.println("Shutdown");
            }

        });
        // Start the client thread
        t.start();
        // Create HTTP requester
//        HttpAsyncRequester requester = new HttpAsyncRequester(
//                httpproc, new DefaultConnectionReuseStrategy(), params);
        // Execute HTTP GETs to the following hosts and
        HttpHost[] targets = new HttpHost[] {
//        		new HttpHost("123.125.106.28", 8093, "http"),
//        		new HttpHost("123.125.106.28", 8093, "http"),
//                new HttpHost("123.125.106.28", 8093, "http"),
//                new HttpHost("123.125.106.28", 8093, "http"),
//                new HttpHost("123.125.106.28", 8093, "http"),
//                new HttpHost("123.125.106.28", 8093, "http"),
//                new HttpHost("123.125.106.28", 8093, "http"),
//                new HttpHost("123.125.106.28", 8093, "http"),
//                new HttpHost("123.125.106.28", 8093, "http"),
//                new HttpHost("123.125.106.28", 8093, "http"),
//                new HttpHost("123.125.106.28", 8093, "http"),
                new HttpHost("123.125.106.28", 8082, "http")
        };
        
        final CountDownLatch latch = new CountDownLatch(targets.length);
        int callbackId = 0;
        
        for(int i = 0; i < 1; i++) {
        	for (final HttpHost target : targets) {
        		BasicHttpEntityEnclosingRequest request = new BasicHttpEntityEnclosingRequest("POST", "/wesync");
        		
//        		String usrpwd = Base64.encodeBase64String((username + ":" + password).getBytes());
//        		request.setHeader("authorization", "Basic " + usrpwd);
        		request.setHeader("uid", "2565640713");
        		Meyou.MeyouPacket packet = null;
        		
        		if(callbackId == 0) {
        			packet = Meyou.MeyouPacket.newBuilder()
            				.setCallbackId(String.valueOf(callbackId ++))
            				.setSort(MeyouSort.notice)
            				.build();
        		}
        		else {
        			packet = Meyou.MeyouPacket.newBuilder()
            				.setCallbackId(String.valueOf(callbackId ++))
            				.setSort(MeyouSort.wesync)
            				.build();        			
        		}
        		
        		ByteArrayEntity entity = new ByteArrayEntity(packet.toByteArray());
        		request.setEntity(entity);
//        	BasicHttpRequest request = new BasicHttpRequest("GET", "/test.html");
        		
        		System.out.println("send ...");
        		HttpAsyncRequester requester = new HttpAsyncRequester(
                        httpproc, new DefaultConnectionReuseStrategy(), params);
        		requester.execute(
        				new BasicAsyncRequestProducer(target, request),
        				new BasicAsyncResponseConsumer(),
        				pool,
        				new BasicHttpContext(),
        				// Handle HTTP response from a callback
        				new FutureCallback<HttpResponse>() 
        				{
        					public void completed(final HttpResponse response) {
        						StatusLine status = response.getStatusLine();
        						int code = status.getStatusCode();
        						
        						if(code == 200) {
        							try {
        								latch.countDown();
        								DataInputStream in;
        								in = new DataInputStream(response.getEntity().getContent());
        								int packetLength = in.readInt();
        								int start = 0;
        								
        								while(packetLength > 0) {
        									ByteArrayOutputStream outstream = new ByteArrayOutputStream(packetLength);
        									byte[] buffer = new byte[1024];
        									int len = 0;
        									
        									while(start < packetLength && (len = in.read(buffer, start, packetLength)) > 0) {
        										outstream.write(buffer, 0, len);
        										start += len;
        									}
        									
        									Meyou.MeyouPacket packet0 = Meyou.MeyouPacket.parseFrom(outstream.toByteArray());
        									System.out.println(target + "->" + packet0);
        									
        									if((len = in.read(buffer, start, 4)) > 0) {
        										packetLength = Util.readPacketLength(buffer);
        									}
        									else {
        										break;
        									}
        								}
        							} catch (IllegalStateException e) {
        								// TODO Auto-generated catch block
        								e.printStackTrace();
        							} catch (IOException e) {
        								// TODO Auto-generated catch block
        								e.printStackTrace();
        							}
        						}
        						else {
        							System.out.println("error code=" + code +"|"+ status.getReasonPhrase());
        						}
        					}
        					
        					public void failed(final Exception ex) {
        						latch.countDown();
        						System.out.println(target + "->" + ex);
        					}
        					
        					public void cancelled() {
        						latch.countDown();
        						System.out.println(target + " cancelled");
        					}
        					
        				});
        		
        		Thread.sleep((long) (Math.random() * 10000));
        	}
        }
//        latch.await();
//        System.out.println("Shutting down I/O reactor");
//        ioReactor.shutdown();
//        System.out.println("Done");
    }

}

