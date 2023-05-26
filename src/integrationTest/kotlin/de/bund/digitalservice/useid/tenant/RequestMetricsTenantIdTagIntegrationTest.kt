package de.bund.digitalservice.useid.tenant

import de.bund.digitalservice.useid.eidservice.EidService
import de.bund.digitalservice.useid.identification.extractRelativePathFromURL
import de.bund.digitalservice.useid.identification.mockTcToken
import de.bund.digitalservice.useid.identification.sendGETRequest
import de.bund.digitalservice.useid.identification.sendStartSessionRequest
import io.mockk.mockkConstructor
import io.mockk.unmockkAll
import mu.KotlinLogging
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.reactive.server.WebTestClient
import java.util.UUID

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureObservability
@TestPropertySource(properties = ["management.endpoints.web.exposure.include=prometheus"])
class RequestMetricsTenantIdTagIntegrationTest(@Autowired val webTestClient: WebTestClient) {

    val expectedTenantId = "integration_test_1"

    @BeforeAll
    fun beforeAll() {
        mockkConstructor(EidService::class)
    }

    @AfterAll
    fun afterAll() {
        unmockkAll()
    }

    @Test
    fun `prometheus metric contains tenant ID tag for start session`() {
        // Given
        val expectedPrometheusLogRegex = Regex("http_server_requests_seconds_count\\{error=\"none\",exception=\"none\",method=\"POST\",outcome=\"SUCCESS\",status=\"200\",tenant_id=\"(.+)\",uri=\"/api/v1/identifications\",}")

        // When
        webTestClient.sendStartSessionRequest()

        // Then
        val tenantId = getTenantIdFromPrometheusLog(expectedPrometheusLogRegex)
        assertThat(tenantId).isEqualTo(expectedTenantId)
    }

    @Test
    fun `prometheus metric contains tenant ID tag for tc token endpoint`() {
        val expectedPrometheusLogRegex = Regex("http_server_requests_seconds_count\\{error=\"none\",exception=\"none\",method=\"GET\",outcome=\"SUCCESS\",status=\"200\",tenant_id=\"(.+)\",uri=\"/api/v1/tc-tokens/\\{useIdSessionId}\",}")

        // Given
        var tcTokenURL = ""
        webTestClient.sendStartSessionRequest()
            .expectBody().jsonPath("$.tcTokenUrl").value<String> { tcTokenURL = it }
        val eIdSessionId = UUID.randomUUID()
        mockTcToken("https://www.foobar.com?sessionId=$eIdSessionId")

        // When
        webTestClient.sendGETRequest(extractRelativePathFromURL(tcTokenURL))

        // Then
        val tenantId = getTenantIdFromPrometheusLog(expectedPrometheusLogRegex)
        assertThat(tenantId).isEqualTo(expectedTenantId)
    }

    @Test
    fun `prometheus metric contains tenant ID tag for widget endpoint`() {
        // Given
        val expectedPrometheusLogRegex = Regex("http_server_requests_seconds_count\\{error=\"none\",exception=\"none\",method=\"GET\",outcome=\"SUCCESS\",status=\"200\",tenant_id=\"(.+)\",uri=\"/widget\",}")
        val allowedHost = "i.am.allowed.1"

        // When
        webTestClient.sendGETRequest("/widget?hostname=$allowedHost")
            .expectStatus().isOk

        // Then
        val tenantId = getTenantIdFromPrometheusLog(expectedPrometheusLogRegex, debug = true)
        assertThat(tenantId).isEqualTo(expectedTenantId)
    }

    private fun getTenantIdFromPrometheusLog(expectedPrometheusLogRegex: Regex, debug: Boolean = false): String? {
        val log = KotlinLogging.logger {}
        var tenantId: String? = ""
        webTestClient
            .sendGETRequest("actuator/prometheus")
            .expectStatus().isOk
            .expectBody()
            .returnResult()
            .responseBody?.let { prometheusLogRaw ->
            val prometheusLog = String(bytes = prometheusLogRaw)
            if (debug) {
                println("##### DEBUG LOG \n $prometheusLog")
                log.info {
                    "##### DEBUG LOG \n" +
                        " $prometheusLog"
                }
            }
            val result = expectedPrometheusLogRegex.find(prometheusLog)
            tenantId = result?.groupValues?.get(1)
        }
        return tenantId
    }
}
