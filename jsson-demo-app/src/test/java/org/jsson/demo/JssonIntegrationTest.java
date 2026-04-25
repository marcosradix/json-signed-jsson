package org.jsson.demo;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class JssonIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    public void testJssonSignAndVerifyEndToEnd() throws Exception {
        // 1. GET - The application automatically generates the signature before returning the JSON
        MvcResult getResult = mockMvc.perform(get("/api/orders/ORDER-1020"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value("ORDER-1020"))
                .andExpect(jsonPath("$['$jsson'].sig").exists()) // Asserting the dynamic $jsson addition
                .andExpect(jsonPath("$['$jsson'].alg").value("Ed25519"))
                .andReturn();

        // 2. Extract the exact generated String
        String jssonSignedPayload = getResult.getResponse().getContentAsString();

        // 3. POST - Send it back and expect 200 OK success (Interceptor found and verified the signature)
        mockMvc.perform(post("/api/orders/process")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jssonSignedPayload))
                .andExpect(status().isOk());
    }

    @Test
    public void testJssonVerifyBlocksForgedData() throws Exception {
        // A. Valid original GET
        MvcResult getResult = mockMvc.perform(get("/api/orders/ORDER-9999"))
                .andReturn();
        
        String originalJson = getResult.getResponse().getContentAsString();
        
        // B. Man-in-The-Middle attack trying to bypass the monthly price from 39.99 to zero.
        String forgedJson = originalJson.replace("39.99", "0.0");

        // C. The POST will send the data containing the original uncorrupted "sig" attached to a changed String
        // The verification will fail and the pipeline will return an infrastructure block (403 Forbidden).
        mockMvc.perform(post("/api/orders/process")
                .contentType(MediaType.APPLICATION_JSON)
                .content(forgedJson))
                .andExpect(status().isForbidden());
    }
}
