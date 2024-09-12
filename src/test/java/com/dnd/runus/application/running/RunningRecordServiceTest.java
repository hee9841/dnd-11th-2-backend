package com.dnd.runus.application.running;

import com.dnd.runus.domain.challenge.achievement.ChallengeAchievement;
import com.dnd.runus.domain.challenge.achievement.ChallengeAchievementPercentageRepository;
import com.dnd.runus.domain.challenge.achievement.ChallengeAchievementRepository;
import com.dnd.runus.domain.challenge.*;
import com.dnd.runus.domain.common.Coordinate;
import com.dnd.runus.domain.common.Pace;
import com.dnd.runus.domain.goalAchievement.GoalAchievement;
import com.dnd.runus.domain.goalAchievement.GoalAchievementRepository;
import com.dnd.runus.domain.level.Level;
import com.dnd.runus.domain.member.Member;
import com.dnd.runus.domain.member.MemberLevel;
import com.dnd.runus.domain.member.MemberLevelRepository;
import com.dnd.runus.domain.member.MemberRepository;
import com.dnd.runus.domain.running.RunningRecord;
import com.dnd.runus.domain.running.RunningRecordRepository;
import com.dnd.runus.domain.scale.ScaleAchievementRepository;
import com.dnd.runus.domain.scale.ScaleRepository;
import com.dnd.runus.global.constant.MemberRole;
import com.dnd.runus.global.constant.RunningEmoji;
import com.dnd.runus.global.exception.BusinessException;
import com.dnd.runus.global.exception.type.ErrorType;
import com.dnd.runus.presentation.v1.running.dto.RunningRecordMetricsDto;
import com.dnd.runus.presentation.v1.running.dto.request.RunningAchievementMode;
import com.dnd.runus.presentation.v1.running.dto.request.RunningRecordRequest;
import com.dnd.runus.presentation.v1.running.dto.response.RunningRecordAddResultResponse;
import com.dnd.runus.presentation.v1.running.dto.response.RunningRecordMonthlySummaryResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.*;
import java.util.List;
import java.util.Optional;

import static com.dnd.runus.global.constant.TimeConstant.SERVER_TIMEZONE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mockStatic;

@ExtendWith(MockitoExtension.class)
class RunningRecordServiceTest {
    private RunningRecordService runningRecordService;

    @Mock
    private RunningRecordRepository runningRecordRepository;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private MemberLevelRepository memberLevelRepository;

    @Mock
    private ChallengeRepository challengeRepository;

    @Mock
    private ChallengeAchievementRepository challengeAchievementRepository;

    @Mock
    private ChallengeAchievementPercentageRepository percentageValuesRepository;

    @Mock
    private GoalAchievementRepository goalAchievementRepository;

    @Mock
    private ScaleRepository scaleRepository;

    @Mock
    private ScaleAchievementRepository scaleAchievementRepository;

    private final ZoneOffset defaultZoneOffset = ZoneOffset.of("+9");

    @BeforeEach
    void setUp() {
        runningRecordService = new RunningRecordService(
                runningRecordRepository,
                memberRepository,
                memberLevelRepository,
                challengeRepository,
                challengeAchievementRepository,
                percentageValuesRepository,
                goalAchievementRepository,
                scaleRepository,
                scaleAchievementRepository,
                defaultZoneOffset);
    }

    @Test
    @DisplayName("CHALLENGE 모드의 러닝 기록 추가 요청시, challengeId에 해당하는 챌린지가 있을 경우, 정상적으로 러닝 기록이 추가된다.")
    void addRunningRecord_challenge() {
        // given
        RunningRecordRequest request = new RunningRecordRequest(
                LocalDateTime.of(2021, 1, 1, 12, 10, 30),
                LocalDateTime.of(2021, 1, 1, 13, 12, 10),
                "start location",
                "end location",
                RunningEmoji.VERY_GOOD,
                1L,
                null,
                null,
                RunningAchievementMode.CHALLENGE,
                new RunningRecordMetricsDto(new Pace(5, 30), Duration.ofSeconds(10_100), 10_000, 500.0));

        Member member = new Member(MemberRole.USER, "nickname1");
        RunningRecord expected = createRunningRecord(request, member);

        ChallengeWithCondition challengeWithCondition = new ChallengeWithCondition(
                new Challenge(1L, "challenge", "image", ChallengeType.TODAY),
                List.of(new ChallengeCondition(
                        GoalMetricType.DISTANCE, ComparisonType.GREATER_THAN_OR_EQUAL_TO, 10_000)));
        ChallengeAchievement challengeAchievement =
                new ChallengeAchievement(0L, challengeWithCondition.challenge(), expected, true);

        given(memberRepository.findById(1L)).willReturn(Optional.of(member));
        given(runningRecordRepository.save(expected)).willReturn(expected);
        given(challengeRepository.findChallengeWithConditionsByChallengeId(1L))
                .willReturn(Optional.of(challengeWithCondition));
        given(challengeAchievementRepository.save(challengeAchievement)).willReturn(challengeAchievement);

        // when
        RunningRecordAddResultResponse response = runningRecordService.addRunningRecord(1L, request);

        // then
        assertEquals(request.startAt(), response.startAt());
        assertEquals(request.endAt(), response.endAt());
    }

