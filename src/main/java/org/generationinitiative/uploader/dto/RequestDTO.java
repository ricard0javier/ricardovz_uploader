package org.generationinitiative.uploader.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RequestDTO {
    private String token;
    private String bucket;
    private String key;
    private String body;
}
