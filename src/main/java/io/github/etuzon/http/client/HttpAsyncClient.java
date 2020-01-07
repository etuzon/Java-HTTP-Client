package io.github.etuzon.http.client;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.CharBuffer;
import java.nio.charset.UnsupportedCharsetException;
import java.util.ArrayList;
import java.util.List;

import org.apache.hc.client5.http.async.methods.AbstractCharResponseConsumer;
import org.apache.hc.client5.http.async.methods.AsyncRequestBuilder;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.client5.http.impl.async.CloseableHttpAsyncClient;
import org.apache.hc.client5.http.impl.async.HttpAsyncClients;
import org.apache.hc.core5.concurrent.FutureCallback;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.http.nio.AsyncRequestProducer;
import org.apache.hc.core5.util.TimeValue;

import io.github.etuzon.http.exceptions.HttpException;
import io.github.etuzon.http.exceptions.InvalidHttpRequestException;
import io.github.etuzon.http.factory.RequestBuilder;
import io.github.etuzon.projects.core.utils.DateUtil;
import io.github.etuzon.projects.core.utils.ListUtil;
import io.github.etuzon.projects.core.utils.StringUtil;

/************************************************************
 * Async HTTP client.
 * 
 * @author Eyal Tuzon
 *
 */
public class HttpAsyncClient extends Thread {
	private enum ServerResponsContainsBodyEnum {
		FALSE, STARTED, ALREADY_CONTAINS;
	}

	public static final int BUFFER_SIZE = 1024 * 1024;

	public static final boolean IS_DEBUG = false;

	public static final String SEPARATOR_BETWEEN_HEADERS_TO_BODY = "\r\n\r\n";

	private final List<HttpResponse> httpResponseList = new ArrayList<HttpResponse>();
	private final AbstractCharResponseConsumer<HttpResponse> consumer = initAsyncCharConsumer();
	private final FutureCallback<HttpResponse> futureCallback = initFutureCallback();
	private final AsyncRequestProducer producer;
	private final HttpUriRequestBase httpRequest;
	private CloseableHttpAsyncClient closeableAsyncHttpClient = null;
	private Integer responseStatusCode = null;
	private List<Header> responseHeaderList = new ArrayList<Header>();
	private StringBuffer currentResponseBodyBuffer = new StringBuffer();

	private Exception exception = null;
	private boolean isRunning = true;
	private boolean isDebug = IS_DEBUG;
	private CharsPerSecondCounter charsPerSecond = new CharsPerSecondCounter();

	/************************************************************
	 * Constructor.
	 * 
	 * @param requestBuilder HTTP request.
	 * @throws InvalidHttpRequestException in case HTTP request is invalid.
	 * @throws HttpException               in case failed to init HTTP client.
	 */
	public HttpAsyncClient(RequestBuilder requestBuilder) throws InvalidHttpRequestException, HttpException {
		this(requestBuilder, IS_DEBUG);
	}

	/************************************************************
	 * Constructor.
	 * 
	 * @param requestBuilder HTTP request.
	 * @param isDebug        true in case print debug.
	 * @throws InvalidHttpRequestException in case HTTP request is invalid.
	 * @throws HttpException               in case failed to init HTTP client.
	 */
	public HttpAsyncClient(RequestBuilder requestBuilder, boolean isDebug)
			throws InvalidHttpRequestException, HttpException {
		this.httpRequest = requestBuilder.build();
		this.isDebug = isDebug;
		closeableAsyncHttpClient = HttpAsyncClients.createDefault();
		producer = buildAsyncHttpRequestProducer();
	}

	/************************************************************
	 * Thread run method.
	 * 
	 */
	public void run() {
		printDebug("DEBUG is true");
		closeableAsyncHttpClient.start();

		try {
			closeableAsyncHttpClient.execute(producer, consumer, futureCallback);

			try {
				closeableAsyncHttpClient.awaitShutdown(TimeValue.MAX_VALUE);
			} catch (InterruptedException e) {
			}
		} finally {
			isRunning = false;
			closeHttpConnection();
		}
	}

