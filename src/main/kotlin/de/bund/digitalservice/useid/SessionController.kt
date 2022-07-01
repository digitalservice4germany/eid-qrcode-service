package de.bund.digitalservice.useid

import de.bund.digitalservice.useid.model.ClientRequestSession
import de.bund.digitalservice.useid.model.SessionResponse
import de.bund.digitalservice.useid.service.SessionService
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono
import javax.validation.Valid

@RestController
@RequestMapping("/api/v1")
class SessionController(private val sessionService: SessionService) {
    @PostMapping(
        path = ["/session"],
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    fun initSession(@Valid @RequestBody clientRequestSession: ClientRequestSession): Mono<SessionResponse> {
        return sessionService.getSession(clientRequestSession)
    }
}
