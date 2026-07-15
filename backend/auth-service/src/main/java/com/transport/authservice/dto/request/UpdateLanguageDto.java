package com.transport.authservice.dto.request;


import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class UpdateLanguageDto {

    @Pattern(regexp = "^(en|hi)$", message="Language must be 'en' | 'hi'")
    private String preferredLanguage;
}
