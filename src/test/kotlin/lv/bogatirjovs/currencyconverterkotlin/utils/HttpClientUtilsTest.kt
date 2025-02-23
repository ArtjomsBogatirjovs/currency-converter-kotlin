package lv.bogatirjovs.currencyconverterkotlin.utils

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import java.net.http.HttpClient
import javax.net.ssl.SSLContext

class HttpClientUtilsTest {

    @Test
    fun `createInsecureHttpClient returns a non-null HttpClient`() {
        val client: HttpClient = HttpClientUtils.createInsecureHttpClient()
        assertNotNull(client, "HttpClient should not be null")
        assertEquals("TLS", client.sslContext().protocol, "SSLContext protocol should be TLS")
    }

    @Test
    fun `createInsecureSSLContext returns SSLContext with TLS protocol`() {
        val sslContext: SSLContext = HttpClientUtils.createInsecureSSLContext()
        assertEquals("TLS", sslContext.protocol, "SSLContext protocol should be TLS")
    }
}