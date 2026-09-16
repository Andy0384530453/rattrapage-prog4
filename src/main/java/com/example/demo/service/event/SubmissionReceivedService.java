package com.example.demo.service.event;

import com.example.demo.endpoint.event.model.SubmissionReceived;
import com.example.demo.file.bucket.BucketComponent;
import com.example.demo.mail.Email;
import com.example.demo.mail.Mailer;
import com.example.demo.repository.SubmissionRepository;
import com.example.demo.service.ThumbnailComponent;
import jakarta.mail.internet.InternetAddress;
import java.time.Duration;
import java.util.List;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
@Slf4j
public class SubmissionReceivedService implements Consumer<SubmissionReceived> {

  private static final String THUMBNAIL_PREFIX = "submissions/thumbnail/";
  private static final Duration LINK_VALIDITY = Duration.ofDays(7);

  private final BucketComponent bucketComponent;
  private final ThumbnailComponent thumbnailComponent;
  private final SubmissionRepository submissionRepository;
  private final Mailer mailer;

  @Override
  @SneakyThrows
  public void accept(SubmissionReceived event) {
    var originalFile = bucketComponent.download(event.getOriginalBucketKey());
    var thumbnailFile = thumbnailComponent.resizeTo256(originalFile);
    originalFile.delete();

    var thumbnailKey = THUMBNAIL_PREFIX + event.getSubmissionId() + ".png";
    bucketComponent.upload(thumbnailFile, thumbnailKey);
    thumbnailFile.delete();

    submissionRepository
        .findById(event.getSubmissionId())
        .ifPresent(
            submission -> {
              submission.setThumbnailKey(thumbnailKey);
              submissionRepository.save(submission);
            });

    var downloadUrl = bucketComponent.presign(thumbnailKey, LINK_VALIDITY).toString();
    mailer.accept(
        new Email(
            new InternetAddress(event.getEmail()),
            List.of(),
            List.of(),
            "Your thumbnail is ready",
            "<p>Hello,</p>"
                + "<p>Your 256x256 thumbnail is ready. Download it by clicking on the link "
                + "below:</p>"
                + "<p><a href=\""
                + downloadUrl
                + "\">Download my thumbnail</a></p>"
                + "<p>This link is valid for 7 days.</p>",
            List.of()));

    log.info("Thumbnail {} sent to {}", thumbnailKey, event.getEmail());
  }
}
