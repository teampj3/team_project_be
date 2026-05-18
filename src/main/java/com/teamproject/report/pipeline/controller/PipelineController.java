package com.teamproject.report.pipeline.controller;

import com.teamproject.report.pipeline.dto.PipelineResultResponse;
import com.teamproject.report.pipeline.dto.PipelineRunRequest;
import com.teamproject.report.pipeline.dto.PipelineRunResponse;
import com.teamproject.report.pipeline.service.PipelineService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/pipeline")
public class PipelineController {

    private final PipelineService pipelineService;

    public PipelineController(PipelineService pipelineService) {
        this.pipelineService = pipelineService;
    }

    @PostMapping("/run")
    public PipelineRunResponse run(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @Valid @RequestBody PipelineRunRequest request
    ) {
        return pipelineService.startRun(authorization, request.topic());
    }

    @GetMapping("/result")
    public PipelineResultResponse result(@RequestParam String runId) {
        return pipelineService.getResult(runId);
    }
}
