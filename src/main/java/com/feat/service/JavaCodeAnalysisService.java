
package com.feat.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.github.javaparser.ParseProblemException;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;

import com.feat.dto.CodeAnalysisResponse;
import com.feat.entity.Project;
import com.feat.repository.ProjectRepository;

import com.feat.dto.ApiEndpointResult;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.MemberValuePair;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.expr.ArrayInitializerExpr;


@Service
public class JavaCodeAnalysisService {

    private final ProjectRepository projectRepository;

    public JavaCodeAnalysisService(
            ProjectRepository projectRepository) {
        this.projectRepository = projectRepository;
    }

    public CodeAnalysisResponse analyzeProject(Long projectId) {
    	
    	List<ApiEndpointResult> apiEndpoints = new ArrayList<>();
    	
        // 1. Find project in database
        Project project = projectRepository.findById(projectId)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Project not found"));

        // 2. Get imported repository path
        if (!"IMPORTED".equals(project.getStatus())
                || project.getLocalPath() == null
                || project.getLocalPath().isBlank()) {

            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Please import the repository first");
        }

        Path repositoryPath = Paths.get(
            project.getLocalPath()
        ).toAbsolutePath().normalize();

        if (!Files.isDirectory(repositoryPath)) {
            throw new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Imported repository folder not found");
        }

        // 3. Prepare analysis result lists
        List<CodeAnalysisResponse.JavaFileResult> fileResults =
            new ArrayList<>();

        List<String> failedFiles = new ArrayList<>();

        int totalFiles = 0;
        int totalClasses = 0;
        int totalInterfaces = 0;
        int totalMethods = 0;

        // 4. Find and analyse Java files
        try (Stream<Path> paths = Files.walk(repositoryPath)) {

            List<Path> javaFiles = paths
                .filter(Files::isRegularFile)
                .filter(path -> path.toString().endsWith(".java"))
                .filter(path -> !isExcluded(
                    repositoryPath.relativize(path)))
                .toList();

            totalFiles = javaFiles.size();

            for (Path javaFile : javaFiles) {

                try {

                    // Parse Java source code
                    CompilationUnit cu =
                        StaticJavaParser.parse(javaFile);

                    String packageName = cu.getPackageDeclaration()
                        .map(p -> p.getNameAsString())
                        .orElse("");

                    // Extract imports
                    List<String> imports = cu.getImports()
                        .stream()
                        .map(i -> i.getNameAsString()
                            + (i.isAsterisk() ? ".*" : ""))
                        .toList();

                    List<CodeAnalysisResponse.TypeResult> types =
                        new ArrayList<>();

                    // Extract classes and interfaces
                    for (ClassOrInterfaceDeclaration declaration :
                            cu.findAll(
                                ClassOrInterfaceDeclaration.class)) {

                        List<String> annotations =
                            declaration.getAnnotations()
                                .stream()
                                .map(a -> a.getNameAsString())
                                .toList();

                        String type = declaration.isInterface()
                            ? "INTERFACE" : "CLASS";

                        if (declaration.isInterface()) {
                            totalInterfaces++;
                        } else {
                            totalClasses++;
                        }
                     // Extract class-level API path
                        String classPath = getAnnotationPath(
                            declaration.getAnnotations(),
                            "RequestMapping"
                        );

                        List<CodeAnalysisResponse.MethodResult>
                            methods = new ArrayList<>();

                        // Extract methods
                        for (MethodDeclaration method :
                                declaration.getMethods()) {

                            List<String> methodAnnotations =
                                method.getAnnotations()
                                    .stream()
                                    .map(a -> a.getNameAsString())
                                    .toList();

                            methods.add(
                                new CodeAnalysisResponse.MethodResult(
                                    method.getNameAsString(),
                                    method.getTypeAsString(),
                                    method.getParameters().size(),
                                    methodAnnotations
                                )
                            );

                            totalMethods++;
                         // Extract REST API endpoint
                            for (AnnotationExpr annotation : method.getAnnotations()) {

                                String annotationName = annotation.getNameAsString();

                                String httpMethod = switch (annotationName) {
                                    case "GetMapping" -> "GET";
                                    case "PostMapping" -> "POST";
                                    case "PutMapping" -> "PUT";
                                    case "DeleteMapping" -> "DELETE";
                                    case "PatchMapping" -> "PATCH";
                                    default -> null;
                                };

                                if (httpMethod != null) {

                                    String methodPath = getAnnotationPath(
                                        List.of(annotation),
                                        annotationName
                                    );

                                    String endpoint = combinePaths(classPath, methodPath);

                                    apiEndpoints.add(
                                        new ApiEndpointResult(
                                            declaration.getNameAsString(),
                                            method.getNameAsString(),
                                            httpMethod,
                                            endpoint
                                        )
                                    );
                                }
                            }
                        }
                     // Extract REST API endpoint

                     


                        types.add(
                            new CodeAnalysisResponse.TypeResult(
                                declaration.getNameAsString(),
                                type,
                                annotations,
                                methods
                            )
                        );
                    }

                    // Save extracted file information
                    String relativePath = repositoryPath
                        .relativize(javaFile)
                        .toString();

                    fileResults.add(
                        new CodeAnalysisResponse.JavaFileResult(
                            relativePath,
                            packageName,
                            imports,
                            types
                        )
                    );

                } catch (IOException | ParseProblemException e) {

                    failedFiles.add(
                        repositoryPath.relativize(javaFile).toString()
                    );
                }
            }

        } catch (IOException e) {

            throw new ResponseStatusException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Unable to scan repository files",
                e
            );
        }