    @Test
    @DisplayName("GOAL 모드의 러닝 기록 추가 시,goalTime이 null이 아니고 목표값보다 실제값이 더 크다면, 성공한 goalAchievement와 함께 정상적으로 러닝 기록이 추가된다.")
    void addRunningRecord_goal_time_success() {
        // given
        RunningRecordRequest request = new RunningRecordRequest(
                LocalDateTime.of(2021, 1, 1, 12, 10, 30),
                LocalDateTime.of(2021, 1, 1, 13, 12, 10),
                "start location",
                "end location",
                RunningEmoji.VERY_GOOD,
                null,
                null,
                1200,
                RunningAchievementMode.GOAL,
                new RunningRecordMetricsDto(new Pace(5, 30), Duration.ofSeconds(10_100), 10_000, 500.0));

        Member member = new Member(MemberRole.USER, "nickname1");
        RunningRecord expected = createRunningRecord(request, member);

        GoalAchievement goalAchievement = new GoalAchievement(expected, GoalMetricType.TIME, 1200, true);

        given(memberRepository.findById(1L)).willReturn(Optional.of(member));
        given(runningRecordRepository.save(expected)).willReturn(expected);
        given(goalAchievementRepository.save(goalAchievement)).willReturn(goalAchievement);

        // when
        RunningRecordAddResultResponse response = runningRecordService.addRunningRecord(1L, request);

        // then
        assertEquals(request.startAt(), response.startAt());
        assertEquals(request.endAt(), response.endAt());

        assertTrue(response.goal().title().contains("분"));
        assertFalse(response.goal().title().contains("km"));

        assertTrue(response.goal().isSuccess());
    }

    @Test
    @DisplayName(
            "GOAL 모드의 러닝 기록 추가 시, goalTime이 null이 아니고 목표값보다 실제값이 더 낮다면, 실패한 goalAchievement와 함께 정상적으로 러닝 기록이 추가된다.")
    void addRunningRecord_goal_time_fail() {
        // given
        RunningRecordRequest request = new RunningRecordRequest(
                LocalDateTime.of(2021, 1, 1, 12, 10, 30),
                LocalDateTime.of(2021, 1, 1, 13, 12, 10),
                "start location",
                "end location",
                RunningEmoji.VERY_GOOD,
                null,
                null,
                20_000,
                RunningAchievementMode.GOAL,
                new RunningRecordMetricsDto(new Pace(5, 30), Duration.ofSeconds(10_100), 10_000, 500.0));

        Member member = new Member(MemberRole.USER, "nickname1");
        RunningRecord expected = createRunningRecord(request, member);

        GoalAchievement goalAchievement = new GoalAchievement(expected, GoalMetricType.TIME, 20_000, false);

        given(memberRepository.findById(1L)).willReturn(Optional.of(member));
        given(runningRecordRepository.save(expected)).willReturn(expected);
        given(goalAchievementRepository.save(goalAchievement)).willReturn(goalAchievement);

        // when
        RunningRecordAddResultResponse response = runningRecordService.addRunningRecord(1L, request);

        // then
        assertEquals(request.startAt(), response.startAt());
        assertEquals(request.endAt(), response.endAt());

        assertTrue(response.goal().title().contains("분"));
        assertFalse(response.goal().title().contains("km"));

        assertFalse(response.goal().isSuccess());
    }

    @Test
    @DisplayName(
            "GOAL 모드의 러닝 기록 추가 요청시, goalDistance가 null이 아니고 목표값보다 실제값이 더 높다면 성공한 goalAchievement와 함께 정상적으로 러닝 기록이 추가된다.")
    void addRunningRecord_goal_distance() {
        // given
        RunningRecordRequest request = new RunningRecordRequest(
                LocalDateTime.of(2021, 1, 1, 12, 10, 30),
                LocalDateTime.of(2021, 1, 1, 13, 12, 10),
                "start location",
                "end location",
                RunningEmoji.VERY_GOOD,
                null,
                5_000,
                null,
                RunningAchievementMode.GOAL,
                new RunningRecordMetricsDto(new Pace(5, 30), Duration.ofSeconds(10_100), 10_000, 500.0));

        Member member = new Member(MemberRole.USER, "nickname1");
        RunningRecord expected = createRunningRecord(request, member);

        GoalAchievement goalAchievement = new GoalAchievement(expected, GoalMetricType.DISTANCE, 5_000, true);

        given(memberRepository.findById(1L)).willReturn(Optional.of(member));
        given(runningRecordRepository.save(expected)).willReturn(expected);
        given(goalAchievementRepository.save(goalAchievement)).willReturn(goalAchievement);

        // when
        RunningRecordAddResultResponse response = runningRecordService.addRunningRecord(1L, request);

        // then
        assertEquals(request.startAt(), response.startAt());
        assertEquals(request.endAt(), response.endAt());

        assertTrue(response.goal().title().contains("km"));
        assertFalse(response.goal().title().contains("분"));

        assertTrue(response.goal().isSuccess());
    }

