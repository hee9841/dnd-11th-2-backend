package com.dnd.runus.application.challenge;

import com.dnd.runus.domain.challenge.ChallengeRepository;
import com.dnd.runus.domain.running.RunningRecordRepository;
import com.dnd.runus.presentation.v1.challenge.dto.response.ChallengesResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

import static com.dnd.runus.global.constant.TimeConstant.SERVER_TIMEZONE_ID;

@Service
@RequiredArgsConstructor
public class ChallengeService {

    private final ChallengeRepository challengeRepository;

    private final RunningRecordRepository runningRecordRepository;

    public List<ChallengesResponse> getChallenges(long memberId) {
        OffsetDateTime todayMidnight = LocalDate.now(SERVER_TIMEZONE_ID)
                .atStartOfDay(SERVER_TIMEZONE_ID)
                .toOffsetDateTime();
        OffsetDateTime yesterday = todayMidnight.minusDays(1);

        // 어제 기록이 없으면
        if (!runningRecordRepository.hasByMemberIdAndStartAtBetween(memberId, yesterday, todayMidnight)) {
            return challengeRepository.findAllIsNotDefeatYesterday().stream()
                    .map(ChallengesResponse::from)
                    .toList();
        }

        return challengeRepository.findAllChallenges().stream()
                .map(ChallengesResponse::from)
                .toList();
    }
}
