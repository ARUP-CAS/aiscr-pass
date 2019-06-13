package cz.ssc.pass.integration;

import com.meterware.httpunit.HttpException;
import com.meterware.httpunit.PostMethodWebRequest;
import com.meterware.httpunit.WebConversation;
import com.meterware.httpunit.WebRequest;
import com.meterware.httpunit.WebResponse;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Set;
import junit.framework.TestCase;
import org.apache.commons.io.FileUtils;
import org.json.JSONObject;
import org.junit.Test;

public class SmokeTest extends TestCase {
    private final String testUrl;

    public SmokeTest() throws IOException {
        URL confRes = SmokeTest.class.getResource("/cz/ssc/pass/config.json");
        File confFile = FileUtils.toFile(confRes);
        String confStr = FileUtils.readFileToString(confFile, "UTF-8");
        JSONObject json = new JSONObject(confStr);

        testUrl = json.getString("testurl");
    }

    @Test
    public void testInvalid() throws Exception {
        doInvalid("");
    }

    @Test
    public void testEmpty() throws Exception {
        doInvalid("{}");
    }

     @Test
     public void testUnknown() throws Exception {
        WebConversation conversation = new WebConversation();
        String reqBody = "{ \"user\": \"x\", \"pwd\": \"y\" }";
        InputStream reqStream = new ByteArrayInputStream(reqBody.getBytes());
        WebRequest request = new PostMethodWebRequest(
                testUrl,
                reqStream,
                "application/json;charset=UTF-8");
        WebResponse response = conversation.getResponse(request);
        assertEquals(200, response.getResponseCode());

        byte[] rspBytes = response.getBytes();
        JSONObject json = new JSONObject(new String(rspBytes));
        Set<String> keys = json.keySet();
        assertEquals(1, keys.size());
        String errVal = json.getString("error");
        assertEquals("unknown", errVal);
    }

    private void doInvalid(String reqBody) throws Exception {
        WebConversation conversation = new WebConversation();
        InputStream reqStream = new ByteArrayInputStream(reqBody.getBytes());

        try {
            WebRequest request = new PostMethodWebRequest(
                    testUrl,
                    reqStream,
                    "application/json;charset=UTF-8");
            conversation.getResponse(request);
            fail("unexpected success");
        } catch (HttpException ex) {
            assertEquals(400, ex.getResponseCode());
        }
    }
}
