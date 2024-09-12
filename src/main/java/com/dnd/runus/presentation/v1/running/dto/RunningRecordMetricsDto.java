package com.dnd.runus.presentation.v1.running.dto;

import com.dnd.runus.domain.common.Pace;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Duration;

public record RunningRecordMetricsDto(
        @Schema(description = "평균 페이스", example = "5'30''")
        Pace averagePace,
        @Schema(description = "멈춘 시간을 제외한 실제로 달린 시간", example = "123:45:56", format = "HH:mm:ss")
        Duration runningTime,
        @Schema(description = "달린 거리(m)", example = "1000")
        int distanceMeter,
        @Schema(description = "소모 칼로리(kcal)", example = "100")
        double calorie
) {
}
