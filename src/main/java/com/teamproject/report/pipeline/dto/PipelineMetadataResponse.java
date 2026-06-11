package com.teamproject.report.pipeline.dto;

import java.util.List;

public record PipelineMetadataResponse(
        String retrievalStatus,
        String relevanceMode,
        String writerMode,
        String note,
        List<PipelineStageInfoResponse> stages
) {

    public static PipelineMetadataResponse current() {
        return new PipelineMetadataResponse(
                "RAG 준비 코드 포함",
                "현재 운영 파이프라인은 keyword-based relevance",
                "Writer는 relevance 결과 기반 한국어 논문형 초안 생성",
                "VectorDB retrieval 실제 연결은 후속 작업",
                List.of(
                        new PipelineStageInfoResponse("search", "Search", "논문 검색 및 메타데이터 수집"),
                        new PipelineStageInfoResponse("reader", "Reader", "논문 초록 요약 생성"),
                        new PipelineStageInfoResponse("relevance", "Relevance", "관련성 점수 계산 및 선별"),
                        new PipelineStageInfoResponse("writer", "Writer", "한국어 논문형 초안 생성"),
                        new PipelineStageInfoResponse("review", "Review", "초안 품질 검토 및 보완"),
                        new PipelineStageInfoResponse("visualization", "Visualization", "시각자료 생성"),
                        new PipelineStageInfoResponse("archive", "Archive", "최종 산출물 저장"),
                        new PipelineStageInfoResponse("docx", "DOCX Export", "문서 내보내기")
                )
        );
    }
}
