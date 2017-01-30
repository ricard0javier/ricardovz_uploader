package org.generationinitiative.uploader.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class Auth0TokenInfoRequestDTO {

    @JsonProperty("id_token")
    private String idToken;

}
