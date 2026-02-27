package com.urlshort.dto.workspace;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Request DTO for updating workspace settings.
 * All fields are optional - only provided fields will be updated.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request object for updating workspace settings")
public class UpdateWorkspaceRequest {

    @Size(max = 255, message = "Workspace name must not exceed 255 characters")
    @Schema(description = "New workspace name", example = "Acme Corporation Updated")
    private String name;

    @Schema(description = "Updated workspace settings and configuration")
    private Map<String, Object> settings;
}
