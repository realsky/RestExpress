package com.strategicgains.restexpress;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.jboss.netty.handler.codec.http.HttpHeaders;
import org.jboss.netty.handler.codec.http.HttpMethod;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.strategicgains.restexpress.common.query.QueryRange;
import com.strategicgains.restexpress.domain.JsendResultWrapper;
import com.strategicgains.restexpress.pipeline.SimpleConsoleLogMessageObserver;
import com.strategicgains.restexpress.response.JsendResponseWrapper;
import com.strategicgains.restexpress.serialization.AbstractSerializationProvider;
import com.strategicgains.restexpress.serialization.DefaultSerializationProvider;
import com.strategicgains.restexpress.serialization.json.JacksonJsonProcessor;
import com.strategicgains.restexpress.serialization.xml.XstreamXmlProcessor;

public class RestExpressServerTest
{
	private static final String URL_PATTERN1 = "/1/restexpress/{id}/test/{test}.{format}";
	private static final String URL_PATTERN2 = "/2/restexpress/{id}/test/{test}";
	private static final String URL_PATTERN3 = "/3/restexpress/{id}/test/{test}.{format}";
	private static final String URL_PATTERN4 = "/4/restexpress/{id}/test/{test}.{format}";
	private static final String LITTLE_O_PATTERN = "/littleos/{id}.{format}";
	private static final String LITTLE_OS_PATTERN = "/littleos.{format}";
	private static final String URL_PATH1 = "/1/restexpress/sam/test/42";
	private static final String URL_PATH3 = "/3/restexpress/polly/test/56";
	private static final String URL_PATH4 = "/4/restexpress/allen/test/33";
	private static final String LITTLE_O_PATH = "/littleos/1";
	private static final String LITTLE_OS_PATH = "/littleos";
	private static final int SERVER_PORT = 8800;
	private static final String SERVER_HOST = "http://localhost:" + SERVER_PORT;
	private static final String URL1_PLAIN = SERVER_HOST + URL_PATH1;
	private static final String URL1_JSON = SERVER_HOST + URL_PATH1 + ".json";
	private static final String URL1_XML = SERVER_HOST + URL_PATH1 + ".xml";
	private static final String URL3_PLAIN = SERVER_HOST + URL_PATH3;
	private static final String URL4_PLAIN = SERVER_HOST + URL_PATH4;
	private static final String LITTLE_O_URL = SERVER_HOST + LITTLE_O_PATH;
	private static final String LITTLE_OS_URL = SERVER_HOST + LITTLE_OS_PATH;

	private RestExpress server = new RestExpress();
	AbstractSerializationProvider serializer = new DefaultSerializationProvider();
	private HttpClient http = new DefaultHttpClient();

	@Before
	public void createServer()
	{
		serializer.add(new JacksonJsonProcessor(Format.WRAPPED_JSON), new JsendResponseWrapper());
		serializer.add(new XstreamXmlProcessor(Format.WRAPPED_XML), new JsendResponseWrapper());
		RestExpress.setSerializationProvider(serializer);

		server.uri(URL_PATTERN1, new StringTestController());
		server.uri(URL_PATTERN2, new StringTestController());
		server.uri(URL_PATTERN3, new StringTestController())
			.method(HttpMethod.GET, HttpMethod.POST);
		server.uri(URL_PATTERN4, new StringTestController())	// Collection route.
			.method(HttpMethod.POST)
			.action("readAll", HttpMethod.GET);
		server.uri(LITTLE_O_PATTERN, new ObjectTestController())
			.method(HttpMethod.GET);
		server.uri(LITTLE_OS_PATTERN, new ObjectTestController())
			.action("readAll", HttpMethod.GET);
		server.addMessageObserver(new SimpleConsoleLogMessageObserver());
		
		server.alias("littleObject", LittleO.class);
//		server.alias("list", ArrayList.class);
	}

	@After
	public void shutdownServer()
	{
		server.shutdown();
	}


	// SECTION: TESTS

