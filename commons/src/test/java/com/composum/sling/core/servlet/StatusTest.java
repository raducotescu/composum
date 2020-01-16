package com.composum.sling.core.servlet;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonWriter;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

/** Checks JSON serialization of {@link Status}. */
public class StatusTest {

    private static final Logger LOG = LoggerFactory.getLogger(StatusTest.class);

    @Mock
    private SlingHttpServletRequest request;

    @Mock
    private SlingHttpServletResponse response;

    @Before
    public void setup() throws Exception {
        MockitoAnnotations.initMocks(this);
        when(request.getResourceBundle(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(new PropertyResourceBundle(new StringReader("")));
    }

    @Test
    public void testSerialization() throws Exception {
        Gson gson = new GsonBuilder().create();

        Status status = new Status(request, response);
        status.setStatus(213);
        status.setTitle("thetitle");
        status.warn("hello to {} from {}", "franz", 17);
        List<Map<String, Object>> list1 = status.list("list1");
        list1.add(Collections.singletonMap("mlkey", 42));
        list1.add(new HashMap() {{
            put("mlkey1", 23);
            put("mlkey2", 54);
        }});
        // status.reference("bla", );
        Map<String, Object> dat1 = status.data("dat1");
        dat1.put("dk1", 15);
        dat1.put("dk2", 18);

        Writer stringWriter = new StringWriter();
        JsonWriter jsonWriter = new JsonWriter(stringWriter);
        status.toJson(jsonWriter);

        assertEquals("{\"status\":213,\"success\":true,\"warning\":false,\"title\":\"thetitle\"," +
                "\"messages\":[{\"level\":\"warn\",\"text\":\"hello to franz from 17\"}]," +
                "\"data\":{\"dat1\":{\"dk1\":15,\"dk2\":18}}," +
                "\"list\":{\"list1\":[{\"mlkey\":42},{\"mlkey1\":23,\"mlkey2\":54}]}}", stringWriter.toString());

        Status readback = gson.fromJson(stringWriter.toString(), Status.class);
        assertEquals(213, readback.getStatus());
        assertEquals("thetitle", readback.getTitle());
        assertEquals("hello to franz from 17", readback.messages.get(0).text);
        assertNotNull(readback.data("dat1"));
        assertNotNull(readback.list("list1"));
        assertEquals(54.0, readback.list("list1").get(1).get("mlkey2"));
        assertEquals(18.0, readback.data("dat1").get("dk2"));
    }

    @Test
    public void testExtension() throws Exception {
        Gson gson = new GsonBuilder().create();

        TestStatusExtension status = new TestStatusExtension(request, response);
        status.containedObject = new TestStatusExtensionObject();
        status.containedObject.attr1 = "hallo";
        status.containedObject.attr2 = 27;

        Writer stringWriter = new StringWriter();
        JsonWriter jsonWriter = new JsonWriter(stringWriter);
        status.toJson(jsonWriter);

        TestStatusExtension readback = gson.fromJson(stringWriter.toString(), TestStatusExtension.class);
        assertNotNull(readback);
        assertNotNull(readback.containedObject);
        assertEquals("hallo", readback.containedObject.attr1);
        assertEquals(Integer.valueOf(27), readback.containedObject.attr2);
    }

    @Test
    public void testEmptyStatus() throws Exception {
        Gson gson = new GsonBuilder().create();
        Status status = new Status(request, response);
        Writer stringWriter = new StringWriter();
        JsonWriter jsonWriter = new JsonWriter(stringWriter);
        status.toJson(jsonWriter);

        assertEquals("{\"status\":200,\"success\":true,\"warning\":false}", stringWriter.toString());

        Status readback = gson.fromJson(stringWriter.toString(), Status.class);
        assertNotNull(readback);
    }

    @Test
    public void checkLoggingExceptions() {
        String id = "arg";
        Status status;
        status = new Status(request, response);
        Throwable exception = new RuntimeException("Some exception");
        status.withLogging(LOG).error("For id {} got exception {}", id, exception.toString(), exception);
        System.out.println(status.getJsonString());
        // can't easily check the log output - manual checking required. There should be a stacktrace and a log message
        // StatusTest - For id arg got exception java.lang.RuntimeException: Some exception
        assertTrue(status.getJsonString().contains("For id arg got exception java.lang.RuntimeException: Some exception"));
    }

    private static class TestStatusExtensionObject {
        public String attr1;
        public Integer attr2;
    }

    private static class TestStatusExtension extends Status {

        public TestStatusExtension(@Nonnull SlingHttpServletRequest request, @Nonnull SlingHttpServletResponse response) {
            super(request, response);
        }

        TestStatusExtensionObject containedObject;
    }

    @Test
    public void testTranslate() throws IOException {
        String json = "{\n" +
                "  \"title\": \"thetitle\",\n" +
                "  \"warning\": true,\n" +
                "  \"status\": \"404\",\n" +
                "  \"messages\": [\n" +
                "    {\n" +
                "      \"level\": \"warn\",\n" +
                "      \"context\": \"thecontext\",\n" +
                "      \"label\": \"thelabel\",\n" +
                "      \"text\": \"with hint {}\",\n" +
                "      \"hint\": \"thehint\"\n" +
                "    }\n" +
                "  ]\n" +
                "}\n";
        Gson gson = new GsonBuilder().setPrettyPrinting().create();

        SlingHttpServletRequest request = Mockito.mock(SlingHttpServletRequest.class);
        ResourceBundle bundle = new PropertyResourceBundle(new StringReader(
                "with\\ hint\\ {}: mit Hinweis {}\n" +
                        "thehint: der Hinweis\n" +
                        "thetile: der Titel"));
        Mockito.when(request.getResourceBundle(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(bundle);

        Status status = new Status(gson, request, Mockito.mock(SlingHttpServletResponse.class));
        status.translate(new StringReader(json));

        Writer stringWriter = new StringWriter();
        JsonWriter jsonWriter = new JsonWriter(stringWriter);
        status.toJson(jsonWriter);
        System.out.println(stringWriter.toString());

        assertEquals("{\"status\":404,\"success\":false,\"warning\":true,\"title\":\"thetitle\"," +
                "\"messages\":[{\"level\":\"warn\",\"context\":\"thecontext\",\"label\":\"thelabel\",\"text\":\"mit Hinweis thehint\"}]}", stringWriter.toString());
        assertEquals("{\n" +
                "  \"status\": 404,\n" +
                "  \"success\": false,\n" +
                "  \"warning\": true,\n" +
                "  \"title\": \"thetitle\",\n" + // FIXME(hps,16.01.20) "der Titel"
                "  \"messages\": [\n" +
                "    {\n" +
                "      \"level\": \"warn\",\n" +
                "      \"context\": \"thecontext\",\n" +
                "      \"label\": \"thelabel\",\n" +
                "      \"text\": \"mit Hinweis thehint\"\n" +
                "    }\n" +
                "  ]\n" +
                "}", gson.toJson(status));
    }

}
