package com.ricardovz.uploader;

import com.amazonaws.services.lambda.runtime.ClientContext;
import com.amazonaws.services.lambda.runtime.CognitoIdentity;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.util.StringInputStream;
import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

@Slf4j
public class UploaderTest {

    private Uploader target;

    @Before
    public void setUp() throws Exception {
        target = new Uploader();
    }

    @Test
    public void test_handleRequest() throws Exception {

        String body = "{\n" +
                "    \"resource\": \"/ricardovz_uploader\",\n" +
                "    \"path\": \"/ricardovz_uploader\",\n" +
                "    \"httpMethod\": \"POST\",\n" +
                "    \"headers\": {\n" +
                "        \"Accept\": \"*/*\",\n" +
                "        \"CloudFront-Forwarded-Proto\": \"https\",\n" +
                "        \"CloudFront-Is-Desktop-Viewer\": \"true\",\n" +
                "        \"CloudFront-Is-Mobile-Viewer\": \"false\",\n" +
                "        \"CloudFront-Is-SmartTV-Viewer\": \"false\",\n" +
                "        \"CloudFront-Is-Tablet-Viewer\": \"false\",\n" +
                "        \"CloudFront-Viewer-Country\": \"GB\",\n" +
                "        \"Content-Type\": \"application/x-www-form-urlencoded\",\n" +
                "        \"Host\": \"6e89359u4k.execute-api.eu-central-1.amazonaws.com\",\n" +
                "        \"User-Agent\": \"curl/7.51.0\",\n" +
                "        \"Via\": \"1.1 64cab1877f302ea74232c74b1e181862.cloudfront.net (CloudFront)\",\n" +
                "        \"X-Amz-Cf-Id\": \"Vh721KbGfYNQGwzct7RLS0GiA0SP4pA91z3cfOxXG34Curdqbmtm5g==\",\n" +
                "        \"X-Forwarded-For\": \"90.192.116.137, 54.239.166.77\",\n" +
                "        \"X-Forwarded-Port\": \"443\",\n" +
                "        \"X-Forwarded-Proto\": \"https\"\n" +
                "    },\n" +
                "    \"queryStringParameters\": null,\n" +
                "    \"pathParameters\": null,\n" +
                "    \"stageVariables\": null,\n" +
                "    \"requestContext\": {\n" +
                "        \"accountId\": \"430132907316\",\n" +
                "        \"resourceId\": \"3uzpmx\",\n" +
                "        \"stage\": \"prod\",\n" +
                "        \"requestId\": \"d687774c-dfdc-11e6-82a8-37edf290889e\",\n" +
                "        \"identity\": {\n" +
                "            \"cognitoIdentityPoolId\": null,\n" +
                "            \"accountId\": null,\n" +
                "            \"cognitoIdentityId\": null,\n" +
                "            \"caller\": null,\n" +
                "            \"apiKey\": null,\n" +
                "            \"sourceIp\": \"90.192.116.137\",\n" +
                "            \"accessKey\": null,\n" +
                "            \"cognitoAuthenticationType\": null,\n" +
                "            \"cognitoAuthenticationProvider\": null,\n" +
                "            \"userArn\": null,\n" +
                "            \"userAgent\": \"curl/7.51.0\",\n" +
                "            \"user\": null\n" +
                "        },\n" +
                "        \"resourcePath\": \"/ricardovz_uploader\",\n" +
                "        \"httpMethod\": \"POST\",\n" +
                "        \"apiId\": \"6e89359u4k\"\n" +
                "    },\n" +
                "    \"body\": \"{\\\"token\\\" : \\\"eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJpc3MiOiJodHRwczovL3JpY2FyZDBqYXZpZXIuZXUuYXV0aDAuY29tLyIsInN1YiI6ImF1dGgwfDU4NzJiMTVjYmY4ZTYzMzI1MDY1OWEwZSIsImF1ZCI6IlIyZ1RBemJKeGh5NnZnVUQxMW51VEhGY3JteGR0VDJOIiwiZXhwIjoxNDg0ODE2MjIwLCJpYXQiOjE0ODQ3ODAyMjB9.RLuD2PC9yEDyiN5Wv-mXQPManoyFms3nFGwKxo2IvXo\\\",\\\"bucket\\\" : \\\"static.ricardovz.com\\\",\\\"key\\\" : \\\"data/test.json\\\",\\\"body\\\" : \\\"Hello world with JAVA\\\"}\",\n" +
                "    \"isBase64Encoded\": false\n" +
                "}";
        Context context = getContext();
        InputStream inputStream = new StringInputStream(body);
        OutputStream outputStream = new ByteArrayOutputStream(1024);
        target.handleRequest(inputStream, outputStream, context);

    }

    private Context getContext() {
        return new Context() {
            @Override
            public String getAwsRequestId() {
                return null;
            }

            @Override
            public String getLogGroupName() {
                return null;
            }

            @Override
            public String getLogStreamName() {
                return null;
            }

            @Override
            public String getFunctionName() {
                return null;
            }

            @Override
            public String getFunctionVersion() {
                return null;
            }

            @Override
            public String getInvokedFunctionArn() {
                return null;
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
                return 0;
            }

            @Override
            public int getMemoryLimitInMB() {
                return 0;
            }

            @Override
            public LambdaLogger getLogger() {
                return new LambdaLogger() {
                    @Override
                    public void log(String string) {
                        log.info(string);
                    }
                };
            }
        };
    }
}