	@Test
	public void shouldHandleGetRequests()
	throws Exception
	{
		server.bind(SERVER_PORT);
		
		HttpGet request = new HttpGet(URL1_PLAIN);
		HttpResponse response = (HttpResponse) http.execute(request);
		assertEquals(HttpResponseStatus.OK.getCode(), response.getStatusLine().getStatusCode());
		HttpEntity entity = response.getEntity();
		assertTrue(entity.getContentLength() > 0l);
		assertEquals(ContentType.JSON, entity.getContentType().getValue());
		assertEquals("\"read\"", EntityUtils.toString(entity));
		request.releaseConnection();
	}

	@Test
	public void shouldHandlePutRequests()
	throws Exception
	{
		server.bind(SERVER_PORT);
		
		HttpPut request = new HttpPut(URL1_PLAIN);
		HttpResponse response = (HttpResponse) http.execute(request);
		assertEquals(HttpResponseStatus.OK.getCode(), response.getStatusLine().getStatusCode());
		HttpEntity entity = response.getEntity();
		assertTrue(entity.getContentLength() > 0l);
		assertEquals(ContentType.JSON, entity.getContentType().getValue());
		assertEquals("\"update\"", EntityUtils.toString(entity));
		request.releaseConnection();
	}

	@Test
	public void shouldHandlePostRequests()
	throws Exception
	{
		server.bind(SERVER_PORT);
		
		HttpPost request = new HttpPost(URL1_PLAIN);
		HttpResponse response = (HttpResponse) http.execute(request);
		assertEquals(HttpResponseStatus.CREATED.getCode(), response.getStatusLine().getStatusCode());
		HttpEntity entity = response.getEntity();
		assertTrue(entity.getContentLength() > 0l);
		assertEquals(ContentType.JSON, entity.getContentType().getValue());
		assertEquals("\"create\"", EntityUtils.toString(entity));
		request.releaseConnection();
	}

	@Test
	public void shouldHandleDeleteRequests()
	throws Exception
	{
		server.bind(SERVER_PORT);
		
		HttpDelete request = new HttpDelete(URL1_PLAIN);
		HttpResponse response = (HttpResponse) http.execute(request);
		assertEquals(HttpResponseStatus.OK.getCode(), response.getStatusLine().getStatusCode());
		HttpEntity entity = response.getEntity();
		assertTrue(entity.getContentLength() > 0l);
		assertEquals(ContentType.JSON, entity.getContentType().getValue());
		assertEquals("\"delete\"", EntityUtils.toString(entity));
		request.releaseConnection();
	}

	@Test
	public void shouldCallSpecifiedMethod()
	throws Exception
	{
		server.bind(SERVER_PORT);
		
		HttpGet request = new HttpGet(URL4_PLAIN);
		HttpResponse response = (HttpResponse) http.execute(request);
		assertEquals(HttpResponseStatus.OK.getCode(), response.getStatusLine().getStatusCode());
		HttpEntity entity = response.getEntity();
		assertTrue(entity.getContentLength() > 0l);
		assertEquals(ContentType.JSON, entity.getContentType().getValue());
		assertEquals("\"readAll\"", EntityUtils.toString(entity));
		request.releaseConnection();
	}

	@Test
	public void shouldFailWithMethodNotAllowed()
	throws Exception
	{
		server.bind(SERVER_PORT);
		
		HttpDelete request = new HttpDelete(URL3_PLAIN);
		HttpResponse response = (HttpResponse) http.execute(request);
		assertEquals(HttpResponseStatus.METHOD_NOT_ALLOWED.getCode(), response.getStatusLine().getStatusCode());
		HttpEntity entity = response.getEntity();
		assertTrue(entity.getContentLength() > 0l);
		assertEquals(ContentType.JSON, entity.getContentType().getValue());
		assertEquals("\"" + URL3_PLAIN + "\"", EntityUtils.toString(entity));
		String methods = response.getHeaders(HttpHeaders.Names.ALLOW)[0].getValue();
		assertTrue(methods.contains("GET"));
		assertTrue(methods.contains("POST"));
		request.releaseConnection();
	}

