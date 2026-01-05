package com.urlshortener.model;

import java.time.LocalDateTime;
import java.util.UUID;

public class ShortLink {
    private String shortCode;
    private String originalUrl;
    private UUID ownerUuid;
    private int maxVisits;
    private int currentVisits;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;

    public ShortLink(String shortCode, String originalUrl, UUID ownerUuid, int maxVisits, int ttlMinutes) {
        this.shortCode = shortCode;
        this.originalUrl = originalUrl;
        this.ownerUuid = ownerUuid;
        this.maxVisits = maxVisits;
        this.currentVisits = 0;
        this.createdAt = LocalDateTime.now();
        this.expiresAt = this.createdAt.plusMinutes(ttlMinutes);
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    public boolean isLimitReached() {
        return currentVisits >= maxVisits;
    }

    public void incrementVisits() {
        this.currentVisits++;
    }

    public void setMaxVisits(int maxVisits) {
        this.maxVisits = maxVisits;
    }

    // Getters
    public String getShortCode() { return shortCode; }
    public String getOriginalUrl() { return originalUrl; }
    public UUID getOwnerUuid() { return ownerUuid; }
    public int getMaxVisits() { return maxVisits; }
    public int getCurrentVisits() { return currentVisits; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
}
