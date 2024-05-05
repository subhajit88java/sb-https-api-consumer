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

    @Value("${client.truststore}")
    private Resource clientTruststore;

    @Value("${client.truststore.password}")
    private String clientTruststorePassword;

    @Value("${client.keystore}")
    private Resource clientKeystore;

    @Value("${client.keystore.password}")
    private String clientKeystorePassword;

    @Bean(name="restTemplateHttps")
    public RestTemplate restTemplate() {

        HttpClient httpClient = HttpClients.custom()
                .setSSLSocketFactory(get2WaySSLConnectionSocketFactory2(clientTruststore,clientTruststorePassword,clientKeystore , clientKeystorePassword))
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

            // Implementing SSLContext with user-defined TrustStrategy, here we can write our own validation logic
           /* SSLContext sslContext = new SSLContextBuilder()
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
                    .build();*/

            // Implementing SSLContext with user-defined TrustSelfSignedStrategy, self signed + CA signed certs will be trusted here
            SSLContext sslContext = new SSLContextBuilder()
                    .loadTrustMaterial(keyStore, new TrustSelfSignedStrategy())
                    .build();

            return new SSLConnectionSocketFactory(sslContext);

        }catch (KeyStoreException | IOException | CertificateException | NoSuchAlgorithmException  | KeyManagementException e) {
            throw new RuntimeException(e);
        }
    }

    private SSLConnectionSocketFactory get2WaySSLConnectionSocketFactory2(Resource clientTruststore, String clientTruststorePassword, Resource clientKeystore, String clientKeystorePassword){
        try{
            KeyStore truststore = KeyStore.getInstance(KeyStore.getDefaultType());
            FileInputStream fileInputStreamTrust = new FileInputStream(clientTruststore.getFile());
            truststore.load(fileInputStreamTrust, clientTruststorePassword.toCharArray()); // This password is jks file password

            KeyStore keystore = KeyStore.getInstance(KeyStore.getDefaultType());
            FileInputStream fileInputStreamKey = new FileInputStream(clientKeystore.getFile());
            keystore.load(fileInputStreamKey, clientKeystorePassword.toCharArray()); // This password is jks file password

            SSLContext sslContext = new SSLContextBuilder()
                    .loadTrustMaterial(truststore, new TrustSelfSignedStrategy())
                    .loadKeyMaterial(keystore, "client_p12".toCharArray()) // used for 2 way SSl, need to send this keystore so that server can validate client. This password is the p12/privatekey entry password
                    .build();

            return new SSLConnectionSocketFactory(sslContext);

        }catch (KeyStoreException | IOException | CertificateException | NoSuchAlgorithmException  | KeyManagementException | UnrecoverableKeyException e) {
            throw new RuntimeException(e);
        }
    }
}
