package com.subhajit.sbhttpsapiconsumer.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

@Component
public class TestService {

    @Autowired
    @Qualifier("restTemplateHttps")
    private RestTemplate restTemplate;

    public ResponseEntity<String> testHttps() {
          ResponseEntity<String> responseEntity = null;
        try {
            System.out.println("HTTPS call .....................");
            responseEntity = restTemplate.getForEntity("https://localhost:443/test-https-get", String.class);
            System.out.println("Result is : " +
                    responseEntity.getStatusCodeValue() + " - " + responseEntity.getStatusCode()
                    + " - " + responseEntity.getBody());
        } catch (HttpClientErrorException hce) {
            System.out.println("HttpClientErrorException is : " +
                    hce.getStatusText() + " <-> " + hce.getStatusCode()
                    + " <-> " + hce.getResponseBodyAsString());
            responseEntity = new ResponseEntity<>(hce.getResponseBodyAsString(), hce.getStatusCode());
        } catch (HttpServerErrorException hse) {
            System.out.println("HttpServerErrorException is : " +
                    hse.getStatusText() + " - " + hse.getStatusCode()
                    + " - " + hse.getResponseBodyAsString());
            responseEntity = new ResponseEntity<>(hse.getResponseBodyAsString(), hse.getStatusCode());
        } catch (Exception e) {
            System.out.println("Exception is : " +
                    e.getMessage());
            responseEntity = new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
            e.printStackTrace();
        }
        return responseEntity;
    }
}
