package com.infra.mynimbus.schedules;

import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.infra.mynimbus.dtos.FileDeletionMetaData;
import com.infra.mynimbus.repositories.BuildRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class Scheduler {
    private final BuildRepository buildRepository;

    @Scheduled(fixedRate = 3600000)
    public void runTask() {
        List<FileDeletionMetaData> olderFiles = buildRepository.getOlderFiles();
        System.out.println("Executing Zip deletion job");
        for(FileDeletionMetaData data : olderFiles) {
            Path filePath = Paths.get(data.getZipPath(), data.getFilename());
            System.out.println(data.getZipPath() + "/" + data.getFilename());
            try {
                Files.delete(filePath);
                System.out.println("File " + data.getFilename() + " deleted successfully");
            } catch(NoSuchFileException e) {
                System.out.println("File does not exist");
            } catch(DirectoryNotEmptyException e) {
                System.out.println("Directory is not empty");
            } catch(Exception e) {
                System.out.println("Error occured while deleting the file");
            }
        }
    }
}
