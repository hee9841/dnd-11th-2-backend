package com.dnd.runus.infrastructure.persistence.jpa.challenge;

import com.dnd.runus.infrastructure.persistence.jpa.challenge.entity.ChallengeAchievementEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface JpaChallengeAchievementRepository extends JpaRepository<ChallengeAchievementEntity, Long> {
    Optional<ChallengeAchievementEntity> findByRunningRecordId(long runningId);

    List<ChallengeAchievementEntity> findAllByRunningRecordIdIn(List<Long> runningIds);
}
