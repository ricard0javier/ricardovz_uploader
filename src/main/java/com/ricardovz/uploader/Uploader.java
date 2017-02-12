package com.ricardovz.uploader;

import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.util.StringInputStream;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.ricardovz.uploader.dto.RequestDTO;
import com.ricardovz.uploader.dto.ResponseDTO;
import com.ricardovz.uploader.dto.auth0.TokenInfoRequestDTO;
import com.ricardovz.uploader.dto.auth0.TokenInfoResponseDTO;
import lombok.SneakyThrows;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;

import java.io.InputStream;
import java.io.UnsupportedEncodingException;

import static java.net.HttpURLConnection.HTTP_UNAUTHORIZED;

public class Uploader implements RequestHandler<RequestDTO, ResponseDTO> {

    private LambdaLogger logger;
    private ObjectMapper objectMapper;
    private String jwtSecret = "default";
    private String jwtIssuer = "default";
    private String jwtAudience = "default";

    private final AmazonS3 s3;

    public Uploader() {

        // use the JWT Secret from the System environment properties
        String systemSecret = System.getenv("JWT_CLIENT_SECRET");
        if (systemSecret != null) {
            jwtSecret = systemSecret;
        }

        // use the JWT Issuer from the System environment properties
        String systemJwtIssuer = System.getenv("JWT_ISSUER");
        if (systemJwtIssuer != null) {
            jwtIssuer = systemJwtIssuer;
        }

        // use the JWT Audience from the System environment properties
        String systemJwtAudience = System.getenv("JWT_AUDIENCE");
        if (systemJwtAudience != null) {
            jwtAudience = systemJwtAudience;
        }

        // use the JWT Audience from the System environment properties
        String systemAwsRegion = System.getenv("AWS_REGION");
        String awsRegion = "eu-central-1";
        if (systemAwsRegion != null) {
            awsRegion = systemAwsRegion;
        }

        // configure the s3 client
        Regions regions = Regions.fromName(awsRegion);

        s3 = new AmazonS3Client();
        s3.setRegion(Region.getRegion(regions));
    }

    @Override
    public ResponseDTO handleRequest(RequestDTO request, Context context) {
        LambdaLogger logger = getLogger(context);
        return process(request, logger);
    }

    private ObjectMapper getObjectMapper() {
        if (objectMapper != null) {
            return objectMapper;
        }
        objectMapper = new ObjectMapper();
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        objectMapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
        return objectMapper;
    }

    private LambdaLogger getLogger(Context context) {
        if (logger != null) {
            return logger;
        }
        logger = context.getLogger();
        return logger;
    }

    @SneakyThrows
    private ResponseDTO process(RequestDTO requestDTO, LambdaLogger logger) {
        logger.log("requestBody:\n" + requestDTO);

        if (!isTokenValid(requestDTO.getToken())) {

            logger.log("Token not valid");
            return ResponseDTO.builder()
                    .statusCode(HTTP_UNAUTHORIZED)
                    .build();

        }

        if (!isAuthorised(requestDTO.getToken(), requestDTO.getBucket())) {

            logger.log("The user does not have enough permissions");
            return ResponseDTO.builder()
                    .statusCode(HTTP_UNAUTHORIZED)
                    .build();

        }

        StringInputStream stringInputStream = new StringInputStream(requestDTO.getBody());

        ObjectMetadata metadata = new ObjectMetadata();
        byte[] resultByte = DigestUtils.md5(stringInputStream);
        String streamMD5 = new String(Base64.encodeBase64(resultByte));
        metadata.setContentMD5(streamMD5);
        metadata.setContentLength(requestDTO.getBody().getBytes().length);

        // avoid leaving the stream in the last byte position
        stringInputStream.reset();

        logger.log("uploading to bucket '" + requestDTO.getBucket() + "', with key '" + requestDTO.getKey() + "'");
        s3.putObject(requestDTO.getBucket(), requestDTO.getKey(), stringInputStream, metadata);

        return ResponseDTO.builder().build();
    }

    private boolean isTokenValid(String token) throws UnsupportedEncodingException {
        try {
            byte[] decodedJwtSecret = Base64.decodeBase64(jwtSecret);
            JWT.require(Algorithm.HMAC256(decodedJwtSecret))
                    .withIssuer(jwtIssuer)
                    .withAudience(jwtAudience)
                    .build()
                    .verify(token);
        } catch (JWTVerificationException exception) {
            return false;
        }
        return true;
    }

    private boolean isAuthorised(String idToken, String bucketName) {
        try {
            TokenInfoRequestDTO request = new TokenInfoRequestDTO();
            request.setIdToken(idToken);

            String entityString = getObjectMapper().writeValueAsString(request);
            HttpEntity entity = new StringEntity(entityString);

            HttpPost httpRequest = new HttpPost(jwtIssuer + "tokeninfo");
            httpRequest.setHeader("Content-type", "application/json");
            httpRequest.setEntity(entity);

            HttpClient httpClient = HttpClientBuilder.create().build();
            HttpResponse httpResponse = httpClient.execute(httpRequest);
            InputStream httpResponseBody = httpResponse.getEntity().getContent();

            TokenInfoResponseDTO response = getObjectMapper().readValue(httpResponseBody, TokenInfoResponseDTO.class);

            return response.getRoles().contains("upload:" + bucketName);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
}
