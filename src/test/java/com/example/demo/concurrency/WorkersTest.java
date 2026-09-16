package com.example.demo.concurrency;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.concurrent.Callable;
import org.junit.jupiter.api.Test;

class WorkersTest {

  private final Workers<String> workers = new Workers<>();

  @Test
  void runs_callables_and_returns_results() {
    var results = workers.apply(List.of(() -> "a", () -> "b", () -> "c"));

    assertEquals(List.of("a", "b", "c"), results);
  }

  @Test
  void propagates_callable_failure() {
    Callable<String> failing =
        () -> {
          throw new IllegalStateException("boom");
        };

    assertThrows(RuntimeException.class, () -> workers.apply(List.of(failing)));
  }
}
