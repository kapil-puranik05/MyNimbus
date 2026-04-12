package com.infra.mynimbus.models;

import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.infra.mynimbus.util.BuildStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "builds", indexes = {@Index(name = "idx_build_user", columnList = "user_id")})
@Data
public class Build {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID buildId;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JsonIgnore
    private AppUser user;
    @Column(nullable = false, unique = true)
    private String imageName;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BuildStatus status;
    @Column(nullable = false)
    private String zipPath;
    @Column(nullable = false)
    private String filename;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    public void create() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    public void update() {
        updatedAt = LocalDateTime.now();
    }
}
