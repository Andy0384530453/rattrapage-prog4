package com.example.demo.repository.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class Submission {
  @Id private String id;

  private String email;

  private String thumbnailKey;

  private Instant createdAt;
}
