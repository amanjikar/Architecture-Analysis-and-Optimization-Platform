package com.feat.service;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.feat.repository.ProjectRepository;
import com.feat.dto.*;
import com.feat.entity.*;

@Service
@Transactional
public class ProjectService {
	 private final ProjectRepository projectRepository;

	    public ProjectService(ProjectRepository projectRepository) {
	        this.projectRepository = projectRepository;
	    }

	    public Project createProject(ProjectRequest request) {

	        Project project = new Project();

	        project.setProjectName(request.projectName());
	        project.setDescription(request.description());
	        project.setRepositoryUrl(request.repositoryUrl());
	        project.setProjectType(request.projectType());

	        return projectRepository.save(project);
	    }

	    @Transactional(readOnly = true)
	    public List<Project> getAllProjects() {
	        return projectRepository.findAll();
	    }

	    @Transactional(readOnly = true)
	    public Project getProjectById(Long id) {

	        return projectRepository.findById(id)
	            .orElseThrow(() ->
	                new ResponseStatusException(
	                    HttpStatus.NOT_FOUND,
	                    "Project not found"
	                )
	            );
	    }

	    public void deleteProject(Long id) {

	        if (!projectRepository.existsById(id)) {
	            throw new ResponseStatusException(
	                HttpStatus.NOT_FOUND,
	                "Project not found"
	            );
	        }

	        projectRepository.deleteById(id);
	    }

}