	/************************************************************
	 * Get response status code.
	 * 
	 * @return response status code. Return -1 in case there is no response.
	 */
	public int getStatusCode() {
		if (responseStatusCode != null) {
			return responseStatusCode.intValue();
		}

		if (httpResponseList.size() > 0) {
			return httpResponseList.get(0).getCode();
		}

		return -1;
	}

	/************************************************************
	 * Shutdown client.
	 * 
	 */
	public void shutdown() {
		isRunning = false;
	}

	/************************************************************
	 * Get response list.
	 * 
	 * @return response list.
	 */
	public List<HttpResponse> getHttpResponseList() {
		return httpResponseList;
	}

	/************************************************************
	 * Get response body buffer.
	 * 
	 * @return response body buffer.
	 */
	public String getResponseBuffer() {
		return getResponseBuffer(false);
	}

	/************************************************************
	 * Get response body buffer.
	 * 
	 * @param istruncate true in case truncate the buffer after return it.
	 * @return response body buffer.
	 */
	public String getResponseBuffer(boolean istruncate) {
		if (istruncate == false) {
			return currentResponseBodyBuffer.toString();
		}

		synchronized (currentResponseBodyBuffer) {
			String currentBufferStr = currentResponseBodyBuffer.toString();
			currentResponseBodyBuffer.delete(0, currentBufferStr.length());
			return currentBufferStr;
		}
	}

	/************************************************************
	 * Get response body buffer length.
	 * 
	 * @return response body buffer length.
	 */
	public int getResponseBufferLength() {
		return currentResponseBodyBuffer.length();
	}

	/************************************************************
	 * Return thread status.
	 * 
	 * @return true in case thread still running, else return false.
	 */
	public boolean isRunning() {
		return isRunning;

	}

	/************************************************************
	 * Return Exception object in case expection happened.
	 * 
	 * @return Exception object in case exception happened.
	 */
	public Exception getException() {
		return exception;
	}

	/************************************************************
	 * Return true in case exception happened when running HTTP client thread.
	 * 
	 * @return true in case exception happened when running HTTP client thread.
	 */
	public boolean isException() {
		return exception != null;
	}

	/************************************************************
	 * Return response chars per seconds where segment size is 1 second.
	 * 
	 * @return response chars per seconds.
	 */
	public int getCharsPerSecond_segment_1_second() {
		return charsPerSecond.getSegment_1_second();
	}

	/************************************************************
	 * Return response chars per seconds where segment size is 5 seconds.
	 * 
	 * Example: second 1 response body chars: 10 second 2 response body chars: 0
	 * second 3 response body chars: 0 second 4 response body chars: 0 second 5
	 * response body chars: 0
	 * 
	 * Result will be 10/5 = 2.
	 * 
	 * @return response chars per seconds.
	 */
	public int getCharsPerSecond_segment_5_seconds() {
		return charsPerSecond.getSegment_5_seconds();
	}

	/************************************************************
	 * Return response chars per seconds where segment size is 30 seconds.
	 * 
	 * Example: second 1 response body chars: 60 second 2-30 response body chars: 0
	 * 
	 * Result will be 60/30 = 2.
	 * 
	 * @return response chars per seconds.
	 */
	public int getCharsPerSecond_segment_30_seconds() {
		return charsPerSecond.getSegment_30_seconds();
	}

	/************************************************************
	 * Return response chars per seconds where segment size is 1 minute.
	 * 
	 * Example: second 1 response body chars: 60 second 2-60 response body chars: 0
	 * 
	 * Result will be 60/60 = 1.
	 * 
	 * @return response chars per seconds.
	 */
	public int getCharsPerSecond_segment_1_minute() {
		return charsPerSecond.getSegment_1_minute();
	}

	private void closeHttpConnection() {
		try {
			if (httpRequest != null) {
				httpRequest.abort();
			}
		} catch (Exception e) {
		}
	}

