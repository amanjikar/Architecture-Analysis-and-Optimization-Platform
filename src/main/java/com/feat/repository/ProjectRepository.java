package com.feat.repository;
import org.springframework.data.jpa.repository.JpaRepository;

import com.feat.entity.Project;

public interface ProjectRepository extends JpaRepository<Project, Long> {

}
