package pt.luis.projectaboutanimals.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

@Component
public class PaypalClient {

    private final RestTemplate http = new RestTemplate();

    @Value("${paypal.baseUrl}") private String baseUrl;
    @Value("${paypal.clientId}") private String clientId;
    @Value("${paypal.clientSecret}") private String clientSecret;

    public String getAccessToken() {
        String auth = clientId + ":" + clientSecret;
        String basic = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Basic " + basic);
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String,String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "client_credentials");

        HttpEntity<MultiValueMap<String,String>> req = new HttpEntity<>(body, headers);

        ResponseEntity<Map> res = http.exchange(
                baseUrl + "/v1/oauth2/token",
                HttpMethod.POST,
                req,
                Map.class
        );

        Object token = res.getBody().get("access_token");
        return token == null ? null : token.toString();
    }

    public Map createOrder(String token, Map payload) {
        HttpHeaders h = new HttpHeaders();
        h.setBearerAuth(token);
        h.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map> req = new HttpEntity<>(payload, h);

        ResponseEntity<Map> res = http.exchange(
                baseUrl + "/v2/checkout/orders",
                HttpMethod.POST,
                req,
                Map.class
        );
        return res.getBody();
    }

    public Map captureOrder(String token, String orderId) {
        HttpHeaders h = new HttpHeaders();
        h.setBearerAuth(token);
        h.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Void> req = new HttpEntity<>(h);

        ResponseEntity<Map> res = http.exchange(
                baseUrl + "/v2/checkout/orders/" + orderId + "/capture",
                HttpMethod.POST,
                req,
                Map.class
        );
        return res.getBody();
    }
}
