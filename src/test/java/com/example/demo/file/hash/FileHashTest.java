package com.example.demo.file.hash;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class FileHashTest {

  @Test
  void exposes_algorithm_and_value() {
    var fileHash = new FileHash(FileHashAlgorithm.SHA256, "abc");

    assertEquals(FileHashAlgorithm.SHA256, fileHash.algorithm());
    assertEquals("abc", fileHash.value());
  }
}
