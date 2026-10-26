package com.syncevent.events.api;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.syncevent.events.application.EventScheduleConflictException;
import com.syncevent.events.application.EventNotFoundException;
import com.syncevent.domain.model.InvalidEventStateTransitionException;
import com.syncevent.domain.model.EventModificationNotAllowedException;
import com.syncevent.domain.model.EventCancellationNotAllowedException;
import com.syncevent.domain.model.EventPostponementNotAllowedException;
import com.syncevent.participants.application.DuplicateParticipantException;
import com.syncevent.participants.application.ParticipantNotFoundException;
import com.syncevent.users.application.AuthenticationFailedException;
import com.syncevent.users.application.DuplicateUserEmailException;
import com.syncevent.users.application.LastAdministratorException;
import com.syncevent.users.application.ParticipantAlreadyLinkedException;
import com.syncevent.users.application.ParticipantLinkNotAllowedException;
import com.syncevent.users.application.UserAccountNotFoundException;
import com.syncevent.registrations.application.DuplicateRegistrationException;
import com.syncevent.registrations.application.GuestParticipantNotLinkedException;
import com.syncevent.registrations.application.RegistrationNotAllowedException;
import com.syncevent.registrations.application.RegistrationScheduleConflictException;
import com.syncevent.attendance.application.ActiveRegistrationNotFoundException;
import com.syncevent.attendance.application.CheckInAlreadyRecordedException;
import com.syncevent.attendance.application.CheckInNotAllowedException;
import com.syncevent.attendance.application.InvalidQrCodeException;
import com.syncevent.registrations.application.PendingFineRegistrationException;
import com.syncevent.attendance.domain.FineAlreadyResolvedException;
import com.syncevent.attendance.domain.FineNotFoundException;
import com.syncevent.registrations.application.RegistrationOwnershipException;
import com.syncevent.registrations.domain.RegistrationCancellationNotAllowedException;
import com.syncevent.registrations.domain.RegistrationNotFoundException;

