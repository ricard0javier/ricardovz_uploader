package com.ricardovz.uploader.dto.auth0;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class TokenInfoRequestDTO {

    @JsonProperty("id_token")
    private String idToken;

}