	private AbstractCharResponseConsumer<HttpResponse> initAsyncCharConsumer() {
		return new AbstractCharResponseConsumer<HttpResponse>() {
			private HttpResponse response;
			private ServerResponsContainsBodyEnum serverContainsBodyState = ServerResponsContainsBodyEnum.FALSE;

			private String tempBufferUntilResponseBodyStartedReceived = "";

			@Override
			protected void start(final HttpResponse response, final ContentType contentType) {
				this.response = response;

				if (response != null) {
					responseStatusCode = response.getCode();

					printDebug("Response Status Code: " + responseStatusCode);
					synchronized (response) {
						synchronized (responseHeaderList) {
							responseHeaderList = ListUtil.asList(response.getHeaders());
						}

						printDebugResponseHeaderList();
					}
				} else {
					printDebug("response is null");
				}
			}

			@Override
			protected int capacityIncrement() {
				return Integer.MAX_VALUE;
			}

			@Override
			protected void data(final CharBuffer data, final boolean endOfStream) throws IOException {
				synchronized (currentResponseBodyBuffer) {
					String buf = data.toString();

					if (buf.isEmpty() == false) {
						tempBufferUntilResponseBodyStartedReceived += buf;

						if (isResponseStartedToContainBody()) {
							buf = tempBufferUntilResponseBodyStartedReceived
									.substring(tempBufferUntilResponseBodyStartedReceived
											.indexOf(SEPARATOR_BETWEEN_HEADERS_TO_BODY)
											+ SEPARATOR_BETWEEN_HEADERS_TO_BODY.length());

							tempBufferUntilResponseBodyStartedReceived = null;
							serverContainsBodyState = ServerResponsContainsBodyEnum.STARTED;
						} else if (serverContainsBodyState == ServerResponsContainsBodyEnum.STARTED) {
							serverContainsBodyState = ServerResponsContainsBodyEnum.ALREADY_CONTAINS;
						}

						updateResponseBuffer(buf);
					}
				}
			}

			@Override
			protected HttpResponse buildResult() throws IOException {
				return response;
			}

			@Override
			public HttpResponse getResult() {
				return response;
			}

			@Override
			public void releaseResources() {
			}

			private boolean isResponseStartedToContainBody() {
				synchronized (serverContainsBodyState) {
					if (serverContainsBodyState == ServerResponsContainsBodyEnum.FALSE) {
						if (tempBufferUntilResponseBodyStartedReceived.contains(SEPARATOR_BETWEEN_HEADERS_TO_BODY)) {
							return true;
						}
					}
				}

				return false;
			}

			private void updateResponseBuffer(String buf) {
				synchronized (serverContainsBodyState) {
					if (serverContainsBodyState != ServerResponsContainsBodyEnum.FALSE) {
						currentResponseBodyBuffer.append(buf);
						printDebug("Response buffer: " + buf);
						charsPerSecond.updateCharsAmount(buf.length());
					}
				}
			}

			private void printDebugResponseHeaderList() {
				if (isDebug) {
					StringBuffer headersStrBuf = new StringBuffer();

					for (Header header : responseHeaderList) {
						headersStrBuf.append("     ").append(header.getName()).append(": ").append(header.getValue())
								.append("\n");
					}

					if (headersStrBuf.length() > 0) {
						printDebug("Response headers:\n" + headersStrBuf.toString());
					} else {
						printDebug("No headers were found in the response");
					}
				}
			}
		};
	}

	private FutureCallback<HttpResponse> initFutureCallback() {
		return new FutureCallback<HttpResponse>() {

			@Override
			public void completed(final HttpResponse response) {
			}

			@Override
			public void failed(final Exception ex) {
				exception = ex;
			}

			@Override
			public void cancelled() {
			}
		};
	}

	private AsyncRequestProducer buildAsyncHttpRequestProducer() throws HttpException {
		AsyncRequestBuilder requestBuilder = AsyncRequestBuilder.create(httpRequest.getMethod());

		requestBuilder = setUri(requestBuilder);
		requestBuilder = addHeaders(requestBuilder);

		if (httpRequest.getEntity() != null) {
			HttpEntity entity = httpRequest.getEntity();
			requestBuilder = setEntity(requestBuilder, entity);
		}

		return requestBuilder.build();
	}

