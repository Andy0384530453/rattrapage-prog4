package com.example.demo.endpoint.rest.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.demo.conf.FacadeIT;
import com.example.demo.endpoint.event.EventProducer;
import com.example.demo.endpoint.event.model.SubmissionReceived;
import com.example.demo.file.bucket.BucketComponent;
import com.example.demo.mail.Email;
import com.example.demo.mail.Mailer;
import com.example.demo.repository.SubmissionRepository;
import com.example.demo.repository.model.Submission;
import com.example.demo.service.event.SubmissionReceivedService;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.net.URL;
import java.nio.file.Files;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

class SubmissionIT extends FacadeIT {

  @Autowired TestRestTemplate restTemplate;
  @Autowired SubmissionRepository submissionRepository;
  @Autowired SubmissionReceivedService submissionReceivedService;

  @MockBean BucketComponent bucketComponent;
  @MockBean EventProducer<SubmissionReceived> eventProducer;
  @MockBean Mailer mailer;

  @Test
  void post_creates_submission_with_null_thumbnail_then_get_lists_it() throws Exception {
    var request = multipartRequest(pngBytes(), "photo.png", "student@example.com");

    var postResponse =
        restTemplate.exchange("/submissions", HttpMethod.POST, request, Submission.class);

    assertEquals(HttpStatus.CREATED, postResponse.getStatusCode());
    var created = postResponse.getBody();
    assertNotNull(created);
    assertNotNull(created.getId());
    assertEquals("student@example.com", created.getEmail());
    assertNull(created.getThumbnailKey());
    assertNotNull(created.getCreatedAt());

    var eventCaptor = ArgumentCaptor.forClass(Collection.class);
    verify(eventProducer).accept(eventCaptor.capture());
    var event = (SubmissionReceived) eventCaptor.getValue().iterator().next();
    assertEquals(created.getId(), event.getSubmissionId());
    assertEquals("student@example.com", event.getEmail());
    assertEquals("submissions/original/" + created.getId() + ".png", event.getOriginalBucketKey());

    var listResponse = restTemplate.getForEntity("/submissions", Submission[].class);
    assertEquals(HttpStatus.OK, listResponse.getStatusCode());
    assertNotNull(listResponse.getBody());
    assertTrue(
        Arrays.stream(listResponse.getBody())
            .map(Submission::getId)
            .anyMatch(created.getId()::equals));
  }

  @Test
  void post_without_extension_uses_jpg_suffix() throws Exception {
    var request = multipartRequest(pngBytes(), "photo", "student2@example.com");

    var postResponse =
        restTemplate.exchange("/submissions", HttpMethod.POST, request, Submission.class);

    assertEquals(HttpStatus.CREATED, postResponse.getStatusCode());
    var created = postResponse.getBody();
    assertNotNull(created);

    var eventCaptor = ArgumentCaptor.forClass(Collection.class);
    verify(eventProducer).accept(eventCaptor.capture());
    var event = (SubmissionReceived) eventCaptor.getValue().iterator().next();
    assertEquals("submissions/original/" + created.getId() + ".jpg", event.getOriginalBucketKey());
  }

  @Test
  void post_without_file_returns_400_with_message() {
    MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
    body.add("email", "student@example.com");

    var response =
        restTemplate.exchange(
            "/submissions", HttpMethod.POST, new HttpEntity<>(body, multipartHeaders()), Map.class);

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    assertNotNull(response.getBody());
    assertTrue(response.getBody().containsKey("message"));
  }

  @Test
  void consumer_resizes_uploads_updates_and_sends_email() throws Exception {
    var submissionId = "submission-consumer-test-" + System.nanoTime();
    var submission = new Submission();
    submission.setId(submissionId);
    submission.setEmail("consumer@example.com");
    submission.setCreatedAt(java.time.Instant.now());
    submissionRepository.save(submission);

    var originalFile = createTempImageFile(200, 100);
    when(bucketComponent.download(anyString())).thenReturn(originalFile);
    var downloadUrl = new URL("https://download.example.com/thumb.png");
    when(bucketComponent.presign(anyString(), any(Duration.class))).thenReturn(downloadUrl);

    var uploadedThumbnailBytes = new AtomicReference<byte[]>();
    var uploadedKey = new AtomicReference<String>();
    doAnswer(
            invocation -> {
              var file = (File) invocation.getArgument(0);
              uploadedThumbnailBytes.set(Files.readAllBytes(file.toPath()));
              uploadedKey.set((String) invocation.getArgument(1));
              return null;
            })
        .when(bucketComponent)
        .upload(any(File.class), anyString());

    var emailCaptor = ArgumentCaptor.forClass(Email.class);

    submissionReceivedService.accept(
        new SubmissionReceived(submissionId, "consumer@example.com", "submissions/original/x.png"));

    var updated = submissionRepository.findById(submissionId).orElseThrow(AssertionError::new);
    assertEquals("submissions/thumbnail/" + submissionId + ".png", updated.getThumbnailKey());

    assertEquals("submissions/thumbnail/" + submissionId + ".png", uploadedKey.get());
    var uploadedThumbnail = ImageIO.read(new ByteArrayInputStream(uploadedThumbnailBytes.get()));
    assertEquals(256, uploadedThumbnail.getWidth());
    assertEquals(256, uploadedThumbnail.getHeight());

    verify(mailer).accept(emailCaptor.capture());
    var sentEmail = emailCaptor.getValue();
    assertEquals("consumer@example.com", sentEmail.to().getAddress());
    assertNotNull(sentEmail.htmlBody());
    assertTrue(sentEmail.htmlBody().contains(downloadUrl.toString()));
  }

  private HttpEntity<MultiValueMap<String, Object>> multipartRequest(
      byte[] fileBytes, String filename, String email) {
    MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
    var resource =
        new ByteArrayResource(fileBytes) {
          @Override
          public String getFilename() {
            return filename;
          }
        };
    body.add("file", resource);
    body.add("email", email);
    return new HttpEntity<>(body, multipartHeaders());
  }

  private HttpHeaders multipartHeaders() {
    var headers = new HttpHeaders();
    headers.setContentType(MediaType.MULTIPART_FORM_DATA);
    return headers;
  }

  private byte[] pngBytes() throws Exception {
    var image = new BufferedImage(120, 80, BufferedImage.TYPE_INT_RGB);
    var graphics = image.createGraphics();
    graphics.setColor(Color.BLUE);
    graphics.fillRect(0, 0, 120, 80);
    graphics.dispose();
    var output = new ByteArrayOutputStream();
    assertTrue(ImageIO.write(image, "png", output));
    return output.toByteArray();
  }

  private File createTempImageFile(int width, int height) throws Exception {
    var image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
    var graphics = image.createGraphics();
    graphics.setColor(Color.GREEN);
    graphics.fillRect(0, 0, width, height);
    graphics.dispose();
    var file = File.createTempFile("original-consumer", ".png");
    ImageIO.write(image, "png", file);
    return file;
  }
}
