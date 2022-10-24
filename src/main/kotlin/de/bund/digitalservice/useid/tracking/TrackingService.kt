package de.bund.digitalservice.useid.tracking

import mu.KotlinLogging
import org.springframework.stereotype.Service
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

@Service
class TrackingService(private val trackingProperties: TrackingProperties) {

    private val log = KotlinLogging.logger {}
    private val httpClient: HttpClient = HttpClient.newBuilder().build()
    private val responseHandler: HttpResponse.BodyHandler<String> = HttpResponse.BodyHandlers.ofString()

    private fun sendTrackingRequest(url: String) {
        val request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .build()

        val response = httpClient.send(request, responseHandler)
        val status = response.statusCode()
        if (status != 200) {
            log.error("$status, tracking failed for: $url")
        }
        log.info("$status, successfully tracked: $url")
    }

    fun sendMatomoEvent(category: String, action: String, name: String) {
        val matomoSiteId = trackingProperties.matomoSiteId
        val domain = trackingProperties.matomoDomain
        sendTrackingRequest("$domain?idsite=$matomoSiteId&rec=1&ca=1&e_c=$category&e_a=$action&e_n=$name")
    }
}
