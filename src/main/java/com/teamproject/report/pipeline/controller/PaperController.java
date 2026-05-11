package com.teamproject.report.pipeline.controller;

import com.teamproject.report.pipeline.dto.RelevancePaperResponse;
import com.teamproject.report.pipeline.dto.SearchPaperResponse;
import com.teamproject.report.pipeline.service.PipelineService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/papers")
public class PaperController {

    private final PipelineService pipelineService;

    public PaperController(PipelineService pipelineService) {
        this.pipelineService = pipelineService;
    }

    @GetMapping("/search-results")
    public List<SearchPaperResponse> searchResults(@RequestParam String runId) {
        return pipelineService.getSearchResults(runId);
    }

    @GetMapping("/relevance-results")
    public List<RelevancePaperResponse> relevanceResults(@RequestParam String runId) {
        return pipelineService.getRelevanceResults(runId);
    }
}
