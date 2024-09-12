package com.dnd.runus.infrastructure.persistence.jooq.running;

import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.jooq.Record1;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;

import static com.dnd.runus.jooq.Tables.RUNNING_RECORD;
import static org.jooq.impl.DSL.sum;

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
}
