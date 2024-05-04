package com.subhajit.sbhttpsapiconsumer.controller;

import com.subhajit.sbhttpsapiconsumer.service.TestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

@RestController
public class TestController {

    @Autowired
    private TestService service;

    @GetMapping("/test-https")
    public ResponseEntity<?> testHttps() {
       return service.testHttps();
    }
}
