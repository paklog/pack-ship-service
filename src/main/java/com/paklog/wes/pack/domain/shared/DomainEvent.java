package com.paklog.wes.pack.domain.shared;

import java.time.Instant;

/**
 * Base class for all domain events in Pack & Ship bounded context
 */
public abstract class DomainEvent {

    private final Instant occurredOn;

    protected DomainEvent() {
        this.occurredOn = Instant.now();
    }

    public Instant getOccurredOn() {
        return occurredOn;
    }

    public String getEventType() {
        return this.getClass().getSimpleName();
    }
}
