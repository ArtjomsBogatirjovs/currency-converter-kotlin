package lv.bogatirjovs.currencyconverterkotlin.utils

import java.net.http.HttpClient
import java.security.SecureRandom
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

object HttpClientUtils {
    fun createInsecureHttpClient(): HttpClient {
        return HttpClient.newBuilder()
            .sslContext(createInsecureSSLContext())
            .build()
    }

    internal fun createInsecureSSLContext(): SSLContext {
        val sslContext = SSLContext.getInstance("TLS")
        sslContext.init(
            null,
            arrayOf<TrustManager>(
                object : X509TrustManager {
                    override fun getAcceptedIssuers(): Array<java.security.cert.X509Certificate> = arrayOf()
                    override fun checkClientTrusted(
                        chain: Array<java.security.cert.X509Certificate>,
                        authType: String
                    ) {
                    }

                    override fun checkServerTrusted(
                        chain: Array<java.security.cert.X509Certificate>,
                        authType: String
                    ) {
                    }
                }
            ),
            SecureRandom()
        )
        return sslContext
    }
}