package com.example.demo.repository.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "submission")
@Getter
@Setter
public class Submission {
  @Id
  @Column(name = "id", nullable = false)
  private String id;

  @Column(name = "email", nullable = false)
  private String email;

  @Column(name = "thumbnail_key")
  private String thumbnailKey;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;
}
