package com.knowgauge.core.service.invitation;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.knowgauge.core.exception.AttemptConflictException;
import com.knowgauge.core.exception.ResourceNotFoundException;
import com.knowgauge.core.model.Attempt;
import com.knowgauge.core.model.Test;
import com.knowgauge.core.model.TestInvitation;
import com.knowgauge.core.model.enums.TestStatus;
import com.knowgauge.core.port.repository.AttemptRepository;
import com.knowgauge.core.port.repository.TestInvitationRepository;
import com.knowgauge.core.port.repository.TestQuestionRepository;
import com.knowgauge.core.port.repository.TestRepository;
import com.knowgauge.core.service.invitation.InvitationService.InvitationView;
import com.knowgauge.core.service.invitation.InvitationService.PublicInvitationView;
import com.knowgauge.core.util.PublicTokens;

/**
 * The database side of invitations, in short transactions. Mail is sent by
 * {@link InvitationServiceImpl} between them.
 */
@Service
public class InvitationTransactionalServiceImpl {

	/** A test with the invitations to send for it. */
	public record Prepared(Test test, int questionCount, List<TestInvitation> invitations,
			Map<Long, Attempt> attemptsByInvitation) {
	}

	private final TestInvitationRepository invitationRepository;
	private final TestRepository testRepository;
	private final TestQuestionRepository questionRepository;
	private final AttemptRepository attemptRepository;

	public InvitationTransactionalServiceImpl(TestInvitationRepository invitationRepository,
			TestRepository testRepository, TestQuestionRepository questionRepository,
			AttemptRepository attemptRepository) {
		this.invitationRepository = invitationRepository;
		this.testRepository = testRepository;
		this.questionRepository = questionRepository;
		this.attemptRepository = attemptRepository;
	}

	/** Creates the missing invitations; existing ones keep their token. */
	@Transactional
	public Prepared prepare(long tenantId, Long testId, List<String> emails) {
		Test test = publishedTest(tenantId, testId);
		List<TestInvitation> invitations = new ArrayList<>(emails.size());
		for (String email : emails) {
			invitations.add(invitationRepository.findByTenantIdAndTestIdAndEmail(tenantId, testId, email)
					.orElseGet(() -> invitationRepository.save(TestInvitation.builder().tenantId(tenantId)
							.testId(testId).email(email).publicToken(PublicTokens.newToken()).build())));
		}
		return new Prepared(test, questionCount(tenantId, testId), invitations,
				attemptsByInvitation(tenantId, testId));
	}

	@Transactional(readOnly = true)
	public Prepared prepareResend(long tenantId, Long testId, Long invitationId) {
		Test test = publishedTest(tenantId, testId);
		TestInvitation invitation = invitationRepository.findByTenantIdAndId(tenantId, invitationId)
				.filter(i -> i.getTestId().equals(testId))
				.orElseThrow(() -> new ResourceNotFoundException("Invitation not found: " + invitationId));
		Map<Long, Attempt> attempts = attemptsByInvitation(tenantId, testId);
		if (attempts.containsKey(invitationId)) {
			throw new AttemptConflictException(
					"The invitation for " + invitation.getEmail() + " was already used; it cannot be resent.");
		}
		return new Prepared(test, questionCount(tenantId, testId), List.of(invitation), attempts);
	}

	/** Records one send: {@code error} is {@code null} when it was delivered. */
	@Transactional
	public TestInvitation recordSend(long tenantId, Long invitationId, String error) {
		TestInvitation invitation = invitationRepository.findByTenantIdAndId(tenantId, invitationId)
				.orElseThrow(() -> new ResourceNotFoundException("Invitation not found: " + invitationId));
		invitation.setSendCount(invitation.getSendCount() + 1);
		invitation.setSendError(error);
		if (error == null) {
			invitation.setSentAt(Instant.now());
		}
		return invitationRepository.save(invitation);
	}

	@Transactional(readOnly = true)
	public List<InvitationView> list(long tenantId, Long testId) {
		testRepository.findByTenantIdAndId(tenantId, testId)
				.orElseThrow(() -> new ResourceNotFoundException("Test not found: " + testId));
		Map<Long, Attempt> attempts = attemptsByInvitation(tenantId, testId);
		return invitationRepository.findByTenantIdAndTestId(tenantId, testId).stream()
				.map(i -> new InvitationView(i, attempts.get(i.getId()))).toList();
	}

	@Transactional(readOnly = true)
	public PublicInvitationView getByToken(long tenantId, String token) {
		TestInvitation invitation = invitationRepository.findByTenantIdAndPublicToken(tenantId, token)
				.orElseThrow(() -> new ResourceNotFoundException("This invitation link is not valid."));
		Test test = testRepository.findByTenantIdAndId(tenantId, invitation.getTestId())
				.filter(t -> t.getStatus() == TestStatus.PUBLISHED)
				.orElseThrow(() -> new ResourceNotFoundException("This test is closed."));
		Attempt attempt = attemptRepository.findByTenantIdAndInvitationId(tenantId, invitation.getId())
				.orElse(null);
		return new PublicInvitationView(invitation, test, questionCount(tenantId, test.getId()), attempt);
	}

	private Test publishedTest(long tenantId, Long testId) {
		Test test = testRepository.findByTenantIdAndId(tenantId, testId)
				.orElseThrow(() -> new ResourceNotFoundException("Test not found: " + testId));
		if (test.getStatus() != TestStatus.PUBLISHED) {
			throw new AttemptConflictException("Test " + testId + " is not published (status " + test.getStatus()
					+ "). Publish it before inviting participants.");
		}
		return test;
	}

	private int questionCount(long tenantId, Long testId) {
		return questionRepository.findByTenantIdAndTestId(tenantId, testId).size();
	}

	private Map<Long, Attempt> attemptsByInvitation(long tenantId, Long testId) {
		return attemptRepository.findByTenantId(tenantId, testId, null, null).stream()
				.filter(a -> a.getInvitationId() != null)
				.collect(Collectors.toMap(Attempt::getInvitationId, Function.identity(), (a, b) -> a));
	}
}
