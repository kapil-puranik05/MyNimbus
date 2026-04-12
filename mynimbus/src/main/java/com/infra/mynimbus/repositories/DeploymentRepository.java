package com.infra.mynimbus.repositories;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.infra.mynimbus.models.Deployment;

@Repository
public interface DeploymentRepository extends JpaRepository<Deployment, UUID>{
    @Query(value = "SELECT * FROM deployments d WHERE d.build_id = :buildId", nativeQuery = true)
    List<Deployment> getDeploymentsByBuildId(UUID buildId);
    Optional<Deployment> findByContainerId(String containerId);
}
