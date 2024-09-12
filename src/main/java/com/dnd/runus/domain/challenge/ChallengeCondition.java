package com.dnd.runus.domain.challenge;

import lombok.Getter;
import lombok.experimental.Accessors;

@Getter
@Accessors(fluent = true)
public class ChallengeCondition {

    private final GoalMetricType goalMetricType;
    private final ComparisonType comparisonType;
    private final int goalValue; // 목표 값(챌린지가 지정한)

    private int comparisonValue; // 비교할 값(챌린지 성공 유무확인을 위한 비교할 값)

    public ChallengeCondition(GoalMetricType goalMetricType, ComparisonType comparisonType, int goalValue) {
        this.goalMetricType = goalMetricType;
        this.comparisonType = comparisonType;
        this.goalValue = goalValue;
        this.comparisonValue = goalValue;
    }

    public boolean isAchieved(int currentValue) {
        return comparisonType == ComparisonType.GREATER_THAN_OR_EQUAL_TO
                ? currentValue >= comparisonValue
                : currentValue <= comparisonValue;
    }

    public void registerComparisonValue(int comparisonValue) {
        if (comparisonType == ComparisonType.GREATER_THAN_OR_EQUAL_TO) {
            this.comparisonValue += comparisonValue;
        } else {
            this.comparisonValue = comparisonValue - this.comparisonValue;
        }
    }
}
