package com.teamproject.report.archive.repository;

import com.teamproject.report.archive.model.ArchiveEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ArchiveEntryRepository extends JpaRepository<ArchiveEntry, UUID> {

    List<ArchiveEntry> findAllByUserIdOrderByUpdatedAtDesc(UUID userId);

    Optional<ArchiveEntry> findByIdAndUserId(UUID archiveId, UUID userId);

    Optional<ArchiveEntry> findByUserIdAndReportId(UUID userId, UUID reportId);

    void deleteAllByUserId(UUID userId);
}
