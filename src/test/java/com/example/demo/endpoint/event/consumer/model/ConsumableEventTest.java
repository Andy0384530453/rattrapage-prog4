package com.example.demo.endpoint.event.consumer.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.demo.endpoint.event.model.UuidCreated;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;

class ConsumableEventTest {

  @Test
  void ack_runs_acknowledger() {
    var acked = new AtomicBoolean(false);
    var event =
        new ConsumableEvent(
            new TypedEvent("UuidCreated", new UuidCreated()), () -> acked.set(true), () -> {});

    event.ack();

    assertTrue(acked.get());
  }

  @Test
  void new_random_visibility_timeout_runs_setter() {
    var visibilitySet = new AtomicBoolean(false);
    var event =
        new ConsumableEvent(
            new TypedEvent("UuidCreated", new UuidCreated()),
            () -> {},
            () -> visibilitySet.set(true));

    event.newRandomVisibilityTimeout();

    assertTrue(visibilitySet.get());
  }

  @Test
  void exposes_event_payload() {
    var uuidCreated = new UuidCreated();
    var event = new ConsumableEvent(new TypedEvent("UuidCreated", uuidCreated), () -> {}, () -> {});

    assertEquals(uuidCreated, event.getEvent().payload());
    assertEquals("UuidCreated", event.getEvent().typeName());
  }
}
