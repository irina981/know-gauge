package com.knowgauge.restapi.mapper;

import java.util.List;

import org.springframework.stereotype.Component;

import com.knowgauge.contract.dto.AttemptQuestionDto;
import com.knowgauge.contract.dto.PublicAttemptDto;
import com.knowgauge.contract.dto.PublicInvitationDto;
import com.knowgauge.contract.dto.PublicTestDto;
import com.knowgauge.contract.dto.TestInvitationDto;
import com.knowgauge.core.model.Attempt;
import com.knowgauge.core.model.Test;
import com.knowgauge.core.model.enums.AttemptStatus;
import com.knowgauge.core.service.attempt.AttemptService.AttemptView;
import com.knowgauge.core.service.invitation.InvitationProperties;
import com.knowgauge.core.service.invitation.InvitationService.InvitationView;
import com.knowgauge.core.service.invitation.InvitationService.PublicInvitationView;

/**
 * DTOs for participants (ADR009) and for the administrator's invitation list.
 * Participant DTOs never carry the answer key: while an attempt runs, its
 * questions come without it, and once scored only the score and the
 * participant's own answers marked right or wrong are returned.
 */
@Component
public class PublicMapper {

	private final AttemptMapper attemptMapper;
	private final InvitationProperties invitationProperties;

	public PublicMapper(AttemptMapper attemptMapper, InvitationProperties invitationProperties) {
		this.attemptMapper = attemptMapper;
		this.invitationProperties = invitationProperties;
	}

	public PublicTestDto toPublicTest(Test test, int questionCount) {
		return new PublicTestDto(test.getName(), test.getDescription(), questionCount,
				test.getAnswerCardinality() == null ? null : test.getAnswerCardinality().name());
	}

	public PublicAttemptDto toPublicAttempt(AttemptView view) {
		Attempt attempt = view.attempt();
		boolean running = attempt.getStatus() == AttemptStatus.STARTED;
		List<AttemptQuestionDto> questions = attemptMapper.toDetailDto(view).questions();
		return new PublicAttemptDto(attempt.getPublicToken(), view.test().getName(),
				view.test().getAnswerCardinality() == null ? null : view.test().getAnswerCardinality().name(),
				attempt.getParticipantName(), attempt.getStatus().name(), attempt.getTotalQuestions(),
				running ? null : attempt.getCorrectCount(), running ? null : attempt.getScorePercent(),
				running ? questions : questions.stream().map(PublicMapper::withoutKey).toList());
	}

	/**
	 * After scoring, participants see their own choices marked right or wrong,
	 * but never the correct options or the explanation, so the answer key stays
	 * private and the test can be reused (ADR009).
	 */
	private static AttemptQuestionDto withoutKey(AttemptQuestionDto q) {
		return new AttemptQuestionDto(q.questionId(), q.questionVersionId(), q.questionIndex(), q.questionText(),
				q.options(), q.chosenOptions(), null, q.correct(), null);
	}

	public PublicInvitationDto toPublicInvitation(PublicInvitationView view) {
		Attempt attempt = view.attempt();
		boolean running = attempt != null && attempt.getStatus() == AttemptStatus.STARTED;
		return new PublicInvitationDto(view.invitation().getEmail(), toPublicTest(view.test(), view.questionCount()),
				attempt == null ? null : attempt.getStatus().name(), running ? attempt.getPublicToken() : null);
	}

	public TestInvitationDto toInvitation(InvitationView view) {
		var invitation = view.invitation();
		Attempt attempt = view.attempt();
		boolean scored = attempt != null && attempt.getStatus() == AttemptStatus.SCORED;
		return new TestInvitationDto(invitation.getId(), invitation.getTestId(), invitation.getEmail(),
				invitationProperties.linkFor(invitation.getPublicToken()), invitation.getCreatedAt(),
				invitation.getSentAt(), invitation.getSendCount(), invitation.getSendError(),
				attempt == null ? null : attempt.getId(), attempt == null ? null : attempt.getStatus().name(),
				attempt == null ? null : attempt.getParticipantName(), scored ? attempt.getScorePercent() : null);
	}
}
