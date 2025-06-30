package com.example.demo;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Value;

import java.util.HashMap;
import java.util.Map;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.rekognition.RekognitionClient;
import software.amazon.awssdk.services.rekognition.model.*;

@RestController
@RequestMapping("/face-liveness")
public class FaceLivenessController {
	
	private final String accessKey = System.getenv("AWS_ACCESS_KEY_ID");
	private final String secretKey = System.getenv("AWS_SECRET_ACCESS_KEY");

    private final RekognitionClient rekognition = RekognitionClient.builder()
            .region(Region.US_EAST_1)                          // your region
            .credentialsProvider(
                    StaticCredentialsProvider.create(
                            AwsBasicCredentials.create(
                                    accessKey,                      // ← hard‑coded
                                    secretKey                   // ← hard‑coded
                            )
                    )
            )
            .build();

    @PostMapping("/create-session")
    public ResponseEntity<Map<String, String>> createSession() {
        try {
            // Replace with your actual bucket name and (optional) prefix
            String bucketName    = "bucketliveness0731";
            String keyPrefix     = "face-liveness-session/";

            // Build the output config with required bucket (and prefix)
            LivenessOutputConfig outputConfig = LivenessOutputConfig.builder()
                    .s3Bucket(bucketName)           // **required**
                    .s3KeyPrefix(keyPrefix)         // optional, but recommended
                    .build();

            // Use the correct settings class from AWS SDK v2
            CreateFaceLivenessSessionRequestSettings sessionSettings = CreateFaceLivenessSessionRequestSettings.builder()
                    .outputConfig(outputConfig)
                    .build();

            CreateFaceLivenessSessionRequest request = CreateFaceLivenessSessionRequest.builder()
                    .settings(sessionSettings)
                    .build();

            CreateFaceLivenessSessionResponse response =
                    rekognition.createFaceLivenessSession(request);

            Map<String, String> result = new HashMap<>();
            result.put("sessionId", response.sessionId());
            result.put("region", "us-east-1");
			System.out.println(result);
            return ResponseEntity.status(HttpStatus.CREATED).body(result);

        } catch (Exception e) {
			e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }


    @GetMapping("/results")
    public ResponseEntity<Map<String, Object>> getResults(@RequestParam String sessionId) {
        try {
            GetFaceLivenessSessionResultsRequest request = GetFaceLivenessSessionResultsRequest.builder()
                    .sessionId(sessionId)
                    .build();

            GetFaceLivenessSessionResultsResponse response = rekognition.getFaceLivenessSessionResults(request);
	    System.out.println("Full response: " + response);

            Map<String, Object> result = new HashMap<>();
            result.put("status", response.statusAsString());
            result.put("confidence", response.confidence());
            result.put("isLive", response.confidence() != null && response.confidence() > 95);
	    System.out.println("RESULT: " + result);

            return ResponseEntity.ok(result);
        } catch (Exception e) {
			e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }
}