	@Test
	public void shouldFailWithNotFound()
	throws Exception
	{
		server.bind(SERVER_PORT);
		
		HttpDelete request = new HttpDelete(SERVER_HOST + "/x/y/z.json");
		HttpResponse response = (HttpResponse) http.execute(request);
		assertEquals(HttpResponseStatus.NOT_FOUND.getCode(), response.getStatusLine().getStatusCode());
		HttpEntity entity = response.getEntity();
		assertTrue(entity.getContentLength() > 0l);
		assertEquals(ContentType.JSON, entity.getContentType().getValue());
		assertEquals("\"Unresolvable URL: " + SERVER_HOST + "/x/y/z.json\"", EntityUtils.toString(entity));
		request.releaseConnection();
	}

	@Test
	public void shouldReturnXmlUsingFormat()
	throws Exception
	{
		server.bind(SERVER_PORT);
		
		HttpGet request = new HttpGet(URL1_XML);
		HttpResponse response = (HttpResponse) http.execute(request);
		assertEquals(HttpResponseStatus.OK.getCode(), response.getStatusLine().getStatusCode());
		HttpEntity entity = response.getEntity();
		assertTrue(entity.getContentLength() > 0l);
		assertEquals(ContentType.XML, entity.getContentType().getValue());
		assertEquals("<string>read</string>", EntityUtils.toString(entity));
		request.releaseConnection();
	}

	@Test
	public void shouldReturnXmlUsingAccept()
	throws Exception
	{
		server.bind(SERVER_PORT);
		
		HttpGet request = new HttpGet(URL1_PLAIN);
		request.addHeader(HttpHeaders.Names.ACCEPT, "application/xml");
		HttpResponse response = (HttpResponse) http.execute(request);
		assertEquals(HttpResponseStatus.OK.getCode(), response.getStatusLine().getStatusCode());
		HttpEntity entity = response.getEntity();
		assertTrue(entity.getContentLength() > 0l);
		assertEquals(ContentType.XML, entity.getContentType().getValue());
		assertEquals("<string>read</string>", EntityUtils.toString(entity));
		request.releaseConnection();
	}

	@Test
	public void shouldFavorFormatOverAcceptHeader()
	throws Exception
	{
		server.bind(SERVER_PORT);
		
		HttpGet request = new HttpGet(URL1_XML);
		request.addHeader(HttpHeaders.Names.ACCEPT, "application/json");
		HttpResponse response = (HttpResponse) http.execute(request);
		assertEquals(HttpResponseStatus.OK.getCode(), response.getStatusLine().getStatusCode());
		HttpEntity entity = response.getEntity();
		assertTrue(entity.getContentLength() > 0l);
		assertEquals(ContentType.XML, entity.getContentType().getValue());
		assertEquals("<string>read</string>", EntityUtils.toString(entity));
		request.releaseConnection();
	}

	@Test
	public void shouldReturnJsonUsingFormat()
	throws Exception
	{
		server.bind(SERVER_PORT);
		
		HttpGet request = new HttpGet(URL1_JSON);
		HttpResponse response = (HttpResponse) http.execute(request);
		assertEquals(HttpResponseStatus.OK.getCode(), response.getStatusLine().getStatusCode());
		HttpEntity entity = response.getEntity();
		assertTrue(entity.getContentLength() > 0l);
		assertEquals(ContentType.JSON, entity.getContentType().getValue());
		assertEquals("\"read\"", EntityUtils.toString(entity));
		request.releaseConnection();
	}

	@Test
	public void shouldReturnErrorOnCapitalizedFormat()
	throws Exception
	{
		server.bind(SERVER_PORT);

		HttpGet request = new HttpGet(URL1_PLAIN + ".JSON");
		HttpResponse response = (HttpResponse) http.execute(request);
		assertEquals(HttpResponseStatus.BAD_REQUEST.getCode(), response.getStatusLine().getStatusCode());
		HttpEntity entity = response.getEntity();
		assertTrue(entity.getContentLength() > 0l);
		assertEquals(ContentType.JSON, entity.getContentType().getValue());
		assertEquals("\"Requested representation format not supported: JSON. Supported formats: json, wxml, wjson, xml\"", EntityUtils.toString(entity));
		request.releaseConnection();
	}

