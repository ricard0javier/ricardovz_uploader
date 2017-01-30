package com.ricardovz.uploader;

import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.util.StringInputStream;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.ricardovz.uploader.dto.Auth0TokenInfoRequestDTO;
import com.ricardovz.uploader.dto.Auth0TokenInfoResponseDTO;
import com.ricardovz.uploader.dto.RequestDTO;
import com.ricardovz.uploader.dto.ResultDTO;
import lombok.SneakyThrows;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;

import java.io.*;
import java.nio.charset.Charset;

import static java.net.HttpURLConnection.HTTP_UNAUTHORIZED;

public class Uploader implements RequestStreamHandler {

    // constants
    private static final String DEFAULT_BODY = "{}";
    private static final Charset STREAMS_ENCODING = Charset.forName("UTF-8");

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
    public void handleRequest(InputStream input, OutputStream output, Context context) throws IOException {
        LambdaLogger logger = getLogger(context);

        String inputString = convertToString(input);

        String body = extract(inputString, "body");

        String result = process(body, logger);

        result = result == null ? DEFAULT_BODY : result;

        output.write(result.getBytes(STREAMS_ENCODING));

    }

    private String convertToString(InputStream inputStream) throws IOException {
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int length;
        while ((length = inputStream.read(buffer)) != -1) {
            result.write(buffer, 0, length);
        }
        return result.toString(STREAMS_ENCODING.displayName());
    }

    private String extract(String inputString, String nodeKey) throws IOException {
        ObjectMapper objectMapper = getObjectMapper();
        JsonNode jsonNode = objectMapper.readTree(inputString);
        if (jsonNode == null || !jsonNode.has(nodeKey)) {
            return DEFAULT_BODY;
        }
        return jsonNode.findValue(nodeKey).asText(DEFAULT_BODY);
    }

    private ObjectMapper getObjectMapper() {
        if (objectMapper != null) {
            return objectMapper;
        }
        objectMapper = new ObjectMapper();
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        objectMapper.enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
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
    public String process(String requestBody, LambdaLogger logger) {
        logger.log("requestBody:\n" + requestBody);

        ResultDTO resultDTO = new ResultDTO();

        ObjectMapper objectMapper = getObjectMapper();

        RequestDTO request = objectMapper.readValue(requestBody, RequestDTO.class);

        if (!isTokenValid(request.getToken())) {

            resultDTO.setStatusCode(HTTP_UNAUTHORIZED);
            logger.log("Token not valid");

            return objectMapper.writeValueAsString(resultDTO);
        }

        if (!isAuthorised(request.getToken(), request.getBucket())) {

            resultDTO.setStatusCode(HTTP_UNAUTHORIZED);
            logger.log("The user does not have enough permissions");

            return objectMapper.writeValueAsString(resultDTO);
        }

        StringInputStream stringInputStream = new StringInputStream(request.getBody());

        ObjectMetadata metadata = new ObjectMetadata();
        byte[] resultByte = DigestUtils.md5(stringInputStream);
        String streamMD5 = new String(Base64.encodeBase64(resultByte));
        metadata.setContentMD5(streamMD5);
        metadata.setContentLength(request.getBody().getBytes().length);

        // avoid leaving the stream in the last byte position
        stringInputStream.reset();

        logger.log("uploading to bucket '" + request.getBucket() + "', with key '" + request.getKey() + "'");
        s3.putObject(request.getBucket(), request.getKey(), stringInputStream, metadata);

        return objectMapper.writeValueAsString(resultDTO);
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
            Auth0TokenInfoRequestDTO auth0TokenInfoRequestDTO = new Auth0TokenInfoRequestDTO();
            auth0TokenInfoRequestDTO.setIdToken(idToken);

            String entityString = getObjectMapper().writeValueAsString(auth0TokenInfoRequestDTO);
            HttpEntity entity = new StringEntity(entityString);

            HttpPost request = new HttpPost(jwtIssuer + "tokeninfo");
            request.setHeader("Content-type", "application/json");
            request.setEntity(entity);

            HttpClient httpClient = HttpClientBuilder.create().build();
            HttpResponse response = httpClient.execute(request);

            Auth0TokenInfoResponseDTO auth0TokenInfoResponseDTO = getObjectMapper().readValue(response.getEntity().getContent(), Auth0TokenInfoResponseDTO.class);

            return auth0TokenInfoResponseDTO.getRoles().contains("upload:" + bucketName);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
}