        // 5. Return complete analysis report
        return new CodeAnalysisResponse(
        	    project.getId(),
        	    project.getProjectName(),
        	    totalFiles,
        	    totalClasses,
        	    totalInterfaces,
        	    totalMethods,
        	    fileResults.size(),
        	    failedFiles.size(),
        	    fileResults,
        	    failedFiles,
        	    apiEndpoints
        	);
    }

private String getAnnotationPath(
        List<AnnotationExpr> annotations,
        String annotationName) {

    for (AnnotationExpr annotation : annotations) {

        if (!annotation.getNameAsString()
                .equals(annotationName)) {
            continue;
        }

        if (annotation.isSingleMemberAnnotationExpr()) {
            return getStringValue(
                annotation.asSingleMemberAnnotationExpr()
                    .getMemberValue()
            );
        }

        if (annotation.isNormalAnnotationExpr()) {

            for (MemberValuePair pair :
                    annotation.asNormalAnnotationExpr()
                        .getPairs()) {

                if (pair.getNameAsString().equals("value")
                        || pair.getNameAsString().equals("path")) {

                    return getStringValue(pair.getValue());
                }
            }
        }
    }

    return "";
}

private String getStringValue(
        com.github.javaparser.ast.expr.Expression expression) {

    if (expression.isStringLiteralExpr()) {
        return expression.asStringLiteralExpr()
            .asString();
    }

    if (expression.isArrayInitializerExpr()) {

        return expression.asArrayInitializerExpr()
            .getValues()
            .stream()
            .filter(v -> v.isStringLiteralExpr())
            .map(v -> v.asStringLiteralExpr().asString())
            .findFirst()
            .orElse("");
    }

    return "";
}

private String combinePaths(String classPath, String methodPath) {

    String first = classPath == null ? "" : classPath;
    String second = methodPath == null ? "" : methodPath;

    String result = (first + "/" + second)
        .replaceAll("/+", "/");

    if (result.isEmpty()) {
        return "/";
    }

    return result.startsWith("/") ? result : "/" + result;
}


    // Ignore generated and unnecessary directories
    private boolean isExcluded(Path relativePath) {

        for (Path part : relativePath) {

            String folder = part.toString();

            if (folder.equals(".git")
                    || folder.equals("target")
                    || folder.equals("build")
                    || folder.equals("node_modules")) {

                return true;
            }
        }

        return false;
    }
}
