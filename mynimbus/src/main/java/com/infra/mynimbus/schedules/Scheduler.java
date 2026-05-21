package com.infra.mynimbus.schedules;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.infra.mynimbus.repositories.BuildRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class Scheduler {
    private final BuildRepository buildRepository;

    @Scheduled(fixedDelay = 7200000)
    public void cleanFailedEntries() {
        buildRepository.deleteFailedBuilds();
    }
}
