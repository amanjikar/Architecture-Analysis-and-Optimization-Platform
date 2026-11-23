
package com.feat.service;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.feat.dto.RepositoryImportResponse;
import com.feat.entity.Project;
import com.feat.repository.ProjectRepository;

@Service
public class RepositoryImportService {

    private final ProjectRepository projectRepository;

    @Value("${app.repository-storage}")
    private String storageLocation;

    public RepositoryImportService(
            ProjectRepository projectRepository) {
        this.projectRepository = projectRepository;
    }

    @Transactional
    public RepositoryImportResponse importRepository(Long projectId) {

        Project project = projectRepository.findById(projectId)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Project not found"));

        String repoUrl = project.getRepositoryUrl();

        validateGitHubUrl(repoUrl);

        if ("IMPORTING".equals(project.getStatus())) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Repository import is already in progress");
        }

        Path storageRoot =
            Paths.get(storageLocation).toAbsolutePath().normalize();

        Path destination = storageRoot
            .resolve("project-" + projectId)
            .normalize();

        if (!destination.startsWith(storageRoot)) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Invalid repository destination");
        }

        if (Files.exists(destination)) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Repository already exists in storage");
        }

        try {
            Files.createDirectories(storageRoot);

            project.setStatus("IMPORTING");
            projectRepository.save(project);

            try (Git git = Git.cloneRepository()
                    .setURI(repoUrl)
                    .setDirectory(destination.toFile())
                    .setDepth(1)
                    .call()) {

                // Repository cloned successfully
            }

            project.setLocalPath(destination.toString());
            project.setStatus("IMPORTED");

            projectRepository.save(project);

            return new RepositoryImportResponse(
                project.getId(),
                project.getProjectName(),
                project.getStatus(),
                project.getLocalPath(),
                "GitHub repository imported successfully"
            );

        } catch (GitAPIException | IOException e) {

            project.setStatus("IMPORT_FAILED");
            projectRepository.save(project);

            try {
                if (Files.exists(destination)) {
                    try (var paths = Files.walk(destination)) {
                        paths.sorted(java.util.Comparator.reverseOrder())
                            .forEach(path -> {
                                try {
                                    Files.deleteIfExists(path);
                                } catch (IOException ex) {
                                    throw new RuntimeException(ex);
                                }
                            });
                    }
                }
            } catch (Exception cleanupError) {
                e.addSuppressed(cleanupError);
            }

            throw new ResponseStatusException(
                HttpStatus.BAD_GATEWAY,
                "Repository import failed. Check the URL and repository access.",
                e
            );
        }
    }

    private void validateGitHubUrl(String repoUrl) {

        if (repoUrl == null || repoUrl.isBlank()) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "GitHub repository URL is required");
        }

        try {
            URI uri = URI.create(repoUrl.trim());

            boolean valid =
                "https".equalsIgnoreCase(uri.getScheme())
                && "github.com".equalsIgnoreCase(uri.getHost())
                && uri.getUserInfo() == null
                && uri.getQuery() == null
                && uri.getFragment() == null
                && uri.getPath().matches(
                    "^/[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+(?:\\.git)?/?$"
                );

            if (!valid) {
                throw new IllegalArgumentException("Invalid URL");
            }

        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Please provide a valid public GitHub HTTPS repository URL");
        }
    }
}
