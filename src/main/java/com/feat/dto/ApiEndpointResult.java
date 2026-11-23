
package com.feat.dto;

public record ApiEndpointResult(
    String className,
    String methodName,
    String httpMethod,
    String endpoint
) {}
