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
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
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
                .setSSLSocketFactory(getSSLConnectionSocketFactory(clientTruststore,clientTruststorePassword))
                //.setSSLSocketFactory(get2WaySSLConnectionSocketFactory2(clientTruststore,clientTruststorePassword,clientKeystore , clientKeystorePassword))
                //.setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE)
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

            // TrustSelfSignedStrategy implemented, means any server cert which self signed or containing just 1 entry will be trusted. No cert check will be done with client-truststore as reference
            // Also with X509TrustManagerImpl implemented here, no cert expiration check will be performed for self-signed certs only, though more research needed on this statement
            // Even if we dont pass any truststore cert in the 1st param the server cert will be trusted because TrustSelfSignedStrategy is implemented

            // TrustAllStrategy implemented, any self signed cert or CA signed cert chain will be trusted
            // rest all features same as TrustSelfSignedStrategy

            // If we customize TrustStrategy interface and override isTrusted() then,
            // If return  = true, no cert validation will be performed on client side. Any server cert will be trusted
            // If return  = false, second level of validation will be performed by X509TrustManagerImpl's  checkServerTrusted(). Under X509TrustManagerImpl validation, server-cert validation will be done with client-truststore cert and also expiry date validation
            // Throw exeption, we can write customized validation logic here and throw exception so that X509TrustManagerImpl's  checkServerTrusted() is not called.
            // NB : returning false from isTrusted() method and not using any TrustStrategy as 2nd param are equivalent. Both with delegate the flow to X509TrustManagerImpl's  checkServerTrusted() to perform 2nd level of validations
            SSLContext sslContext = new SSLContextBuilder()
                    .loadTrustMaterial(keyStore, new TrustStrategy(){
                        @Override
                        public boolean isTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {
                              return false;
                        }
                    })
                    .build();

            // We can create our customized trust-manager so avoid execution of X509TrustManagerImpl

//            X509TrustManager wrapper = new X509TrustManager() {
//
//                @Override
//                public void checkClientTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {
//                    System.out.println("checkClientTrusted");
//                }
//
//                @Override
//                public void checkServerTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {
//                    System.out.println("checkServerTrusted");
//                }
//
//                @Override
//                public X509Certificate[] getAcceptedIssuers() {
//                    System.out.println("getAcceptedIssuers");
//                    return new X509Certificate[0];
//                }
//            };

           // sslContext.init(null, new TrustManager[]{wrapper}, null);

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
