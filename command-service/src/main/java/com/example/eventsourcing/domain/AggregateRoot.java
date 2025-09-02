package com.example.eventsourcing.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Classe base para todos os agregados que implementam Event Sourcing
 */
@Getter
@Setter
public abstract class AggregateRoot {
    protected UUID id;
    protected Long version = 0L;

    @JsonIgnore
    private final List<Event> uncommittedEvents = new ArrayList<>();

    protected AggregateRoot() { }

    protected AggregateRoot(UUID id) {
        this.id = id;
    }

    /**
     * Aplica um evento novo ao agregado (será persistido no Event Store).
     */
    protected void applyNewEvent(Event event) {
        event.apply(this);
        this.version = event.getVersion();
        this.uncommittedEvents.add(event);
    }

    /**
     * Aplica um evento histórico ao agregado (não será persistido de novo).
     */
    protected void applyHistoricalEvent(Event event) {
        event.apply(this);
        this.version = event.getVersion();
    }

    public List<Event> getUncommittedEvents() {
        return new ArrayList<>(uncommittedEvents);
    }

    public void markEventsAsCommitted() {
        uncommittedEvents.clear();
    }

    public boolean hasUncommittedEvents() {
        return !uncommittedEvents.isEmpty();
    }

    public String getAggregateType() {
        return this.getClass().getSimpleName();
    }
    // cada agregado precisa implementar sua própria re-hidratação
    public abstract void loadFromHistory(List<Event> events);
}
