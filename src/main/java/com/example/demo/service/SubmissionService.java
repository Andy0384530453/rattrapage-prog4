package com.example.demo.service;

import static java.io.File.createTempFile;
import static java.util.UUID.randomUUID;

import com.example.demo.endpoint.event.EventProducer;
import com.example.demo.endpoint.event.model.SubmissionReceived;
import com.example.demo.file.bucket.BucketComponent;
import com.example.demo.repository.SubmissionRepository;
import com.example.demo.repository.model.Submission;
import java.io.IOException;
import java.nio.file.Files;
import java.time.Instant;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@AllArgsConstructor
public class SubmissionService {

  private static final String ORIGINAL_PREFIX = "submissions/original/";

  private final SubmissionRepository submissionRepository;
  private final BucketComponent bucketComponent;
  private final EventProducer<SubmissionReceived> eventProducer;

  public Submission create(MultipartFile file, String email) throws IOException {
    var submission = new Submission();
    submission.setId(randomUUID().toString());
    submission.setEmail(email);
    submission.setCreatedAt(Instant.now());
    submissionRepository.save(submission);

    var originalBucketKey = ORIGINAL_PREFIX + submission.getId() + suffix(file);
    var originalFile = createTempFile("original-", suffix(file));
    Files.write(originalFile.toPath(), file.getBytes());
    bucketComponent.upload(originalFile, originalBucketKey);
    originalFile.delete();

    eventProducer.accept(
        List.of(
            SubmissionReceived.builder()
                .submissionId(submission.getId())
                .email(email)
                .originalBucketKey(originalBucketKey)
                .build()));

    return submission;
  }

  public List<Submission> findAll() {
    return submissionRepository.findAll();
  }

  private String suffix(MultipartFile file) {
    var originalFilename = file.getOriginalFilename();
    if (originalFilename == null || originalFilename.isBlank()) {
      return ".jpg";
    }
    var dotIndex = originalFilename.lastIndexOf('.');
    if (dotIndex < 0 || dotIndex == originalFilename.length() - 1) {
      return ".jpg";
    }
    return originalFilename.substring(dotIndex);
  }
}
