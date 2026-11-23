package com.feat.entity;
import java.time.LocalDateTime;
import jakarta.persistence.*;

@Entity
@Table(name = "projects")

public class Project {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	
	 private Long id;
	
	@Column(nullable=false,length=150)
	
	private String projectName;
	
    @Column(length = 1000)
    
	 private String description;
    
    @Column(length = 500)
    
    private String repositoryUrl;

    @Column(nullable = false, length = 100)
    
    private String projectType;
    
    @Column(nullable = false)
    private String status = "REGISTERED";
	
    private LocalDateTime createdAt;
    
    @Column(length = 1000)
    private String localPath;
    
    @PrePersist  //Executes before inserting a new record.
    
    public void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
    
    public Long getId() {
        return id;
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getRepositoryUrl() {
        return repositoryUrl;
    }

    public void setRepositoryUrl(String repositoryUrl) {
        this.repositoryUrl = repositoryUrl;
    }

    public String getProjectType() {
        return projectType;
    }

    public void setProjectType(String projectType) {
        this.projectType = projectType;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public String getLocalPath() {
        return localPath;
    }

    public void setLocalPath(String localPath) {
        this.localPath = localPath;
    }
}

