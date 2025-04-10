package com.example.ec2launcher.deployment.repository;

import com.example.ec2launcher.deployment.model.ProjectArchive;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProjectArchiveRepository extends JpaRepository<ProjectArchive, Long> {
}
