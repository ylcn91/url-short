package com.urlshort.dto.link;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request DTO for bulk creation of short links.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request object for bulk creating multiple short links")
public class BulkCreateRequest {

    @NotEmpty(message = "URLs list cannot be empty")
    @Schema(description = "List of URLs to be shortened",
            example = "[\"https://example.com/page1\", \"https://example.com/page2\", \"https://example.com/page3\"]")
    private List<@org.hibernate.validator.constraints.URL(message = "All URLs must be valid") String> urls;
}
