package com.teamproject.report.archive.exception;

import java.util.UUID;

public class ArchiveNotFoundException extends RuntimeException {

    public ArchiveNotFoundException(UUID archiveId) {
        super("Archive not found: " + archiveId);
    }
}
