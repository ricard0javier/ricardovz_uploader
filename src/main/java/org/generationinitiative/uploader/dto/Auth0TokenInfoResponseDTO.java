package org.generationinitiative.uploader.dto;

import lombok.Data;

import java.util.List;

@Data
public class Auth0TokenInfoResponseDTO {

    private List<String> roles;

}