	@Test
	public void shouldReturnWrappedJsonUsingFormat()
	throws Exception
	{
		server.bind(SERVER_PORT);
		
		HttpGet request = new HttpGet(URL1_PLAIN + ".wjson");
		HttpResponse response = (HttpResponse) http.execute(request);
		assertEquals(HttpResponseStatus.OK.getCode(), response.getStatusLine().getStatusCode());
		HttpEntity entity = response.getEntity();
		assertTrue(entity.getContentLength() > 0l);
		assertEquals(ContentType.JSON, entity.getContentType().getValue());
		String result = EntityUtils.toString(entity);
		assertTrue(result.contains("\"code\":200"));
		assertTrue(result.contains("\"status\":\"success\""));
		String data = extractJson(result);
		assertEquals("\"read\"", data);
		request.releaseConnection();
	}

	@Test
	public void shouldReturnWrappedJsonAsDefault()
	throws Exception
	{
		serializer.setDefaultFormat(Format.WRAPPED_JSON);
		server.bind(SERVER_PORT);

		HttpGet request = new HttpGet(URL1_PLAIN);
		HttpResponse response = (HttpResponse) http.execute(request);
		assertEquals(HttpResponseStatus.OK.getCode(), response.getStatusLine().getStatusCode());
		HttpEntity entity = response.getEntity();
		assertTrue(entity.getContentLength() > 0l);
		assertEquals(ContentType.JSON, entity.getContentType().getValue());
		String result = EntityUtils.toString(entity);
		assertTrue(result.contains("\"code\":200"));
		assertTrue(result.contains("\"status\":\"success\""));
		String data = extractJson(result);
		assertEquals("\"read\"", data);
		request.releaseConnection();
	}

	@Test
	public void shouldReturnWrappedXmlUsingFormat()
	throws Exception
	{
		server.bind(SERVER_PORT);
		
		HttpGet request = new HttpGet(URL1_PLAIN + ".wxml");
		HttpResponse response = (HttpResponse) http.execute(request);
		assertEquals(HttpResponseStatus.OK.getCode(), response.getStatusLine().getStatusCode());
		HttpEntity entity = response.getEntity();
		assertTrue(entity.getContentLength() > 0l);
		assertEquals(ContentType.XML, entity.getContentType().getValue());
		String entityString = EntityUtils.toString(entity);
		assertTrue(entityString.startsWith("<response>"));
		assertTrue(entityString.contains("<code>200</code>"));
		assertTrue(entityString.contains("<status>success</status>"));
		assertTrue(entityString.contains("<data class=\"string\">read</data>"));
		assertTrue(entityString.endsWith("</response>"));
		request.releaseConnection();
	}

	@Test
	public void shouldReturnWrappedXmlAsDefault()
	throws Exception
	{
		serializer.setDefaultFormat(Format.WRAPPED_XML);
		server.bind(SERVER_PORT);
		
		HttpGet request = new HttpGet(URL1_PLAIN);
		HttpResponse response = (HttpResponse) http.execute(request);
		assertEquals(HttpResponseStatus.OK.getCode(), response.getStatusLine().getStatusCode());
		HttpEntity entity = response.getEntity();
		assertTrue(entity.getContentLength() > 0l);
		assertEquals(ContentType.XML, entity.getContentType().getValue());
		String entityString = EntityUtils.toString(entity);
		assertTrue(entityString.startsWith("<response>"));
		assertTrue(entityString.contains("<code>200</code>"));
		assertTrue(entityString.contains("<status>success</status>"));
		assertTrue(entityString.contains("<data class=\"string\">read</data>"));
		assertTrue(entityString.endsWith("</response>"));
		request.releaseConnection();
	}

	@Test
	public void shouldReturnNonSerializedTextPlainResult()
	throws Exception
	{
		server.uri("/unserialized", new StringTestController())
			.noSerialization();
		server.bind(SERVER_PORT);
		
		HttpGet request = new HttpGet(SERVER_HOST + "/unserialized");
		HttpResponse response = (HttpResponse) http.execute(request);
		assertEquals(HttpResponseStatus.OK.getCode(), response.getStatusLine().getStatusCode());
		HttpEntity entity = response.getEntity();
		assertTrue(entity.getContentLength() > 0l);
		assertEquals(ContentType.TEXT_PLAIN, entity.getContentType().getValue());
		assertEquals("read", EntityUtils.toString(entity));
		request.releaseConnection();
	}

