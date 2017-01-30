package org.generationinitiative.uploader.dto;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
public class ResultDTO {
    private int statusCode = 200;
    private Map<String, String> headers = new HashMap<>();
    private String body = "{}";
}
