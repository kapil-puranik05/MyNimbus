package com.infra.mynimbus.models;

import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import com.infra.mynimbus.util.DeploymentStatus;

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
@Table(name = "deployments", indexes = {@Index(name = "idx_deployment_build", columnList = "build_id")})
@Data
public class Deployment {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID deploymentId;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "build_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Build build;
    @Column(nullable = false, unique = true)
    private String containerId;
    @Column(nullable = false)
    private int hostPort;
    @Column(nullable = false)
    private int containerPort;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DeploymentStatus status;
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
