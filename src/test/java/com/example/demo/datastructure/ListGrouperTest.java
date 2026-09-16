package com.example.demo.datastructure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class ListGrouperTest {

  private final ListGrouper<String> listGrouper = new ListGrouper<>();

  @Test
  void groups_list_by_size() {
    List<List<String>> groups = listGrouper.apply(List.of("a", "b", "c", "d", "e"), 2);

    assertEquals(3, groups.size());
    assertEquals(List.of("a", "b"), groups.get(0));
    assertEquals(List.of("c", "d"), groups.get(1));
    assertEquals(List.of("e"), groups.get(2));
  }

  @Test
  void empty_list_gives_no_group() {
    assertTrue(listGrouper.apply(List.of(), 3).isEmpty());
  }
}
