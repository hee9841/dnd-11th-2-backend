package com.dnd.runus.domain.running;

import com.dnd.runus.domain.member.Member;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface RunningRecordRepository {
    Optional<RunningRecord> findById(long runningRecordId);

    RunningRecord save(RunningRecord runningRecord);

    void deleteByMemberId(long memberId);

    List<RunningRecord> findByMemberIdAndStartAtBetween(
            long memberId, OffsetDateTime startTime, OffsetDateTime endTime);

    boolean hasByMemberIdAndStartAtBetween(long memberId, OffsetDateTime startTime, OffsetDateTime endTime);

    int findTotalDistanceMeterByMemberId(long memberId, OffsetDateTime startDate, OffsetDateTime endDate);

    List<RunningRecord> findByMember(Member member);

    List<RunningRecordWeeklySummary> findWeeklyDistanceSummaryMeter(long memberId, OffsetDateTime today);
}
