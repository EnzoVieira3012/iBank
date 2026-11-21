package com.ibank.audit.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChecksumService {

    private static final String ALGORITHM = "HmacSHA256";

    @Value("${ibank.audit.checksum.secret:change-me-in-production}")
    private String secret;

    public String computeChecksum(String entityType, String entityId, String eventType,
                                  String payload, String metadata, Instant occurredAt) {
        String data = String.join("|",
            entityType,
            entityId,
            eventType,
            payload != null ? payload : "",
            metadata != null ? metadata : "",
            occurredAt.toString()
        );
        return hmacSha256(data);
    }

    public String computeChecksum(AuditEvent event) {
        return computeChecksum(
            event.getEntityType(),
            event.getEntityId(),
            event.getEventType(),
            serialize(event.getPayload()),
            serialize(event.getMetadata()),
            event.getOccurredAt()
        );
    }

    public boolean verifyChecksum(AuditEvent event) {
        String expected = computeChecksum(event);
        return expected.equals(event.getChecksum());
    }

    private String hmacSha256(String data) {
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            SecretKeySpec keySpec = new SecretKeySpec(
                secret.getBytes(StandardCharsets.UTF_8), ALGORITHM);
            mac.init(keySpec);
            byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new IllegalStateException("Failed to compute HMAC", e);
        }
    }

    private String serialize(Object obj) {
        if (obj == null) return "";
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(obj);
        } catch (Exception e) {
            log.warn("Failed to serialize object for checksum", e);
            return obj.toString();
        }
    }
}