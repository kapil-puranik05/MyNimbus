package com.infra.mynimbus.repositories;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.infra.mynimbus.models.Deployment;

@Repository
public interface DeploymentRepository extends JpaRepository<Deployment, UUID>{
    @Query(value = "SELECT * FROM deployemnts d WHERE d.buildId = :buidId", nativeQuery = true)
    List<Deployment> getDeploymentsByBuildId(UUID buildId);
}