import jakarta.validation.ConstraintViolationException;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleInvalidRequest(MethodArgumentNotValidException exception) {
        Map<String, String> errors = exception.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                        error -> error.getField(),
                        error -> error.getDefaultMessage() == null ? "Valor inválido" : error.getDefaultMessage(),
                        (first, ignored) -> first,
                        LinkedHashMap::new));

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, "Uno o más campos no cumplen las validaciones.");
        problem.setTitle("Solicitud inválida");
        problem.setProperty("errors", errors);
        return ResponseEntity.badRequest().body(problem);
    }

    @ExceptionHandler({IllegalArgumentException.class, ConstraintViolationException.class})
    public ResponseEntity<ProblemDetail> handleDomainValidation(RuntimeException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, exception.getMessage());
        problem.setTitle("Datos del evento inválidos");
        return ResponseEntity.badRequest().body(problem);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ProblemDetail> handleInvalidParameter(MethodArgumentTypeMismatchException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "El parámetro '%s' tiene un formato o valor no permitido.".formatted(exception.getName()));
        problem.setTitle("Parámetro inválido");
        return ResponseEntity.badRequest().body(problem);
    }

    @ExceptionHandler(EventScheduleConflictException.class)
    public ResponseEntity<ProblemDetail> handleScheduleConflict(EventScheduleConflictException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.getMessage());
        problem.setTitle("Conflicto de horario");
        return ResponseEntity.status(HttpStatus.CONFLICT).body(problem);
    }

    @ExceptionHandler(EventNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleEventNotFound(EventNotFoundException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
        problem.setTitle("Evento no encontrado");
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problem);
    }

    @ExceptionHandler(InvalidEventStateTransitionException.class)
    public ResponseEntity<ProblemDetail> handleInvalidStateTransition(
            InvalidEventStateTransitionException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.getMessage());
        problem.setTitle("Cambio de estado no permitido");
        return ResponseEntity.status(HttpStatus.CONFLICT).body(problem);
    }

    @ExceptionHandler(EventModificationNotAllowedException.class)
    public ResponseEntity<ProblemDetail> handleEventModificationNotAllowed(
            EventModificationNotAllowedException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.getMessage());
        problem.setTitle("Edición no permitida");
        return ResponseEntity.status(HttpStatus.CONFLICT).body(problem);
    }

    @ExceptionHandler(EventCancellationNotAllowedException.class)
    public ResponseEntity<ProblemDetail> handleEventCancellationNotAllowed(
            EventCancellationNotAllowedException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.getMessage());
        problem.setTitle("Cancelación no permitida");
        return ResponseEntity.status(HttpStatus.CONFLICT).body(problem);
    }

    @ExceptionHandler(EventPostponementNotAllowedException.class)
    public ResponseEntity<ProblemDetail> handleEventPostponementNotAllowed(
            EventPostponementNotAllowedException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.getMessage());
        problem.setTitle("Aplazamiento no permitido");
        return ResponseEntity.status(HttpStatus.CONFLICT).body(problem);
    }

    @ExceptionHandler(DuplicateParticipantException.class)
    public ResponseEntity<ProblemDetail> handleDuplicateParticipant(
            DuplicateParticipantException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.getMessage());
        problem.setTitle("Participante ya registrado");
        problem.setProperty("existingParticipantId", exception.getExistingParticipantId());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(problem);
    }

    @ExceptionHandler(ParticipantNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleParticipantNotFound(ParticipantNotFoundException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
        problem.setTitle("Participante no encontrado");
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problem);
    }

    @ExceptionHandler(DuplicateUserEmailException.class)
    public ResponseEntity<ProblemDetail> handleDuplicateUserEmail(DuplicateUserEmailException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.getMessage());
        problem.setTitle("Correo ya registrado");
        return ResponseEntity.status(HttpStatus.CONFLICT).body(problem);
    }

    @ExceptionHandler(AuthenticationFailedException.class)
    public ResponseEntity<ProblemDetail> handleAuthenticationFailed(AuthenticationFailedException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, exception.getMessage());
        problem.setTitle("Autenticación fallida");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(problem);
    }

    @ExceptionHandler(UserAccountNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleUserAccountNotFound(UserAccountNotFoundException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
        problem.setTitle("Usuario no encontrado");
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problem);
    }

    @ExceptionHandler(LastAdministratorException.class)
    public ResponseEntity<ProblemDetail> handleLastAdministrator(LastAdministratorException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.getMessage());
        problem.setTitle("Operación no permitida");
        return ResponseEntity.status(HttpStatus.CONFLICT).body(problem);
    }

    @ExceptionHandler({ParticipantAlreadyLinkedException.class, ParticipantLinkNotAllowedException.class})
    public ResponseEntity<ProblemDetail> handleParticipantLinkNotAllowed(RuntimeException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.getMessage());
        problem.setTitle("Vinculación no permitida");
        return ResponseEntity.status(HttpStatus.CONFLICT).body(problem);
    }

    @ExceptionHandler(GuestParticipantNotLinkedException.class)
    public ResponseEntity<ProblemDetail> handleGuestParticipantNotLinked(
            GuestParticipantNotLinkedException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.getMessage());
        problem.setTitle("Participante no vinculado");
        return ResponseEntity.status(HttpStatus.CONFLICT).body(problem);
    }

    @ExceptionHandler({DuplicateRegistrationException.class, RegistrationNotAllowedException.class})
    public ResponseEntity<ProblemDetail> handleRegistrationNotAllowed(RuntimeException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.getMessage());
        problem.setTitle("Inscripción no permitida");
        return ResponseEntity.status(HttpStatus.CONFLICT).body(problem);
    }

    @ExceptionHandler(RegistrationScheduleConflictException.class)
    public ResponseEntity<ProblemDetail> handleRegistrationScheduleConflict(
            RegistrationScheduleConflictException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.getMessage());
        problem.setTitle("Cruce de horario");
        problem.setProperty("conflictingEventId", exception.getConflictingEventId());
        problem.setProperty("conflictingEventName", exception.getConflictingEventName());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(problem);
    }

    @ExceptionHandler(InvalidQrCodeException.class)
    public ResponseEntity<ProblemDetail> handleInvalidQrCode(InvalidQrCodeException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, exception.getMessage());
        problem.setTitle("Código QR inválido");
        return ResponseEntity.badRequest().body(problem);
    }

    @ExceptionHandler(CheckInNotAllowedException.class)
    public ResponseEntity<ProblemDetail> handleCheckInNotAllowed(CheckInNotAllowedException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.getMessage());
        problem.setTitle("Check-in no permitido");
        return ResponseEntity.status(HttpStatus.CONFLICT).body(problem);
    }

    @ExceptionHandler(CheckInAlreadyRecordedException.class)
    public ResponseEntity<ProblemDetail> handleCheckInAlreadyRecorded(CheckInAlreadyRecordedException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.getMessage());
        problem.setTitle("Asistencia ya registrada");
        problem.setProperty("checkedInAt", exception.getCheckedInAt());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(problem);
    }

    @ExceptionHandler(ActiveRegistrationNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleActiveRegistrationNotFound(
            ActiveRegistrationNotFoundException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
        problem.setTitle("Inscripción activa no encontrada");
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problem);
    }

    @ExceptionHandler(PendingFineRegistrationException.class)
    public ResponseEntity<ProblemDetail> handlePendingFineRegistration(
            PendingFineRegistrationException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.getMessage());
        problem.setTitle("Inscripción bloqueada por multa pendiente");
        return ResponseEntity.status(HttpStatus.CONFLICT).body(problem);
    }

    @ExceptionHandler(FineNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleFineNotFound(FineNotFoundException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
        problem.setTitle("Multa no encontrada");
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problem);
    }

    @ExceptionHandler(FineAlreadyResolvedException.class)
    public ResponseEntity<ProblemDetail> handleFineAlreadyResolved(FineAlreadyResolvedException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.getMessage());
        problem.setTitle("Multa ya resuelta");
        return ResponseEntity.status(HttpStatus.CONFLICT).body(problem);
    }

    @ExceptionHandler(RegistrationOwnershipException.class)
    public ResponseEntity<ProblemDetail> handleRegistrationOwnership(RegistrationOwnershipException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, exception.getMessage());
        problem.setTitle("Cancelación no autorizada");
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(problem);
    }

    @ExceptionHandler(RegistrationNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleRegistrationNotFound(RegistrationNotFoundException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
        problem.setTitle("Inscripción no encontrada");
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problem);
    }

    @ExceptionHandler(RegistrationCancellationNotAllowedException.class)
    public ResponseEntity<ProblemDetail> handleRegistrationCancellationNotAllowed(
            RegistrationCancellationNotAllowedException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.getMessage());
        problem.setTitle("Cancelación de inscripción no permitida");
        return ResponseEntity.status(HttpStatus.CONFLICT).body(problem);
    }
}