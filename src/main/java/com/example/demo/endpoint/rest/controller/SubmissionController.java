package com.example.demo.endpoint.rest.controller;

import com.example.demo.repository.model.Submission;
import com.example.demo.service.SubmissionService;
import java.io.IOException;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/submissions")
@AllArgsConstructor
public class SubmissionController {

  private final SubmissionService submissionService;

  @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<Submission> createSubmission(
      @RequestParam("file") MultipartFile file, @RequestParam("email") String email)
      throws IOException {
    var submission = submissionService.create(file, email);
    return ResponseEntity.status(HttpStatus.CREATED).body(submission);
  }

  @GetMapping
  public List<Submission> listSubmissions() {
    return submissionService.findAll();
  }
}
