
package com.feat.dto;

import java.util.List;

public record CodeAnalysisResponse(

    Long projectId,
    String projectName,

    int totalJavaFiles,
    int totalClasses,
    int totalInterfaces,
    int totalMethods,

    int successfullyParsedFiles,
    int failedFiles,

    List<JavaFileResult> files,
    List<String> failedFilePaths,

    List<ApiEndpointResult> apiEndpoints

) {

    public record JavaFileResult(
        String filePath,
        String packageName,
        List<String> imports,
        List<TypeResult> types
    ) {}

    public record TypeResult(
        String name,
        String type,
        List<String> annotations,
        List<MethodResult> methods
    ) {}

    public record MethodResult(
        String name,
        String returnType,
        int parameterCount,
        List<String> annotations
    ) {}
}
