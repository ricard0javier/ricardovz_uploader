package com.ricardovz.uploader.dto.auth0;

import lombok.Data;

import java.util.List;

@Data
public class TokenInfoResponseDTO {

    private List<String> roles;

}
