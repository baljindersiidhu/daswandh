package com.daswandh.backend.storage;

import com.daswandh.backend.exception.ExternalServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.net.http.HttpClient;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;

@Component
public class CloudinaryClient {

    private static final Logger log = LoggerFactory.getLogger(CloudinaryClient.class);

    public record UploadedFile(String url, String publicId) {}

    private final RestClient restClient;
    private final String cloudName;
    private final String apiKey;
    private final String apiSecret;
    private final String folder;

    public CloudinaryClient(@Value("${app.cloudinary.cloud-name}") String cloudName,
                            @Value("${app.cloudinary.api-key}") String apiKey,
                            @Value("${app.cloudinary.api-secret}") String apiSecret,
                            @Value("${app.cloudinary.folder}") String folder,
                            @Value("${app.cloudinary.base-url}") String baseUrl) {
        this.cloudName = cloudName;
        this.apiKey = apiKey;
        this.apiSecret = apiSecret;
        this.folder = folder;

        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build());
        requestFactory.setReadTimeout(Duration.ofSeconds(30)); // uploads can take a little longer

        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .build();
    }

    // Signed upload. Cloudinary's rule: take the parameters being signed (here "folder" and "timestamp"),
    // sort them by name, join as name=value with &, append the API secret, and SHA-1 hash the result.
    public UploadedFile uploadImage(byte[] bytes, String filename) {
        long timestamp = Instant.now().getEpochSecond();
        String signature = Hashing.sha1Hex("folder=" + folder + "&timestamp=" + timestamp + apiSecret);

        ByteArrayResource fileResource = new ByteArrayResource(bytes) {
            @Override
            public String getFilename() {
                return filename; // multipart parts need a file name
            }
        };

        MultiValueMap<String, Object> form = new LinkedMultiValueMap<>();
        form.add("file", fileResource);
        form.add("api_key", apiKey);
        form.add("timestamp", String.valueOf(timestamp));
        form.add("folder", folder);
        form.add("signature", signature);

        try {
            Map<String, Object> response = restClient.post()
                    .uri("/{cloud}/image/upload", cloudName)
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(form)
                    .retrieve()
                    .body(new ParameterizedTypeReference<Map<String, Object>>() {});

            if (response != null
                    && response.get("secure_url") instanceof String url
                    && response.get("public_id") instanceof String publicId) {
                return new UploadedFile(url, publicId);
            }
            throw new ExternalServiceException("File storage returned an unexpected response.");
        } catch (RestClientException e) {
            log.error("Cloudinary upload failed", e); // never log the secret or the signature
            throw new ExternalServiceException("File storage is unavailable. Please try again.");
        }
    }
}