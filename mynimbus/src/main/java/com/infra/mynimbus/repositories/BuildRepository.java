package com.infra.mynimbus.repositories;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.infra.mynimbus.dtos.FileDeletionMetaData;
import com.infra.mynimbus.models.Build;

public interface BuildRepository extends JpaRepository<Build, UUID> {
    @Query(value = "SELECT * FROM builds b WHERE b.user_id = :userId", nativeQuery = true)
    List<Build> getBuildsByUserId(UUID userId);
    @Query(value = "SELECT b.zip_path, b.filename FROM builds b WHERE created_at < NOW() - INTERVAL '1 hour'", nativeQuery = true)
    List<FileDeletionMetaData> getOlderFiles();
    Optional<Build> findByImageName(String imageName);
}
