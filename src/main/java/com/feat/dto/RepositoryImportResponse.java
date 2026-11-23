package com.feat.dto;

public record RepositoryImportResponse(
	    Long projectId,
	    String projectName,
	    String status,
	    String localPath,
	    String message
	) {}
