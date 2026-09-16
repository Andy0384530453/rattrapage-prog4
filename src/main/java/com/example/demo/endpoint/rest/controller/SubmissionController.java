package com.example.demo.endpoint.rest.controller;

import com.example.demo.repository.model.Submission;
import com.example.demo.service.SubmissionService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
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
      @RequestParam("file") MultipartFile file,
      @RequestParam(value = "email", required = false) String email,
      HttpServletRequest request)
      throws IOException, ServletException {
    var submission = submissionService.create(file, email(request, email));
    return ResponseEntity.status(HttpStatus.CREATED).body(submission);
  }

  private String email(HttpServletRequest request, String fromParameter)
      throws IOException, ServletException {
    if (fromParameter != null && !fromParameter.isBlank()) {
      return fromParameter;
    }
    var part = request.getPart("email");
    if (part == null) {
      throw new IllegalArgumentException("Required request parameter 'email' is not present");
    }
    return new String(part.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
  }

  @GetMapping
  public List<Submission> listSubmissions() {
    return submissionService.findAll();
  }
}