	@Test
	public void shouldFailWithBadRequest()
	throws Exception
	{
		server.bind(SERVER_PORT);
		
		HttpGet request = new HttpGet(URL1_PLAIN + ".xyz");
		HttpResponse response = (HttpResponse) http.execute(request);
		assertEquals(HttpResponseStatus.BAD_REQUEST.getCode(), response.getStatusLine().getStatusCode());
		HttpEntity entity = response.getEntity();
		assertTrue(entity.getContentLength() > 0l);
		assertEquals(ContentType.JSON, entity.getContentType().getValue());
		assertEquals("\"Requested representation format not supported: xyz. Supported formats: json, wxml, wjson, xml\"", EntityUtils.toString(entity));
		request.releaseConnection();
	}

	@Test
	public void shouldFailOnInvalidAccept()
	throws Exception
	{
		server.bind(SERVER_PORT);
		
		HttpGet request = new HttpGet(URL1_PLAIN);
		request.addHeader(HttpHeaders.Names.ACCEPT, "application/nogood");
		HttpResponse response = (HttpResponse) http.execute(request);
		assertEquals(HttpResponseStatus.NOT_ACCEPTABLE.getCode(), response.getStatusLine().getStatusCode());
		HttpEntity entity = response.getEntity();
		assertTrue(entity.getContentLength() > 0l);
		assertEquals(ContentType.JSON, entity.getContentType().getValue());
		assertEquals("\"Supported Media Types: application/json; charset=UTF-8, application/javasctript; charset=UTF-8, text/javascript; charset=UTF-8, application/xml; charset=UTF-8, text/xml; charset=UTF-8\"", EntityUtils.toString(entity));
		request.releaseConnection();
	}

	@Test
	public void shouldSerializeObjectAsJson()
	throws Exception
	{
		server.bind(SERVER_PORT);
		
		HttpGet request = new HttpGet(LITTLE_O_URL + ".json");
		HttpResponse response = (HttpResponse) http.execute(request);
		assertEquals(HttpResponseStatus.OK.getCode(), response.getStatusLine().getStatusCode());
		HttpEntity entity = response.getEntity();
		assertTrue(entity.getContentLength() > 0l);
		assertEquals(ContentType.JSON, entity.getContentType().getValue());
		LittleO o = serializer.getSerializer(Format.JSON).deserialize(EntityUtils.toString(entity), LittleO.class);
		verifyObject(o);
		request.releaseConnection();
	}

	@Test
	public void shouldSerializeListAsJson()
	throws Exception
	{
		server.bind(SERVER_PORT);
		
		HttpGet request = new HttpGet(LITTLE_OS_URL + ".json");
		HttpResponse response = (HttpResponse) http.execute(request);
		assertEquals(HttpResponseStatus.OK.getCode(), response.getStatusLine().getStatusCode());
		HttpEntity entity = response.getEntity();
		assertTrue(entity.getContentLength() > 0l);
		assertEquals(ContentType.JSON, entity.getContentType().getValue());
		Header range = response.getFirstHeader(HttpHeaders.Names.CONTENT_RANGE);
		assertNotNull(range);
		assertEquals("items 0-2/3", range.getValue());
		LittleO[] result = serializer.getSerializer(Format.JSON).deserialize(EntityUtils.toString(entity), LittleO[].class);
		verifyList(result);
		request.releaseConnection();
	}

	@Test
	public void shouldNotContainContentRangeHeaderOnInvalidAcceptHeader()
	throws Exception
	{
		server.bind(SERVER_PORT);
		
		HttpGet request = new HttpGet(LITTLE_OS_URL);
		request.addHeader(HttpHeaders.Names.ACCEPT, "no-good/no-good");
		HttpResponse response = (HttpResponse) http.execute(request);
		assertEquals(HttpResponseStatus.NOT_ACCEPTABLE.getCode(), response.getStatusLine().getStatusCode());
		HttpEntity entity = response.getEntity();
		assertTrue(entity.getContentLength() > 0l);
		assertEquals(ContentType.JSON, entity.getContentType().getValue());
		assertNull(response.getFirstHeader(HttpHeaders.Names.CONTENT_RANGE));
		assertEquals("\"Supported Media Types: application/json; charset=UTF-8, application/javasctript; charset=UTF-8, text/javascript; charset=UTF-8, application/xml; charset=UTF-8, text/xml; charset=UTF-8\"", EntityUtils.toString(entity));
		request.releaseConnection();
	}

