package com.dnd.runus.infrastructure.persistence.jooq.scale;

import lombok.RequiredArgsConstructor;
import org.jooq.CommonTableExpression;
import org.jooq.DSLContext;
import org.jooq.Record1;
import org.jooq.Record2;
import org.springframework.stereotype.Repository;

import java.util.List;

import static com.dnd.runus.jooq.Tables.RUNNING_RECORD;
import static com.dnd.runus.jooq.Tables.SCALE_ACHIEVEMENT;
import static com.dnd.runus.jooq.tables.Scale.SCALE;
import static org.jooq.impl.DSL.coalesce;
import static org.jooq.impl.DSL.name;
import static org.jooq.impl.DSL.select;
import static org.jooq.impl.DSL.sum;

@Repository
@RequiredArgsConstructor
public class JooqScaleRepository {

    private final DSLContext dsl;

    public List<Long> findAchievableScaleIds(long memberId) {

        CommonTableExpression<Record1<Integer>> totalDistance = name("total_distance")
                .fields("total_distance_meter")
                .as(select(sum(RUNNING_RECORD.DISTANCE_METER).cast(int.class))
                        .from(RUNNING_RECORD)
                        .where(RUNNING_RECORD.MEMBER_ID.eq(memberId)));

        CommonTableExpression<Record2<Long, Integer>> cumulativeScale = name("cumulative_scale")
                .fields("id", "cumulative_sum")
                .as(select(
                                SCALE.ID,
                                sum(SCALE.SIZE_METER).over().orderBy(SCALE.ID).cast(int.class))
                        .from(SCALE));

        return dsl.with(totalDistance)
                .with(cumulativeScale)
                .select(cumulativeScale.field("id", Long.class))
                .from(cumulativeScale)
                .join(totalDistance)
                .on(coalesce(cumulativeScale.field("cumulative_sum", Integer.class), 0)
                        .le(totalDistance.field("total_distance_meter", Integer.class)))
                .where(coalesce(cumulativeScale.field("id", Long.class), -1)
                        .notIn(select(SCALE_ACHIEVEMENT.SCALE_ID)
                                .from(SCALE_ACHIEVEMENT)
                                .where(SCALE_ACHIEVEMENT.MEMBER_ID.eq(memberId))))
                .fetchInto(Long.class);
    }
}
