package com.firefly.net.tcp.ssl;

import com.firefly.utils.exception.CommonRuntimeException;
import io.netty.handler.ssl.ApplicationProtocolConfig;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SupportedCipherSuiteFilter;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.netty.handler.ssl.util.SelfSignedCertificate;

import javax.net.ssl.SSLException;
import java.security.cert.CertificateException;
import java.util.Arrays;
import java.util.List;

/**
 * @author Pengtao Qiu
 */
public class SelfSignedCertificateOpenSSLContextFactory extends AbstractOpenSSLContextFactory {

    private SelfSignedCertificate selfSignedCertificate;
    private List<String> supportedProtocols = Arrays.asList("h2", "h2-17", "h2-16", "h2-15", "h2-14", "http/1.1");

    public SelfSignedCertificateOpenSSLContextFactory() {
        try {
            selfSignedCertificate = new SelfSignedCertificate("www.fireflysource.com");
        } catch (CertificateException e) {
            log.error("create certificate exception", e);
            throw new CommonRuntimeException(e);
        }
    }

    public SelfSignedCertificate getSelfSignedCertificate() {
        return selfSignedCertificate;
    }

    public void setSelfSignedCertificate(SelfSignedCertificate selfSignedCertificate) {
        this.selfSignedCertificate = selfSignedCertificate;
    }

    public List<String> getSupportedProtocols() {
        return supportedProtocols;
    }

    public void setSupportedProtocols(List<String> supportedProtocols) {
        this.supportedProtocols = supportedProtocols;
    }

    @Override
    public SslContext createSSLContext(boolean clientMode) {
        SslContextBuilder sslContextBuilder = clientMode ? SslContextBuilder.forClient().trustManager(InsecureTrustManagerFactory.INSTANCE)
                : SslContextBuilder.forServer(selfSignedCertificate.certificate(), selfSignedCertificate.privateKey());

        try {
            return sslContextBuilder.ciphers(SecurityUtils.CIPHERS, SupportedCipherSuiteFilter.INSTANCE)
                                    .applicationProtocolConfig(new ApplicationProtocolConfig(ApplicationProtocolConfig.Protocol.ALPN,
                                            ApplicationProtocolConfig.SelectorFailureBehavior.CHOOSE_MY_LAST_PROTOCOL,
                                            ApplicationProtocolConfig.SelectedListenerFailureBehavior.CHOOSE_MY_LAST_PROTOCOL,
                                            supportedProtocols)).build();
        } catch (SSLException e) {
            log.error("create ssl context exception", e);
            throw new CommonRuntimeException(e);
        }
    }
}
