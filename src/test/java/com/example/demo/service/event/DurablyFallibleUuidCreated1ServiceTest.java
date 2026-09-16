package com.example.demo.service.event;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.example.demo.endpoint.event.model.DurablyFallibleUuidCreated1;
import com.example.demo.endpoint.event.model.UuidCreated;
import org.junit.jupiter.api.Test;

class DurablyFallibleUuidCreated1ServiceTest {

  private final UuidCreatedService uuidCreatedService = mock(UuidCreatedService.class);
  private final DurablyFallibleUuidCreated1Service service =
      new DurablyFallibleUuidCreated1Service(uuidCreatedService);

  @Test
  void delegates_to_uuid_created_service_when_no_failure() {
    var event = new DurablyFallibleUuidCreated1(new UuidCreated("uuid"), 0, 0.0);

    service.accept(event);

    var expectedNested = new UuidCreated("uuid");
    verify(uuidCreatedService).accept(expectedNested);
  }

  @Test
  void throws_when_failure_expected() {
    var event = new DurablyFallibleUuidCreated1(new UuidCreated("uuid"), 0, 1.0);

    assertThrows(RuntimeException.class, () -> service.accept(event));
  }
}
