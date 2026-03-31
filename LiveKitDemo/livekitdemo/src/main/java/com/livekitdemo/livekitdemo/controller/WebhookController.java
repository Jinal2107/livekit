package com.livekitdemo.livekitdemo.controller;

import com.livekitdemo.livekitdemo.service.EgressService;
import com.livekitdemo.livekitdemo.service.TranscriptionService;
import io.livekit.server.WebhookReceiver;
import livekit.LivekitEgress;
import livekit.LivekitWebhook;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/webhooks")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin
public class WebhookController {

    private final EgressService egressService;
    private final TranscriptionService transcriptionService;

    @Value("${livekit.api.key}")
    private String apiKey;

    @Value("${livekit.api.secret}")
    private String apiSecret;

    @PostMapping("/livekit")
    public ResponseEntity<String> handleLiveKitWebhook(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody String body
    ) {
        WebhookReceiver receiver = new WebhookReceiver(apiKey, apiSecret);
        try {
            LivekitWebhook.WebhookEvent event = receiver.receive(body, authHeader);
            log.info("Received LiveKit Webhook: {}", event.getEvent());

            if (event.hasEgressInfo()) {
                LivekitEgress.EgressInfo egress = event.getEgressInfo();
                String status = egress.getStatus().name();
                String fileUrl = "";
                if (egress.hasFile()) {
                    fileUrl = egress.getFile().getLocation();
                }
                egressService.updateRecordingStatus(egress.getEgressId(), status, fileUrl);
            }

            // Handle transcription if it comes as a room event or similar
            // This depends on how LiveKit is configured to send transcriptions
            // For now, assume it's a specific event type or metadata update

            return ResponseEntity.ok("OK");
        } catch (Exception e) {
            log.error("Error processing webhook", e);
            return ResponseEntity.status(401).body("Invalid webhook signature");
        }
    }
}
