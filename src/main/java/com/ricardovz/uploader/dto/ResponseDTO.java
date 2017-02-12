package com.ricardovz.uploader.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ResponseDTO {
    private int statusCode = 200;
}
