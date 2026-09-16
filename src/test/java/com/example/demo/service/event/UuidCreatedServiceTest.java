package com.example.demo.service.event;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.example.demo.endpoint.event.model.UuidCreated;
import com.example.demo.repository.DummyUuidRepository;
import com.example.demo.repository.model.DummyUuid;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class UuidCreatedServiceTest {

  private final DummyUuidRepository dummyUuidRepository = mock(DummyUuidRepository.class);
  private final UuidCreatedService uuidCreatedService = new UuidCreatedService(dummyUuidRepository);

  @Test
  void saves_dummy_uuid_with_event_id() {
    uuidCreatedService.accept(new UuidCreated("some-uuid-id"));

    var captor = ArgumentCaptor.forClass(DummyUuid.class);
    verify(dummyUuidRepository).save(captor.capture());
    assertEquals("some-uuid-id", captor.getValue().getId());
  }
}