    @Test
    @DisplayName(
            "GOAL 모드의 러닝 기록 추가 요청시, goalDistance가 null이 아니고 목표값보다 실제값과 같다면 성공한 goalAchievement와 함께 정상적으로 러닝 기록이 추가된다.")
    void addRunningRecord_goal_distance_same_value() {
        // given
        RunningRecordRequest request = new RunningRecordRequest(
                LocalDateTime.of(2021, 1, 1, 12, 10, 30),
                LocalDateTime.of(2021, 1, 1, 13, 12, 10),
                "start location",
                "end location",
                RunningEmoji.VERY_GOOD,
                null,
                10_000,
                null,
                RunningAchievementMode.GOAL,
                new RunningRecordMetricsDto(new Pace(5, 30), Duration.ofSeconds(10_100), 10_000, 500.0));

        Member member = new Member(MemberRole.USER, "nickname1");
        RunningRecord expected = createRunningRecord(request, member);

        GoalAchievement goalAchievement = new GoalAchievement(expected, GoalMetricType.DISTANCE, 10_000, true);

        given(memberRepository.findById(1L)).willReturn(Optional.of(member));
        given(runningRecordRepository.save(expected)).willReturn(expected);
        given(goalAchievementRepository.save(goalAchievement)).willReturn(goalAchievement);

        // when
        RunningRecordAddResultResponse response = runningRecordService.addRunningRecord(1L, request);

        // then
        assertEquals(request.startAt(), response.startAt());
        assertEquals(request.endAt(), response.endAt());

        assertTrue(response.goal().title().contains("km"));
        assertFalse(response.goal().title().contains("분"));

        assertTrue(response.goal().isSuccess());
    }

    @Test
    @DisplayName("시작 시간이 종료 시간보다 늦을 경우, BusinessException이 발생한다.")
    void addRunningRecord_StartAtAfterEndAt() {
        // given
        RunningRecordRequest request = new RunningRecordRequest(
                LocalDateTime.of(2021, 1, 1, 12, 10, 30),
                LocalDateTime.of(2021, 1, 1, 11, 12, 10),
                "start location",
                "end location",
                RunningEmoji.VERY_GOOD,
                1L,
                null,
                null,
                RunningAchievementMode.CHALLENGE,
                new RunningRecordMetricsDto(new Pace(5, 30), Duration.ofSeconds(10_100), 10_000, 500.0));
        // when
        BusinessException exception =
                assertThrows(BusinessException.class, () -> runningRecordService.addRunningRecord(1L, request));
        // then
        assertEquals(ErrorType.START_AFTER_END, exception.getType());
    }

    @Test
    @DisplayName("이번 달, 달린 키로 수, 러닝 레벨을 조회한다.")
    void getMonthlyRunningSummery() {
        // given
        long memberId = 1;
        OffsetDateTime fixedDate = ZonedDateTime.of(2021, 1, 15, 9, 0, 0, 0, ZoneId.of(SERVER_TIMEZONE))
                .toOffsetDateTime();
        try (MockedStatic<OffsetDateTime> mockedStatic = mockStatic(OffsetDateTime.class)) {
            mockedStatic
                    .when(() -> OffsetDateTime.now(ZoneId.of(SERVER_TIMEZONE)))
                    .thenReturn(fixedDate);

            given(runningRecordRepository.findTotalDistanceMeterByMemberId(eq(memberId), any(), any()))
                    .willReturn(45_780);

            given(memberLevelRepository.findByMemberIdWithLevel(memberId))
                    .willReturn(new MemberLevel.Current(new Level(1, 0, 50_000, "image"), 45_780));

            // when
            RunningRecordMonthlySummaryResponse monthlyRunningSummery =
                    runningRecordService.getMonthlyRunningSummery(memberId);

            // then
            assertNotNull(monthlyRunningSummery);
            assertThat(monthlyRunningSummery.month()).isEqualTo("1월");
            assertThat(monthlyRunningSummery.monthlyKm()).isEqualTo("45.78km");
            assertThat(monthlyRunningSummery.nextLevelName()).isEqualTo("Level 2");
            assertThat(monthlyRunningSummery.nextLevelKm()).isEqualTo("4.22km");
        }
    }

    private RunningRecord createRunningRecord(RunningRecordRequest request, Member member) {
        return RunningRecord.builder()
                .member(member)
                .startAt(request.startAt().atOffset(defaultZoneOffset))
                .endAt(request.endAt().atOffset(defaultZoneOffset))
                .emoji(request.emotion())
                .startLocation(request.startLocation())
                .endLocation(request.endLocation())
                .distanceMeter(request.runningData().distanceMeter())
                .duration(request.runningData().runningTime())
                .calorie(request.runningData().calorie())
                .averagePace(request.runningData().averagePace())
                .route(List.of(new Coordinate(0, 0, 0), new Coordinate(0, 0, 0)))
                .build();
    }
}