	@Test
	public void shouldSerializeObjectAsWrappedJson()
	throws Exception
	{
		server.bind(SERVER_PORT);
		
		HttpGet request = new HttpGet(LITTLE_O_URL + ".wjson");
		HttpResponse response = (HttpResponse) http.execute(request);
		assertEquals(HttpResponseStatus.OK.getCode(), response.getStatusLine().getStatusCode());
		HttpEntity entity = response.getEntity();
		assertTrue(entity.getContentLength() > 0l);
		assertEquals(ContentType.JSON, entity.getContentType().getValue());
		String result = EntityUtils.toString(entity);
		assertTrue(result.contains("\"code\":200"));
		assertTrue(result.contains("\"status\":\"success\""));
		String data = extractJson(result);
		LittleO o = serializer.getSerializer(Format.WRAPPED_JSON).deserialize(data, LittleO.class);
		verifyObject(o);
		request.releaseConnection();
	}

	@Test
	public void shouldSerializeListAsWrappedJson()
	throws Exception
	{
		server.bind(SERVER_PORT);
		
		HttpGet request = new HttpGet(LITTLE_OS_URL + ".wjson");
		HttpResponse response = (HttpResponse) http.execute(request);
		assertEquals(HttpResponseStatus.OK.getCode(), response.getStatusLine().getStatusCode());
		HttpEntity entity = response.getEntity();
		assertTrue(entity.getContentLength() > 0l);
		assertEquals(ContentType.JSON, entity.getContentType().getValue());
		Header range = response.getFirstHeader(HttpHeaders.Names.CONTENT_RANGE);
		assertNotNull(range);
		assertEquals("items 0-2/3", range.getValue());
		String result = EntityUtils.toString(entity);
		assertTrue(result.contains("\"code\":200"));
		assertTrue(result.contains("\"status\":\"success\""));
		String data = extractJson(result);
		LittleO[] o = serializer.getSerializer(Format.WRAPPED_JSON).deserialize(data, LittleO[].class);
		verifyList(o);
		request.releaseConnection();
	}

	@Test
	public void shouldSerializeObjectAsXml()
	throws Exception
	{
		server.bind(SERVER_PORT);
		
		HttpGet request = new HttpGet(LITTLE_O_URL + ".xml");
		HttpResponse response = (HttpResponse) http.execute(request);
		assertEquals(HttpResponseStatus.OK.getCode(), response.getStatusLine().getStatusCode());
		HttpEntity entity = response.getEntity();
		assertTrue(entity.getContentLength() > 0l);
		assertEquals(ContentType.XML, entity.getContentType().getValue());
		LittleO o = serializer.getSerializer(Format.XML).deserialize(EntityUtils.toString(entity), LittleO.class);
		verifyObject(o);
		request.releaseConnection();
	}

	@Test
	public void shouldSerializeListAsXml()
	throws Exception
	{
		server.bind(SERVER_PORT);
		
		HttpGet request = new HttpGet(LITTLE_OS_URL + ".xml");
		HttpResponse response = (HttpResponse) http.execute(request);
		assertEquals(HttpResponseStatus.OK.getCode(), response.getStatusLine().getStatusCode());
		HttpEntity entity = response.getEntity();
		assertTrue(entity.getContentLength() > 0l);
		assertEquals(ContentType.XML, entity.getContentType().getValue());
		Header range = response.getFirstHeader(HttpHeaders.Names.CONTENT_RANGE);
		assertNotNull(range);
		assertEquals("items 0-2/3", range.getValue());
		String entityString = EntityUtils.toString(entity);
		@SuppressWarnings("unchecked")
        List<LittleO> o = serializer.getSerializer(Format.XML).deserialize(entityString, ArrayList.class);
		verifyList(o.toArray(new LittleO[0]));
		request.releaseConnection();
	}

