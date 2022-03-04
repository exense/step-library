package ch.exense.step.library.selenium;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import net.lightbody.bmp.BrowserMobProxy;
import net.lightbody.bmp.BrowserMobProxyServer;
import net.lightbody.bmp.client.ClientUtil;
import okhttp3.*;
import org.openqa.selenium.Proxy;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.X509TrustManager;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * This proxy intercepts https requests and adds certificate based client authentication.
 * It was only tested chromeDriver and msedgedriver. For other drivers, minor adjustments in the filter may be necessary.
 * Note that the url protocol to navigate to must be http, not https
 */
public class SeleniumSslProxy extends Proxy {

    private BrowserMobProxy browserMobProxy;

    private static final List<String> googleDomainsFilter = List.of("accounts.google.com", "gstatic.com");
    private static final List<String> microsoftDomainsFilter = List.of("microsoft.com", "msn.com", "bing.com", "sb.scorecardresearch.com", "akamaized.net");
    private static final List<String> domainsFilter = new ArrayList<>();
    static {
        domainsFilter.addAll(googleDomainsFilter);
        domainsFilter.addAll(microsoftDomainsFilter);
    }
    public SeleniumSslProxy(File clientSslCertificate, String certificatePassword) {
        BrowserMobProxy browserMobProxy = new BrowserMobProxyServer();

        browserMobProxy.addRequestFilter((request, contents, messageInfo) -> {
            String url = request.getUri().replace("http://", "https://");
            if (domainsFilter.stream().anyMatch(url::contains) || !url.startsWith("https://")) {
                return null; // do not intercept driver-specific and non-https requests
            }
            SSLContext sslContext = createSslContext(clientSslCertificate, certificatePassword);
            Response intermediateResponse = doHttpsRequest(sslContext, url, request.getMethod(), contents.getContentType(), contents.getBinaryContents());
            return convertOkhttpResponseToNettyResponse(intermediateResponse);
        });

        this.browserMobProxy = browserMobProxy;
        this.setProxyType(Proxy.ProxyType.MANUAL);
    }

    public void start(int port) {
        this.browserMobProxy.start(port);
        InetSocketAddress connectableAddressAndPort = new InetSocketAddress(ClientUtil.getConnectableAddress(), browserMobProxy.getPort());
        String proxyStr = String.format("%s:%d", connectableAddressAndPort.getHostString(), connectableAddressAndPort.getPort());
        this.setHttpProxy(proxyStr);
        this.setSslProxy(proxyStr);
    }

    public void stop() {
        this.browserMobProxy.stop();
    }

    private Response doHttpsRequest(SSLContext sslContext, String url, HttpMethod httpMethod, String mediaType, byte[] body) {
        RequestBody requestBody = null;
        if (httpMethod != HttpMethod.GET) { // might need to prohibit body for other methods too
            requestBody = RequestBody.create(MediaType.get(mediaType), body);
        }

        Request request = new Request.Builder()
                .url(url)
                .method(httpMethod.name(), requestBody)
                .build();

        OkHttpClient client = new OkHttpClient.Builder()
                .sslSocketFactory(sslContext.getSocketFactory(), new X509TrustManager() {
                    @Override
                    public void checkClientTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {

                    }

                    @Override
                    public void checkServerTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {

                    }

                    @Override
                    public X509Certificate[] getAcceptedIssuers() {
                        return new X509Certificate[0];
                    }
                })
                //.sslSocketFactory(sslContext.getSocketFactory()) // for the non-deprecated version, a truststore must be used as a second parameter
                .build();
        try {
            return client.newCall(request).execute();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private SSLContext createSslContext(File clientSslCertificate, String certificatePassword) {
        try {
            KeyStore keyStore = KeyStore.getInstance("PKCS12");
            keyStore.load(new FileInputStream(clientSslCertificate), certificatePassword.toCharArray());

            KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance("SunX509");
            keyManagerFactory.init(keyStore, certificatePassword.toCharArray());
            KeyManager[] keyManagers = keyManagerFactory.getKeyManagers();

            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(keyManagers, null, null);

            return sslContext;
        } catch (IOException | KeyStoreException | NoSuchAlgorithmException | CertificateException | UnrecoverableKeyException | KeyManagementException e) {
            e.printStackTrace();
        }
        return null;
    }

    private FullHttpResponse convertOkhttpResponseToNettyResponse(Response okhttpResponse) {
        HttpResponseStatus httpResponseStatus = HttpResponseStatus.valueOf(okhttpResponse.code());
        HttpVersion httpVersion = HttpVersion.valueOf(okhttpResponse.protocol().toString());

        ByteBuf content = null;
        try (ResponseBody body = okhttpResponse.body()){
            content = Unpooled.wrappedBuffer(body.bytes());
        } catch (IOException e) {
            e.printStackTrace();
        }

        FullHttpResponse nettyResponse = new DefaultFullHttpResponse(httpVersion, httpResponseStatus, content);
        okhttpResponse.headers().toMultimap().forEach((key, values) -> {
            nettyResponse.headers().remove(key);
            nettyResponse.headers().add(key, String.join(",", values));
        });

        return nettyResponse;
    }

    public BrowserMobProxy getBrowserMobProxy() {
        return this.browserMobProxy;
    }

}