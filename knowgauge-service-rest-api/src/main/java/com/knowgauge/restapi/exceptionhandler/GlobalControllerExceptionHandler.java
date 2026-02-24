package com.knowgauge.restapi.exceptionhandler;

import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import com.knowgauge.core.exception.DuplicateDocumentContentException;
import com.knowgauge.core.exception.DuplicateDocumentNameException;
import com.knowgauge.core.exception.EmbeddingAuthException;
import com.knowgauge.core.exception.EmbeddingBadRequestException;
import com.knowgauge.core.exception.EmbeddingRateLimitedException;
import com.knowgauge.core.exception.EmbeddingUnavailableException;
import com.knowgauge.core.exception.EmbeddingUnexpectedException;
import com.knowgauge.core.exception.LlmResponseParsingException;
import com.knowgauge.core.exception.StorageAuthException;
import com.knowgauge.core.exception.StorageBadRequestException;
import com.knowgauge.core.exception.StorageNotFoundException;
import com.knowgauge.core.exception.StorageUnavailableException;
import com.knowgauge.core.exception.StorageUnexpectedException;

import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ConstraintViolationException;

/**
 * Global exception handler for the Transactions REST API.
 */
@RestControllerAdvice
public class GlobalControllerExceptionHandler extends ResponseEntityExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(GlobalControllerExceptionHandler.class);

	// 1) Bean validation on @RequestBody DTOs (@Valid)
	@Override
	protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
			HttpHeaders headers, HttpStatusCode status, WebRequest request) {

		HttpErrorInfo responseBody = createMethodArgumentNotValidHttpErrorInfo(HttpStatus.BAD_REQUEST, request, ex);

		log.error("Validation failed: {}", responseBody.getMessage());

		return handleExceptionInternal(ex, responseBody, new HttpHeaders(), HttpStatus.BAD_REQUEST, request);
	}

	// 1.5) JSON deserialization errors (including enum conversion failures)
	@Override
	protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex,
			HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		
		String message = ex.getMessage();
		
		// Improve error message for enum conversion failures
		if (message != null && message.contains("Cannot deserialize value of type")) {
			// Extract a more user-friendly message
			if (message.contains("not one of the values accepted for Enum class")) {
				int start = message.indexOf("not one of the values accepted for Enum class");
				if (start > 0) {
					message = "Invalid enum value: " + message.substring(0, start).trim();
				}
			}
		}
		
		HttpErrorInfo responseBody = createHttpErrorInfo(HttpStatus.BAD_REQUEST, request, message);
		log.error("JSON deserialization error: {}", message);
		
		return handleExceptionInternal(ex, responseBody, new HttpHeaders(), HttpStatus.BAD_REQUEST, request);
	}

	// 2) JPA entity not found (typical for /transactions/{id})
	@ExceptionHandler(EntityNotFoundException.class)
	public ResponseEntity<Object> handleEntityNotFoundException(WebRequest request, EntityNotFoundException ex) {
		HttpErrorInfo responseBody = createHttpErrorInfo(HttpStatus.NOT_FOUND, request, ex);
		log.error("Entity not found: {}", ex.getMessage());
		return handleExceptionInternal(ex, responseBody, new HttpHeaders(), HttpStatus.NOT_FOUND, request);
	}

	// 3) DB constraint violations (unique constraints, FK violations, etc.)
	@ExceptionHandler(DataIntegrityViolationException.class)
	public ResponseEntity<Object> handleDataIntegrityViolationException(WebRequest request,
			DataIntegrityViolationException ex) {
		HttpErrorInfo responseBody = createHttpErrorInfo(HttpStatus.CONFLICT, request, ex);
		log.error("Data integrity violation: {}", ex.getMessage());
		return handleExceptionInternal(ex, responseBody, new HttpHeaders(), HttpStatus.CONFLICT, request);
	}

	// 4) Bean validation on method parameters (e.g. @Validated on controller /
	// service)
	@ExceptionHandler(ConstraintViolationException.class)
	public ResponseEntity<Object> handleConstraintViolationException(WebRequest request,
			ConstraintViolationException ex) {
		HttpErrorInfo responseBody = createHttpErrorInfo(HttpStatus.BAD_REQUEST, request, ex);
		log.error("Constraint violation: {}", ex.getMessage());
		return handleExceptionInternal(ex, responseBody, new HttpHeaders(), HttpStatus.BAD_REQUEST, request);
	}

	// 5) Errors from synchronous REST templates (if you use RestTemplate somewhere)
	@ExceptionHandler(HttpClientErrorException.class)
	public ResponseEntity<Object> handleHttpClientErrorException(WebRequest request, HttpClientErrorException ex) {
		HttpStatus status = HttpStatus.valueOf(ex.getStatusCode().value());
		HttpErrorInfo responseBody = createHttpErrorInfo(status, request, ex);
		log.error("Downstream HTTP client error: status={}, body={}", status, ex.getResponseBodyAsString());
		return handleExceptionInternal(ex, responseBody, new HttpHeaders(), status, request);
	}

	// 6) Errors from RestClient
	@ExceptionHandler(RestClientResponseException.class)
	public ResponseEntity<Object> handleRestClientResponseException(WebRequest request,
			RestClientResponseException ex) {

		HttpStatus status = HttpStatus.valueOf(ex.getStatusCode().value());
		HttpErrorInfo body = createHttpErrorInfo(status, request, ex);

		log.error("Downstream RestClient error: status={}, body={}", status, ex.getResponseBodyAsString());

		return handleExceptionInternal(ex, body, new HttpHeaders(), status, request);
	}

	// 7) Bad input / domain misuse from your own code
	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<Object> handleIllegalArgumentException(WebRequest request, IllegalArgumentException ex) {
		HttpErrorInfo responseBody = createHttpErrorInfo(HttpStatus.BAD_REQUEST, request, ex);
		log.error("Illegal argument: {}", ex.getMessage());
		return handleExceptionInternal(ex, responseBody, new HttpHeaders(), HttpStatus.BAD_REQUEST, request);
	}

	// 8) Bad input / unsupported operation exception - e.g. FOCUSED coverage mode is currently unsupported
	@ExceptionHandler(UnsupportedOperationException.class)
	public ResponseEntity<Object> handleUnsupportedOperationException(WebRequest request,
			UnsupportedOperationException ex) {
		HttpErrorInfo responseBody = createHttpErrorInfo(HttpStatus.BAD_REQUEST, request, ex);
		log.error("Unsupported operation: {}", ex.getMessage());
		return handleExceptionInternal(ex, responseBody, new HttpHeaders(), HttpStatus.BAD_REQUEST, request);
	}

	// 9) Catch-all – last resort
	@ExceptionHandler(Exception.class)
	public ResponseEntity<Object> handleUnexpectedException(WebRequest request, Exception ex) {
		HttpErrorInfo responseBody = createHttpErrorInfo(HttpStatus.INTERNAL_SERVER_ERROR, request, ex);
		log.error("Unexpected error", ex);
		return handleExceptionInternal(ex, responseBody, new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR, request);
	}

	// 10) Custom KnowGauge exceptions

	// Storage exceptions
	@ExceptionHandler(StorageNotFoundException.class)
	public ResponseEntity<Object> handleStorageNotFoundException(WebRequest request,
			StorageNotFoundException ex) {
		HttpErrorInfo responseBody = createHttpErrorInfo(HttpStatus.NOT_FOUND, request, ex);
		log.warn("Storage not found: {}", ex.getMessage());
		return handleExceptionInternal(ex, responseBody, new HttpHeaders(), HttpStatus.NOT_FOUND, request);
	}

	@ExceptionHandler(StorageAuthException.class)
	public ResponseEntity<Object> handleStorageAuthException(WebRequest request, StorageAuthException ex) {
		HttpErrorInfo responseBody = createHttpErrorInfo(HttpStatus.UNAUTHORIZED, request, ex);
		log.error("Storage authentication error: {}", ex.getMessage());
		return handleExceptionInternal(ex, responseBody, new HttpHeaders(), HttpStatus.UNAUTHORIZED, request);
	}

	@ExceptionHandler(StorageBadRequestException.class)
	public ResponseEntity<Object> handleStorageBadRequestException(WebRequest request,
			StorageBadRequestException ex) {
		HttpErrorInfo responseBody = createHttpErrorInfo(HttpStatus.BAD_REQUEST, request, ex);
		log.warn("Storage bad request: {}", ex.getMessage());
		return handleExceptionInternal(ex, responseBody, new HttpHeaders(), HttpStatus.BAD_REQUEST, request);
	}

	@ExceptionHandler(StorageUnavailableException.class)
	public ResponseEntity<Object> handleStorageUnavailableException(WebRequest request,
			StorageUnavailableException ex) {
		HttpErrorInfo responseBody = createHttpErrorInfo(HttpStatus.SERVICE_UNAVAILABLE, request, ex);
		log.error("Storage unavailable: {}", ex.getMessage(), ex);
		return handleExceptionInternal(ex, responseBody, new HttpHeaders(), HttpStatus.SERVICE_UNAVAILABLE, request);
	}

	@ExceptionHandler(StorageUnexpectedException.class)
	public ResponseEntity<Object> handleStorageUnexpectedException(WebRequest request,
			StorageUnexpectedException ex) {
		HttpErrorInfo responseBody = createHttpErrorInfo(HttpStatus.INTERNAL_SERVER_ERROR, request, ex);
		log.error("Storage unexpected error: {}", ex.getMessage(), ex);
		return handleExceptionInternal(ex, responseBody, new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR, request);
	}

	// Embedding exceptions
	@ExceptionHandler(EmbeddingAuthException.class)
	public ResponseEntity<Object> handleEmbeddingAuthException(WebRequest request, EmbeddingAuthException ex) {
		HttpErrorInfo responseBody = createHttpErrorInfo(HttpStatus.UNAUTHORIZED, request, ex);
		log.error("Embedding authentication error: {}", ex.getMessage());
		return handleExceptionInternal(ex, responseBody, new HttpHeaders(), HttpStatus.UNAUTHORIZED, request);
	}

	@ExceptionHandler(EmbeddingBadRequestException.class)
	public ResponseEntity<Object> handleEmbeddingBadRequestException(WebRequest request,
			EmbeddingBadRequestException ex) {
		HttpErrorInfo responseBody = createHttpErrorInfo(HttpStatus.BAD_REQUEST, request, ex);
		log.warn("Embedding bad request: {}", ex.getMessage());
		return handleExceptionInternal(ex, responseBody, new HttpHeaders(), HttpStatus.BAD_REQUEST, request);
	}

	@ExceptionHandler(EmbeddingRateLimitedException.class)
	public ResponseEntity<Object> handleEmbeddingRateLimitedException(WebRequest request,
			EmbeddingRateLimitedException ex) {
		HttpErrorInfo responseBody = createHttpErrorInfo(HttpStatus.TOO_MANY_REQUESTS, request, ex);
		log.warn("Embedding rate limited: {}", ex.getMessage());
		return handleExceptionInternal(ex, responseBody, new HttpHeaders(), HttpStatus.TOO_MANY_REQUESTS, request);
	}

	@ExceptionHandler(EmbeddingUnavailableException.class)
	public ResponseEntity<Object> handleEmbeddingUnavailableException(WebRequest request,
			EmbeddingUnavailableException ex) {
		HttpErrorInfo responseBody = createHttpErrorInfo(HttpStatus.SERVICE_UNAVAILABLE, request, ex);
		log.error("Embedding service unavailable: {}", ex.getMessage(), ex);
		return handleExceptionInternal(ex, responseBody, new HttpHeaders(), HttpStatus.SERVICE_UNAVAILABLE, request);
	}

	@ExceptionHandler(EmbeddingUnexpectedException.class)
	public ResponseEntity<Object> handleEmbeddingUnexpectedException(WebRequest request,
			EmbeddingUnexpectedException ex) {
		HttpErrorInfo responseBody = createHttpErrorInfo(HttpStatus.INTERNAL_SERVER_ERROR, request, ex);
		log.error("Embedding unexpected error: {}", ex.getMessage(), ex);
		return handleExceptionInternal(ex, responseBody, new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR, request);
	}

	// LLM/Test generation exceptions
	@ExceptionHandler(LlmResponseParsingException.class)
	public ResponseEntity<Object> handleLlmResponseParsingException(WebRequest request,
			LlmResponseParsingException ex) {
		HttpStatus status;
		switch (ex.getReason()) {
		case LENGTH:
			status = HttpStatus.INSUFFICIENT_STORAGE; // 507 - indicates resource exhaustion
			log.warn("LLM response parsing error (LENGTH): {}", ex.getMessage());
			break;
		case PARSING_ERROR:
		case MAPPING_ERROR:
			status = HttpStatus.BAD_GATEWAY; // 502 - invalid response from upstream
			log.error("LLM response parsing error ({}): {}", ex.getReason(), ex.getMessage(), ex);
			break;
		case INVALID_RESPONSE:
			status = HttpStatus.BAD_GATEWAY;
			log.error("LLM invalid response: {}", ex.getMessage(), ex);
			break;
		case OTHER:
		default:
			status = HttpStatus.INTERNAL_SERVER_ERROR;
			log.error("LLM response parsing error (OTHER): {}", ex.getMessage(), ex);
			break;
		}
		HttpErrorInfo responseBody = createHttpErrorInfo(status, request, ex);
		return handleExceptionInternal(ex, responseBody, new HttpHeaders(), status, request);
	}

	// Document duplicate exceptions
	@ExceptionHandler(DuplicateDocumentNameException.class)
	public ResponseEntity<Object> handleDuplicateDocumentNameException(WebRequest request,
			DuplicateDocumentNameException ex) {
		HttpErrorInfo responseBody = createHttpErrorInfo(HttpStatus.CONFLICT, request, ex);
		log.warn("Duplicate document name: {}", ex.getMessage());
		return handleExceptionInternal(ex, responseBody, new HttpHeaders(), HttpStatus.CONFLICT, request);
	}

	@ExceptionHandler(DuplicateDocumentContentException.class)
	public ResponseEntity<Object> handleDuplicateDocumentContentException(WebRequest request,
			DuplicateDocumentContentException ex) {
		HttpErrorInfo responseBody = createHttpErrorInfo(HttpStatus.CONFLICT, request, ex);
		log.warn("Duplicate document content: {}", ex.getMessage());
		return handleExceptionInternal(ex, responseBody, new HttpHeaders(), HttpStatus.CONFLICT, request);
	}

	// -------- helper methods --------

	private HttpErrorInfo createHttpErrorInfo(HttpStatus httpStatus, WebRequest request, Exception ex) {
		final String path = extractPath(request);
		final String message = ex.getMessage();

		log.error("Returning HTTP status: {} for path: {}, message: {}", httpStatus, path, message);
		return new HttpErrorInfo(httpStatus, path, message);
	}

	private HttpErrorInfo createHttpErrorInfo(HttpStatus httpStatus, WebRequest request, String message) {
		final String path = extractPath(request);
		log.error("Returning HTTP status: {} for path: {}, message: {}", httpStatus, path, message);
		return new HttpErrorInfo(httpStatus, path, message);
	}

	private HttpErrorInfo createMethodArgumentNotValidHttpErrorInfo(HttpStatus httpStatus, WebRequest request,
			MethodArgumentNotValidException ex) {

		final String path = extractPath(request);
		final String message = ex.getBindingResult().getAllErrors().stream().map(ObjectError::getDefaultMessage)
				.collect(Collectors.joining("; "));

		log.error("Returning HTTP status: {} for path: {}, message: {}", httpStatus, path, message);
		return new HttpErrorInfo(httpStatus, path, message);
	}

	private String extractPath(WebRequest request) {
		// typically returns something like "uri=/api/v1/transactions/123"
		String desc = request.getDescription(false);
		if (desc != null && desc.startsWith("uri=")) {
			return desc.substring(4);
		}
		return desc;
	}
}
