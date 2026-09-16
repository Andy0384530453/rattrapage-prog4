package com.example.demo.handler;

import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.amazonaws.serverless.proxy.model.AwsProxyResponse;
import com.amazonaws.serverless.proxy.model.HttpApiV2ProxyRequest;
import com.amazonaws.serverless.proxy.spring.SpringBootLambdaContainerHandler;
import com.amazonaws.services.lambda.runtime.ClientContext;
import com.amazonaws.services.lambda.runtime.CognitoIdentity;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.example.demo.PojaApplication;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.sql.DriverManager;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Le chemin Lambda (adaptateur aws-serverless-java-container) ne se comporte pas comme Tomcat en
 * local : les champs multipart n'apparaissent pas dans le parameterMap du servlet. Ce test rejoue
 * une vraie requete multipart via le handler Lambda pour garantir que POST /submissions atteint
 * bien le controleur et persiste la soumission.
 */
class LambdaMultipartIT {

  private static final String BOUNDARY = "----lambdaMultipartBoundary";
  private static final String EMAIL = "lambda-adapter-check@example.com";
  private static final ObjectMapper OM = new ObjectMapper();

  @Test
  void post_submissions_with_multipart_file_and_email_reaches_the_controller() throws Exception {
    PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:13.9");
    postgres.start();
    System.setProperty("spring.datasource.url", postgres.getJdbcUrl());
    System.setProperty("spring.datasource.username", postgres.getUsername());
    System.setProperty("spring.datasource.password", postgres.getPassword());
    System.setProperty("aws.s3.bucket", "dummy-bucket");
    System.setProperty("aws.eventBridge.bus", "dummy-bus");

    var handler = SpringBootLambdaContainerHandler.getHttpApiV2ProxyHandler(PojaApplication.class);
    var response = invoke(handler, multipartBody(pngBytes()));

    // Sans le correctif, l'adaptateur renvoyait 502 (NullPointerException du RequestLogger) puis
    // 400 ("Required request parameter 'email'"), et la soumission n'était jamais persistée.
    assertFalse(
        response.contains("Required request parameter 'email'"),
        "le champ email multipart doit etre lisible via l'adaptateur Lambda, reponse=" + response);

    assertEquals(
        1,
        countSubmissions(postgres),
        "la soumission doit etre persistee avant l'upload S3, reponse=" + response);

    postgres.stop();
  }

  private String invoke(
      SpringBootLambdaContainerHandler<HttpApiV2ProxyRequest, AwsProxyResponse> handler,
      byte[] body)
      throws Exception {
    var event =
        Map.of(
            "version",
            "2.0",
            "routeKey",
            "POST /submissions",
            "rawPath",
            "/submissions",
            "rawQueryString",
            "",
            "headers",
            Map.of("content-type", "multipart/form-data; boundary=" + BOUNDARY),
            "cookies",
            List.of(),
            "requestContext",
            Map.of(
                "accountId", "123456789012",
                "apiId", "test",
                "domainName", "test.lambda-url.eu-west-3.on.aws",
                "domainPrefix", "test",
                "http",
                    Map.of(
                        "method", "POST",
                        "path", "/submissions",
                        "protocol", "HTTP/1.1",
                        "sourceIp", "127.0.0.1",
                        "userAgent", "junit"),
                "requestId", "test",
                "routeKey", "POST /submissions",
                "stage", "$default",
                "time", "16/Sep/2026:06:00:00 +0000",
                "timeEpoch", 0),
            "body",
            Base64.getEncoder().encodeToString(body),
            "isBase64Encoded",
            true);

    var output = new ByteArrayOutputStream();
    handler.proxyStream(
        new ByteArrayInputStream(OM.writeValueAsBytes(event)), output, fakeContext());
    return output.toString(UTF_8);
  }

  private byte[] multipartBody(byte[] fileBytes) throws Exception {
    var out = new ByteArrayOutputStream();
    out.write(("--" + BOUNDARY + "\r\n").getBytes(ISO_8859_1));
    out.write(
        "Content-Disposition: form-data; name=\"file\"; filename=\"photo.png\"\r\n"
            .getBytes(ISO_8859_1));
    out.write("Content-Type: image/png\r\n\r\n".getBytes(ISO_8859_1));
    out.write(fileBytes);
    out.write("\r\n".getBytes(ISO_8859_1));
    out.write(("--" + BOUNDARY + "\r\n").getBytes(ISO_8859_1));
    out.write("Content-Disposition: form-data; name=\"email\"\r\n\r\n".getBytes(ISO_8859_1));
    out.write((EMAIL + "\r\n").getBytes(ISO_8859_1));
    out.write(("--" + BOUNDARY + "--\r\n").getBytes(ISO_8859_1));
    return out.toByteArray();
  }

  private byte[] pngBytes() throws Exception {
    var image = new BufferedImage(20, 20, BufferedImage.TYPE_INT_RGB);
    var output = new ByteArrayOutputStream();
    ImageIO.write(image, "png", output);
    return output.toByteArray();
  }

  private int countSubmissions(PostgreSQLContainer postgres) throws Exception {
    try (var connection =
            DriverManager.getConnection(
                postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
        var resultSet =
            connection
                .createStatement()
                .executeQuery("select count(*) from submission where email = '" + EMAIL + "'")) {
      resultSet.next();
      return resultSet.getInt(1);
    }
  }

  private Context fakeContext() {
    return new Context() {
      @Override
      public String getAwsRequestId() {
        return "test";
      }

      @Override
      public String getLogGroupName() {
        return "test";
      }

      @Override
      public String getLogStreamName() {
        return "test";
      }

      @Override
      public String getFunctionName() {
        return "test";
      }

      @Override
      public String getFunctionVersion() {
        return "1";
      }

      @Override
      public String getInvokedFunctionArn() {
        return "test";
      }

      @Override
      public CognitoIdentity getIdentity() {
        return null;
      }

      @Override
      public ClientContext getClientContext() {
        return null;
      }

      @Override
      public int getRemainingTimeInMillis() {
        return 30000;
      }

      @Override
      public int getMemoryLimitInMB() {
        return 1024;
      }

      @Override
      public LambdaLogger getLogger() {
        return new LambdaLogger() {
          @Override
          public void log(String message) {}

          @Override
          public void log(byte[] message) {}
        };
      }
    };
  }
}