	private AsyncRequestBuilder setUri(AsyncRequestBuilder requestBuilder) throws HttpException {
		try {
			return requestBuilder.setUri(httpRequest.getUri());
		} catch (URISyntaxException e1) {
			throw new HttpException(e1);
		}
	}

	private AsyncRequestBuilder addHeaders(AsyncRequestBuilder requestBuilder) {
		for (Header header : httpRequest.getHeaders()) {
			requestBuilder.addHeader(header);
		}

		return requestBuilder;
	}

	private AsyncRequestBuilder setEntity(AsyncRequestBuilder requestBuilder, HttpEntity entity) throws HttpException {
		try {
			return requestBuilder.setEntity(StringUtil.readFromInputStream(entity.getContent()),
					ContentType.parse(entity.getContentType()));
		} catch (UnsupportedCharsetException | UnsupportedOperationException | IOException e) {
			throw new HttpException(e);
		}
	}

	private void printDebug(String str) {
		if (isDebug) {
			System.out.println(
					HttpAsyncClient.class.getSimpleName() + " [DEBUG] " + DateUtil.getCurrentDate() + " - " + str);
		}
	}

	private static class CharsPerSecondCounter {
		private List<CharsAmountInTimeStamp> charsAmountPerTimestampList = new ArrayList<CharsAmountInTimeStamp>();

		private CharsPerSecondCounter() {
		}

		public void updateCharsAmount(long charsAmount) {
			deleteCharsAmountThatTimeMoreThanOneMinute();

			synchronized (charsAmountPerTimestampList) {
				charsAmountPerTimestampList.add(new CharsAmountInTimeStamp(charsAmount));
			}
		}

		private int getSegment_1_second() {
			return getCharsAmountInLastMS(DateUtil.SECOND_BY_MS);
		}

		private int getSegment_5_seconds() {
			return getCharsAmountInLastMS(5 * DateUtil.SECOND_BY_MS) / 5;
		}

		private int getSegment_30_seconds() {
			return getCharsAmountInLastMS(30 * DateUtil.SECOND_BY_MS) / 30;
		}

		private int getSegment_1_minute() {
			return getCharsAmountInLastMS(DateUtil.MINUTE_BY_MS) / 60;
		}

		private int getCharsAmountInLastMS(long ms) {
			long currentTimeMs = System.currentTimeMillis();

			int charsAmount = 0;

			synchronized (charsAmountPerTimestampList) {
				for (int index = charsAmountPerTimestampList.size() - 1; index >= 0; index--) {
					if (charsAmountPerTimestampList.get(index).getTime() + ms >= currentTimeMs) {
						charsAmount += charsAmountPerTimestampList.get(index).getCharsAmount();
					} else {
						return charsAmount;
					}
				}

				return charsAmount;
			}
		}

		private void deleteCharsAmountThatTimeMoreThanOneMinute() {
			long currentTimestamp = System.currentTimeMillis();
			long lastCharAmountTimestamp = 0;

			int index = 0;

			synchronized (charsAmountPerTimestampList) {
				while ((index < charsAmountPerTimestampList.size())
						&& (lastCharAmountTimestamp + DateUtil.MINUTE_BY_MS <= currentTimestamp)) {
					lastCharAmountTimestamp = charsAmountPerTimestampList.get(index).getTime();

					if (lastCharAmountTimestamp + DateUtil.MINUTE_BY_MS < currentTimestamp) {
						charsAmountPerTimestampList.remove(index);
					} else {
						index++;
					}
				}
			}
		}

		/*****************************************************
		 * 
		 * @author Eyal Tuzon
		 *
		 */
		private static class CharsAmountInTimeStamp {
			private final long charsAmount;
			private final long time;

			private CharsAmountInTimeStamp(long charsAmount) {
				this(charsAmount, System.currentTimeMillis());
			}

			private CharsAmountInTimeStamp(long charsAmount, long time) {
				this.charsAmount = charsAmount;
				this.time = time;
			}

			private long getCharsAmount() {
				return charsAmount;
			}

			private long getTime() {
				return time;
			}
		}
	}
}