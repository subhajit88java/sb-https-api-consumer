package com.subhajit.sbhttpsapiconsumer.config;

import org.apache.http.client.HttpClient;
import org.apache.http.conn.ssl.*;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import javax.net.ssl.SSLContext;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

@Configuration
public class HttpsRestTemplate {

    @Value("${key.truststore}")
    private Resource truststoreCert;

    @Value("${key.truststore.password}")
    private String truststoreCertPassword;

    @Bean(name="restTemplateHttps")
    public RestTemplate restTemplate() {

        HttpClient httpClient = HttpClients.custom()
                .setSSLSocketFactory(getSSLConnectionSocketFactory(truststoreCert,truststoreCertPassword ))
                .setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE)
                .build();

        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory();
        factory.setHttpClient(httpClient);


        RestTemplate restTemplate = new RestTemplate(factory);
        return restTemplate;
    }


    private SSLConnectionSocketFactory getSSLConnectionSocketFactory(Resource truststoreCert, String truststoreCertPassword){
        try{
            KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            FileInputStream fileInputStream = new FileInputStream(truststoreCert.getFile());
            keyStore.load(fileInputStream, truststoreCertPassword.toCharArray());

            SSLContext sslContext = new SSLContextBuilder()
                    .loadTrustMaterial(keyStore, new TrustStrategy() {
                        @Override
                        public boolean isTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {
                            for (X509Certificate cert: x509Certificates) {
                                System.err.println("See -----------> " + cert);
                            }
                            return true;
                        }
                    })
                    //.loadKeyMaterial(keyStore, truststoreCertPassword.toCharArray()) // no need
                    .build();

            return new SSLConnectionSocketFactory(sslContext);

        }catch (KeyStoreException | IOException | CertificateException | NoSuchAlgorithmException  | KeyManagementException e) {
            throw new RuntimeException(e);
        }
    }
}
