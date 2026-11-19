package com.feat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ProjectRequest(
	    @NotBlank(message = "Project name is required")
	    @Size(max = 150)
	    String projectName,

	    @Size(max = 1000)
	    String description,

	    @Size(max = 500)
	    String repositoryUrl,

	    @NotBlank(message = "Project type is required")
	    String projectType
	    ) {}
