package com.teamproject.report.report.controller;

import com.teamproject.report.archive.exception.ArchiveNotFoundException;
import com.teamproject.report.auth.exception.AuthException;
import com.teamproject.report.auth.exception.InvalidUpdateRequestException;
import com.teamproject.report.auth.exception.UserAlreadyExistsException;
import com.teamproject.report.pipeline.exception.PipelineRunNotFoundException;
import com.teamproject.report.pipeline.exception.PipelineStartException;
import com.teamproject.report.report.service.ReportNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(ReportNotFoundException.class)
    ProblemDetail handleReportNotFound(ReportNotFoundException e) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
        problem.setTitle("Report not found");
        problem.setDetail(e.getMessage());
        return problem;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail handleValidation(MethodArgumentNotValidException e) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problem.setTitle("Invalid request");
        problem.setDetail(e.getBindingResult().getAllErrors().getFirst().getDefaultMessage());
        return problem;
    }

    @ExceptionHandler(PipelineRunNotFoundException.class)
    ProblemDetail handlePipelineRunNotFound(PipelineRunNotFoundException e) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
        problem.setTitle("Pipeline run not found");
        problem.setDetail(e.getMessage());
        problem.setProperty("errorCode", "PIPELINE_RUN_NOT_FOUND");
        return problem;
    }

    @ExceptionHandler(PipelineStartException.class)
    ProblemDetail handlePipelineStartFailure(PipelineStartException e) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.BAD_GATEWAY);
        problem.setTitle("Pipeline start failed");
        problem.setDetail(e.getMessage());
        problem.setProperty("errorCode", "PIPELINE_START_FAILED");
        return problem;
    }

    @ExceptionHandler(UserAlreadyExistsException.class)
    ProblemDetail handleUserAlreadyExists(UserAlreadyExistsException e) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.CONFLICT);
        problem.setTitle("User already exists");
        problem.setDetail(e.getMessage());
        problem.setProperty("errorCode", "USER_ALREADY_EXISTS");
        return problem;
    }

    @ExceptionHandler(AuthException.class)
    ProblemDetail handleAuth(AuthException e) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.UNAUTHORIZED);
        problem.setTitle("Authentication failed");
        problem.setDetail(e.getMessage());
        problem.setProperty("errorCode", "AUTH_FAILED");
        return problem;
    }

    @ExceptionHandler(InvalidUpdateRequestException.class)
    ProblemDetail handleInvalidUpdateRequest(InvalidUpdateRequestException e) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problem.setTitle("Invalid update request");
        problem.setDetail(e.getMessage());
        problem.setProperty("errorCode", "INVALID_UPDATE_REQUEST");
        return problem;
    }

    @ExceptionHandler(ArchiveNotFoundException.class)
    ProblemDetail handleArchiveNotFound(ArchiveNotFoundException e) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
        problem.setTitle("Archive not found");
        problem.setDetail(e.getMessage());
        problem.setProperty("errorCode", "ARCHIVE_NOT_FOUND");
        return problem;
    }
}