	@Test
	public void shouldSerializeObjectAsWrappedXml()
	throws Exception
	{
		server.bind(SERVER_PORT);
		
		HttpGet request = new HttpGet(LITTLE_O_URL + ".wxml");
		HttpResponse response = (HttpResponse) http.execute(request);
		assertEquals(HttpResponseStatus.OK.getCode(), response.getStatusLine().getStatusCode());
		HttpEntity entity = response.getEntity();
		assertTrue(entity.getContentLength() > 0l);
		assertEquals(ContentType.XML, entity.getContentType().getValue());
		String entityString = EntityUtils.toString(entity);
		JsendResultWrapper o = serializer.getSerializer(Format.WRAPPED_XML).deserialize(entityString, JsendResultWrapper.class);
		assertEquals(200, o.getCode());
		assertEquals("success", o.getStatus());
		verifyObject((LittleO) o.getData());
		request.releaseConnection();
	}

    @Test
	public void shouldSerializeListAsWrappedXml()
	throws Exception
	{
		server.bind(SERVER_PORT);
		
		HttpGet request = new HttpGet(LITTLE_OS_URL + ".wxml");
		HttpResponse response = (HttpResponse) http.execute(request);
		assertEquals(HttpResponseStatus.OK.getCode(), response.getStatusLine().getStatusCode());
		HttpEntity entity = response.getEntity();
		assertTrue(entity.getContentLength() > 0l);
		assertEquals(ContentType.XML, entity.getContentType().getValue());
		Header range = response.getFirstHeader(HttpHeaders.Names.CONTENT_RANGE);
		assertNotNull(range);
		assertEquals("items 0-2/3", range.getValue());
		String entityString = EntityUtils.toString(entity);
		JsendResultWrapper o = serializer.getSerializer(Format.WRAPPED_XML).deserialize(entityString, JsendResultWrapper.class);
		verifyList(((ArrayList<LittleO>)o.getData()).toArray(new LittleO[0]));
		request.releaseConnection();
	}

	private String extractJson(String string)
    {
		final String search = "\"data\":";
		int start = string.indexOf(search) + search.length();
		return string.substring(start, string.length() - 1);
    }

	private void verifyObject(LittleO o)
    {
		assertNotNull(o);
		LittleO expected = new LittleO();
		assertEquals(expected.getName(), o.getName());
		assertEquals(expected.getInteger(), o.getInteger());
		assertEquals(expected.isBoolean(), o.isBoolean());
		assertEquals(3, o.getChildren().size());
		assertEquals(3, o.getArray().length);
    }

	private void verifyList(LittleO[] result)
    {
	    assertEquals(3, result.length);
		assertEquals("name", result[0].getName());
    }


	// SECTION: INNER CLASSES

	@SuppressWarnings("unused")
	private class StringTestController
	{
        public String create(Request request, Response response)
        {
        	response.setResponseCreated();
        	return "create";
		}

		public String read(Request request, Response response)
		{
			return "read";
		}
		
		public String update(Request request, Response response)
		{
			return "update";
		}
		
		public String delete(Request request, Response response)
		{
			return "delete";
		}
		
		public String readAll(Request request, Response response)
		{
			return "readAll";
		}
	}
	
	@SuppressWarnings("unused")
    private class ObjectTestController
	{
		public LittleO read(Request request, Response Response)
		{
			return newLittleO(3);
		}
		
		public List<LittleO> readAll(Request request, Response response)
		{
			QueryRange range = new QueryRange(0, 3);
			response.addRangeHeader(range, 3);
			List<LittleO> l = new ArrayList<LittleO>();
			l.add(newLittleO(1));
			l.add(newLittleO(2));
			l.add(newLittleO(3));
			return l;
		}
		
		private LittleO newLittleO(int count)
		{
			LittleO l = new LittleO();
			List<LittleO> list = new ArrayList<LittleO>(count);
			
			for (int i = 0; i < count; i++)
			{
				list.add(new LittleO());
			}

			l.setChildren(list);
			return l;
		}
	}
}
