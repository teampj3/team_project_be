package com.teamproject.report.archive.controller;

import com.teamproject.report.archive.dto.ArchiveDetailResponse;
import com.teamproject.report.archive.dto.ArchiveResponse;
import com.teamproject.report.archive.dto.SaveArchiveRequest;
import com.teamproject.report.archive.service.ArchiveService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.DeleteMapping;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/archives")
public class ArchiveController {

    private final ArchiveService archiveService;

    public ArchiveController(ArchiveService archiveService) {
        this.archiveService = archiveService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ArchiveResponse save(
            @RequestHeader("Authorization") String authorization,
            @Valid @RequestBody SaveArchiveRequest request
    ) {
        return archiveService.save(authorization, request);
    }

    @GetMapping
    public List<ArchiveResponse> list(@RequestHeader("Authorization") String authorization) {
        return archiveService.list(authorization);
    }

    @GetMapping("/{archiveId}")
    public ArchiveDetailResponse get(
            @RequestHeader("Authorization") String authorization,
            @PathVariable UUID archiveId
    ) {
        return archiveService.getDetail(authorization, archiveId);
    }

    @DeleteMapping("/{archiveId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @RequestHeader("Authorization") String authorization,
            @PathVariable UUID archiveId
    ) {
        archiveService.delete(authorization, archiveId);
    }
}
