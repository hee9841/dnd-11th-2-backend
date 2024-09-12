package com.dnd.runus.infrastructure.persistence.jooq.running;

import com.dnd.runus.domain.running.RunningRecordWeeklySummary;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.Record1;
import org.jooq.RecordMapper;
import org.jooq.impl.SQLDataType;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.dnd.runus.jooq.Tables.RUNNING_RECORD;
import static org.jooq.impl.DSL.cast;
import static org.jooq.impl.DSL.select;
import static org.jooq.impl.DSL.sum;

@Slf4j
@Repository
@RequiredArgsConstructor
public class JooqRunningRecordRepository {
    private final DSLContext dsl;

    public int findTotalDistanceMeterByMemberId(long memberId, OffsetDateTime startDate, OffsetDateTime endDate) {
        Record1<Integer> result = dsl.select(sum(RUNNING_RECORD.DISTANCE_METER).cast(Integer.class))
                .from(RUNNING_RECORD)
                .where(RUNNING_RECORD.MEMBER_ID.eq(memberId))
                .and(RUNNING_RECORD.START_AT.ge(startDate))
                .and(RUNNING_RECORD.START_AT.lessThan(endDate))
                .fetchOne();
        if (result != null && result.value1() != null) {
            return result.value1();
        }
        return 0;
    }

    public List<RunningRecordWeeklySummary> findWeeklyDistanceSummaryMeter(
            long memberId, OffsetDateTime startWeekDate) {

        List<RunningRecordWeeklySummary> fetch = select(
                        cast(RUNNING_RECORD.START_AT, SQLDataType.DATE).as("date"),
                        sum(RUNNING_RECORD.DISTANCE_METER).cast(Integer.class).as("sum_distance"))
                .from(RUNNING_RECORD)
                .where(RUNNING_RECORD.MEMBER_ID.eq(memberId))
                .and(RUNNING_RECORD.START_AT.ge(startWeekDate))
                .and(RUNNING_RECORD.START_AT.lt(startWeekDate.plusDays(7)))
                .groupBy(cast(RUNNING_RECORD.START_AT, SQLDataType.DATE))
                .orderBy(cast(RUNNING_RECORD.START_AT, SQLDataType.DATE))
                .fetch(new RunningWeeklyDistanceSummary());

        Set<LocalDate> selectDataSet =
                fetch.stream().map(RunningRecordWeeklySummary::date).collect(Collectors.toSet());

        for (int i = 0; i < 7; i++) {
            LocalDate dddd = startWeekDate.plusDays(i).toLocalDate();
            if (!selectDataSet.contains(dddd)) {
                fetch.add(new RunningRecordWeeklySummary(dddd));
            }
        }

        return fetch;
    }

    private static class RunningWeeklyDistanceSummary implements RecordMapper<Record, RunningRecordWeeklySummary> {

        @Override
        public RunningRecordWeeklySummary map(Record record) {
            return new RunningRecordWeeklySummary(
                    record.get("date", LocalDate.class), record.get("sum_distance", Integer.class));
        }
    }
}
