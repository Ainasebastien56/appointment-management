
package com.sebastien.appointmentmanagement.controller;

import com.sebastien.appointmentmanagement.config.SecurityConfig;
import com.sebastien.appointmentmanagement.dto.AppointmentRequest;
import com.sebastien.appointmentmanagement.dto.AppointmentResponse;
import com.sebastien.appointmentmanagement.dto.DoctorAppointmentStatisticsResponse;
import com.sebastien.appointmentmanagement.dto.UpdateAppointmentStatusRequest;
import com.sebastien.appointmentmanagement.entity.*;
import com.sebastien.appointmentmanagement.exception.AppointmentNotFoundException;
import com.sebastien.appointmentmanagement.service.AppointmentService;
import com.sebastien.appointmentmanagement.service.CurrentUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;
// Importe l'outil de test qui ajoute un jeton CSRF à une requête.
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AppointmentController.class)

// Importe explicitement notre configuration de sécurité.
@Import(SecurityConfig.class)

@WithMockUser
class AppointmentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AppointmentService appointmentService;

    @MockitoBean
    private CurrentUserService currentUserService;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUpCurrentUser() {

        // Créer un utilisateur administrateur pour les tests.
        User adminUser = new User(
                "admin@example.com",
                "test-password",
                UserRole.ADMIN
        );

        // Retourner cet utilisateur à chaque recherche d'utilisateur.
        when(currentUserService.requireUser(anyString()))
                .thenReturn(adminUser);
    }

    private AppointmentRequest validRequest() {
        AppointmentRequest request = new AppointmentRequest();
        request.setDoctorId(2L);
        request.setAppointmentDate(LocalDate.of(2026, 10, 20));
        request.setStartTime(LocalTime.of(9, 0));
        request.setType(AppointmentType.CONSULTATION);
        request.setReason("Routine check-up");
        request.setNotes("Test notes");
        return request;
    }

    private AppointmentResponse createResponse(
            Long id,
            AppointmentStatus status) {

        AppointmentResponse response = new AppointmentResponse();
        response.setId(id);
        response.setPatientId(10L);
        response.setPatientName("Jean Dupont");
        response.setDoctorId(20L);
        response.setDoctorName("Dr Martin");
        response.setAppointmentDate(LocalDate.of(2026, 10, 20));
        response.setStartTime(LocalTime.of(9, 0));
        response.setEndTime(LocalTime.of(9, 30));
        response.setType(AppointmentType.CONSULTATION);
        response.setReason("Routine check-up");
        response.setStatus(status);
        response.setNotes("Test notes");
        return response;
    }

    @Test
    void shouldReturnAppointmentById() throws Exception {
        AppointmentResponse response =
                createResponse(1L, AppointmentStatus.SCHEDULED);

        when(appointmentService.getAppointmentById(1L))
                .thenReturn(response);

        mockMvc.perform(get("/api/appointments/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.patientName").value("Jean Dupont"))
                .andExpect(jsonPath("$.doctorName").value("Dr Martin"))
                .andExpect(jsonPath("$.appointmentDate").value("2026-10-20"))
                .andExpect(jsonPath("$.startTime").value("09:00:00"))
                .andExpect(jsonPath("$.endTime").value("09:30:00"))
                .andExpect(jsonPath("$.status").value("SCHEDULED"));

        verify(appointmentService).getAppointmentById(1L);
    }

    @Test
    void shouldReturn404WhenAppointmentDoesNotExist() throws Exception {
        when(appointmentService.getAppointmentById(999L))
                .thenThrow(new AppointmentNotFoundException(999L));

        mockMvc.perform(get("/api/appointments/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message")
                        .value("No appointment found with id: 999"));
    }

    @Test
    void shouldRejectAdminWhenCreatingAppointmentThroughPatientEndpoint()
            throws Exception {

        // Préparer les données de la requête.
        String jsonRequest =
                objectMapper.writeValueAsString(validRequest());

        // L'utilisateur configuré dans setUpCurrentUser() est ADMIN.
        // Un administrateur ne peut pas utiliser le parcours réservé
        // aux patients pour créer un rendez-vous pour lui-même.
        mockMvc.perform(
                        post("/api/appointments")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(jsonRequest)
                )
                .andExpect(status().isForbidden());

        // Le service métier ne doit pas être appelé.
        verifyNoInteractions(appointmentService);
    }

    @Test
    void shouldReturn400WhenRequestIsInvalid() throws Exception {
        AppointmentRequest request = validRequest();
        request.setReason("");

        String jsonRequest = objectMapper.writeValueAsString(request);

        mockMvc.perform(
                        post("/api/appointments")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(jsonRequest)
                )
                .andExpect(status().isBadRequest());

        verifyNoInteractions(appointmentService);
    }

    @Test
    void shouldUpdateAppointment() throws Exception {
        AppointmentRequest request = validRequest();
        String jsonRequest = objectMapper.writeValueAsString(request);

        AppointmentResponse response =
                createResponse(1L, AppointmentStatus.SCHEDULED);

        when(appointmentService.updateAppointment(
                eq(1L), any(AppointmentRequest.class)
        )).thenReturn(response);

        mockMvc.perform(
                        put("/api/appointments/1")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(jsonRequest)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.patientName").value("Jean Dupont"))
                .andExpect(jsonPath("$.doctorName").value("Dr Martin"))
                .andExpect(jsonPath("$.status").value("SCHEDULED"));

        verify(appointmentService).updateAppointment(
                eq(1L), any(AppointmentRequest.class)
        );
    }

    @Test
    void shouldReturn404WhenUpdatingAppointmentDoesNotExist()
            throws Exception {

        String jsonRequest =
                objectMapper.writeValueAsString(validRequest());

        when(appointmentService.updateAppointment(
                eq(999L), any(AppointmentRequest.class)
        )).thenThrow(new AppointmentNotFoundException(999L));

        mockMvc.perform(
                        put("/api/appointments/999")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(jsonRequest)
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message")
                        .value("No appointment found with id: 999"));
    }

    @Test
    void shouldCancelAppointmentAsAdmin() throws Exception {

        mockMvc.perform(delete("/api/appointments/1"))
                .andExpect(status().isNoContent());

        verify(appointmentService).cancelAppointment(
                eq(1L),
                argThat(user -> user.getRole() == UserRole.ADMIN),
                isNull(),
                isNull()
        );
    }

    @Test
    void shouldForwardOptionalCancellationReasonForPatient() throws Exception {

        User patientUser = new User(
                "patient@example.com",
                "test-password",
                UserRole.PATIENT
        );

        Patient patient = mock(Patient.class);

        when(currentUserService.requireUser(anyString()))
                .thenReturn(patientUser);

        when(currentUserService.requirePatientProfile(patientUser))
                .thenReturn(patient);

        mockMvc.perform(
                        delete("/api/appointments/1")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"reason\":\"I have a work conflict.\"}")
                )
                .andExpect(status().isNoContent());

        verify(appointmentService).cancelAppointment(
                1L,
                patientUser,
                patient,
                "I have a work conflict."
        );
    }

    @Test
    void shouldReturn404WhenCancellingAppointmentDoesNotExist()
            throws Exception {

        when(appointmentService.cancelAppointment(
                eq(999L),
                any(User.class),
                isNull(),
                isNull()
        )).thenThrow(new AppointmentNotFoundException(999L));

        mockMvc.perform(delete("/api/appointments/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message")
                        .value("No appointment found with id: 999"));
    }

    @Test
    void shouldReturnAppointments() throws Exception {
        AppointmentResponse response =
                createResponse(1L, AppointmentStatus.SCHEDULED);

        Page<AppointmentResponse> page =
                new PageImpl<>(List.of(response));

        when(appointmentService.getAppointments(
                isNull(), isNull(), any(Pageable.class)
        )).thenReturn(page);

        mockMvc.perform(get("/api/appointments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].patientName")
                        .value("Jean Dupont"))
                .andExpect(jsonPath("$.content[0].doctorName")
                        .value("Dr Martin"))
                .andExpect(jsonPath("$.content[0].status")
                        .value("SCHEDULED"));

        verify(appointmentService).getAppointments(
                isNull(), isNull(), any(Pageable.class)
        );
    }

    @Test
    void shouldReturnAppointmentsFilteredByDoctorLastName()
            throws Exception {

        AppointmentResponse response =
                createResponse(1L, AppointmentStatus.SCHEDULED);

        Page<AppointmentResponse> page =
                new PageImpl<>(List.of(response));

        when(appointmentService.getAppointments(
                eq("Martin"), isNull(), any(Pageable.class)
        )).thenReturn(page);

        mockMvc.perform(
                        get("/api/appointments")
                                .param("doctorLastName", "Martin")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].doctorName")
                        .value("Dr Martin"));

        verify(appointmentService).getAppointments(
                eq("Martin"), isNull(), any(Pageable.class)
        );
    }

    @Test
    void shouldReturnAppointmentsFilteredByStatus() throws Exception {
        AppointmentResponse response =
                createResponse(1L, AppointmentStatus.COMPLETED);

        Page<AppointmentResponse> page =
                new PageImpl<>(List.of(response));

        when(appointmentService.getAppointments(
                isNull(), eq(AppointmentStatus.COMPLETED),
                any(Pageable.class)
        )).thenReturn(page);

        mockMvc.perform(
                        get("/api/appointments")
                                .param("status", "COMPLETED")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].status")
                        .value("COMPLETED"));

        verify(appointmentService).getAppointments(
                isNull(), eq(AppointmentStatus.COMPLETED),
                any(Pageable.class)
        );
    }

    @Test
    void shouldReturnAppointmentsFilteredByDoctorAndStatus()
            throws Exception {

        AppointmentResponse response =
                createResponse(1L, AppointmentStatus.COMPLETED);

        Page<AppointmentResponse> page =
                new PageImpl<>(List.of(response));

        when(appointmentService.getAppointments(
                eq("Martin"), eq(AppointmentStatus.COMPLETED),
                any(Pageable.class)
        )).thenReturn(page);

        mockMvc.perform(
                        get("/api/appointments")
                                .param("doctorLastName", "Martin")
                                .param("status", "COMPLETED")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].doctorName")
                        .value("Dr Martin"))
                .andExpect(jsonPath("$.content[0].status")
                        .value("COMPLETED"));

        verify(appointmentService).getAppointments(
                eq("Martin"), eq(AppointmentStatus.COMPLETED),
                any(Pageable.class)
        );
    }

    @Test
    void shouldHandlePagination() throws Exception {
        AppointmentResponse response =
                createResponse(1L, AppointmentStatus.SCHEDULED);

        Page<AppointmentResponse> page =
                new PageImpl<>(List.of(response));

        when(appointmentService.getAppointments(
                isNull(), isNull(), any(Pageable.class)
        )).thenReturn(page);

        mockMvc.perform(
                        get("/api/appointments")
                                .param("page", "0")
                                .param("size", "10")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(1));

        org.mockito.ArgumentCaptor<Pageable> pageableCaptor =
                org.mockito.ArgumentCaptor.forClass(Pageable.class);

        verify(appointmentService).getAppointments(
                isNull(), isNull(), pageableCaptor.capture()
        );

        Pageable pageable = pageableCaptor.getValue();

        assertEquals(0, pageable.getPageNumber());
        assertEquals(10, pageable.getPageSize());
    }

    @Test
    void shouldHandleSortingByAppointmentDateDescending()
            throws Exception {

        AppointmentResponse response =
                createResponse(1L, AppointmentStatus.SCHEDULED);

        Page<AppointmentResponse> page =
                new PageImpl<>(List.of(response));

        when(appointmentService.getAppointments(
                isNull(), isNull(), any(Pageable.class)
        )).thenReturn(page);

        mockMvc.perform(
                        get("/api/appointments")
                                .param("page", "0")
                                .param("size", "10")
                                .param("sort", "appointmentDate,desc")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1));

        org.mockito.ArgumentCaptor<Pageable> pageableCaptor =
                org.mockito.ArgumentCaptor.forClass(Pageable.class);

        verify(appointmentService).getAppointments(
                isNull(), isNull(), pageableCaptor.capture()
        );

        Pageable pageable = pageableCaptor.getValue();

        assertEquals(
                Sort.Direction.DESC,
                pageable.getSort()
                        .getOrderFor("appointmentDate")
                        .getDirection()
        );
    }

    @Test
    void shouldReturn400WhenStatusIsInvalid() throws Exception {
        mockMvc.perform(
                        get("/api/appointments")
                                .param("status", "INVALID_STATUS")
                )
                .andExpect(status().isBadRequest());

        verifyNoInteractions(appointmentService);
    }

    @Test
    void shouldUpdateAppointmentStatusAsAdmin() throws Exception {

        User admin = new User(
                "admin@example.com",
                "test-password",
                UserRole.ADMIN
        );

        when(currentUserService.requireUser(anyString()))
                .thenReturn(admin);

        UpdateAppointmentStatusRequest request =
                new UpdateAppointmentStatusRequest();

        request.setStatus(AppointmentStatus.COMPLETED);

        AppointmentResponse response =
                createResponse(1L, AppointmentStatus.COMPLETED);

        when(appointmentService.updateAppointmentStatus(
                eq(1L),
                eq(AppointmentStatus.COMPLETED),
                eq(admin),
                isNull(),
                isNull()
        )).thenReturn(response);

        mockMvc.perform(
                        patch("/api/appointments/1/status")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("COMPLETED"));

        verify(appointmentService).updateAppointmentStatus(
                1L,
                AppointmentStatus.COMPLETED,
                admin,
                null,
                null
        );
    }

    @Test
    void shouldCorrectNoShowAppointmentAsAdmin() throws Exception {

        User admin = new User(
                "admin@example.com",
                "test-password",
                UserRole.ADMIN
        );

        when(currentUserService.requireUser(anyString()))
                .thenReturn(admin);

        UpdateAppointmentStatusRequest request =
                new UpdateAppointmentStatusRequest();

        request.setStatus(AppointmentStatus.COMPLETED);
        request.setChangeReason("Patient attendance confirmed after verification.");

        AppointmentResponse response =
                createResponse(1L, AppointmentStatus.COMPLETED);

        when(appointmentService.updateAppointmentStatus(
                eq(1L),
                eq(AppointmentStatus.COMPLETED),
                eq(admin),
                isNull(),
                eq("Patient attendance confirmed after verification.")
        )).thenReturn(response);

        mockMvc.perform(
                        patch("/api/appointments/1/status")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));

        verify(appointmentService).updateAppointmentStatus(
                1L,
                AppointmentStatus.COMPLETED,
                admin,
                null,
                "Patient attendance confirmed after verification."
        );
    }

    @Test
    void shouldUpdateAppointmentStatusAsAssignedDoctor() throws Exception {

        User doctorUser = new User(
                "doctor@example.com",
                "test-password",
                UserRole.DOCTOR
        );

        Doctor doctor = mock(Doctor.class);

        when(currentUserService.requireUser(anyString()))
                .thenReturn(doctorUser);

        when(currentUserService.requireDoctorProfile(doctorUser))
                .thenReturn(doctor);

        UpdateAppointmentStatusRequest request =
                new UpdateAppointmentStatusRequest();

        request.setStatus(AppointmentStatus.NO_SHOW);

        AppointmentResponse response =
                createResponse(1L, AppointmentStatus.NO_SHOW);

        when(appointmentService.updateAppointmentStatus(
                eq(1L),
                eq(AppointmentStatus.NO_SHOW),
                eq(doctorUser),
                eq(doctor),
                isNull()
        )).thenReturn(response);

        mockMvc.perform(
                        patch("/api/appointments/1/status")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("NO_SHOW"));

        verify(currentUserService).requireDoctorProfile(doctorUser);

        verify(appointmentService).updateAppointmentStatus(
                1L,
                AppointmentStatus.NO_SHOW,
                doctorUser,
                doctor,
                null
        );
    }

    @Test
    void shouldReturnDoctorStatisticsForDoctor() throws Exception {

        User doctorUser = new User(
                "doctor@example.com",
                "test-password",
                UserRole.DOCTOR
        );

        Doctor doctor = mock(Doctor.class);
        when(doctor.getId()).thenReturn(20L);

        when(currentUserService.requireUser(anyString()))
                .thenReturn(doctorUser);

        when(currentUserService.requireDoctorProfile(doctorUser))
                .thenReturn(doctor);

        when(appointmentService.getDoctorAppointmentStatistics(20L))
                .thenReturn(new DoctorAppointmentStatisticsResponse(2, 5, 1, 7));

        mockMvc.perform(get("/api/appointments/my-doctor-statistics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.todayAppointments").value(2))
                .andExpect(jsonPath("$.upcomingAppointments").value(5))
                .andExpect(jsonPath("$.pendingOutcomes").value(1))
                .andExpect(jsonPath("$.completedAppointments").value(7));

        verify(appointmentService).getDoctorAppointmentStatistics(20L);
    }

    @Test
    void shouldRejectPatientFromDoctorStatistics() throws Exception {

        User patientUser = new User(
                "patient@example.com",
                "test-password",
                UserRole.PATIENT
        );

        when(currentUserService.requireUser(anyString()))
                .thenReturn(patientUser);

        mockMvc.perform(get("/api/appointments/my-doctor-statistics"))
                .andExpect(status().isForbidden());

        verify(appointmentService, never()).getDoctorAppointmentStatistics(any());
    }

    @Test
    void shouldRejectAdminFromDoctorStatistics() throws Exception {

        // L'utilisateur par défaut défini dans setUpCurrentUser est un ADMIN.
        mockMvc.perform(get("/api/appointments/my-doctor-statistics"))
                .andExpect(status().isForbidden());

        verify(appointmentService, never()).getDoctorAppointmentStatistics(any());
    }

    @Test
    void shouldCancelAppointmentAsAssignedDoctorWithReason() throws Exception {

        User doctorUser = new User(
                "doctor@example.com",
                "test-password",
                UserRole.DOCTOR
        );

        Doctor doctor = mock(Doctor.class);

        when(currentUserService.requireUser(anyString()))
                .thenReturn(doctorUser);

        when(currentUserService.requireDoctorProfile(doctorUser))
                .thenReturn(doctor);

        UpdateAppointmentStatusRequest request =
                new UpdateAppointmentStatusRequest();
        request.setStatus(AppointmentStatus.CANCELLED);
        request.setChangeReason("Doctor unavailable due to an emergency.");

        AppointmentResponse response =
                createResponse(1L, AppointmentStatus.CANCELLED);
        response.setCancellationReason("Doctor unavailable due to an emergency.");

        when(appointmentService.updateAppointmentStatus(
                eq(1L),
                eq(AppointmentStatus.CANCELLED),
                eq(doctorUser),
                eq(doctor),
                eq("Doctor unavailable due to an emergency.")
        )).thenReturn(response);

        mockMvc.perform(
                        patch("/api/appointments/1/status")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.cancellationReason")
                        .value("Doctor unavailable due to an emergency."));

        verify(currentUserService).requireDoctorProfile(doctorUser);
    }

    @Test
    void shouldPassTemporalAndDateFiltersForDoctorUsingProfileId()
            throws Exception {

        User doctorUser = new User(
                "doctor@example.com",
                "test-password",
                UserRole.DOCTOR
        );

        Doctor doctor = mock(Doctor.class);
        when(doctor.getId()).thenReturn(20L);

        when(currentUserService.requireUser(anyString()))
                .thenReturn(doctorUser);

        when(currentUserService.requireDoctorProfile(doctorUser))
                .thenReturn(doctor);

        Page<AppointmentResponse> page = new PageImpl<>(
                List.of(createResponse(1L, AppointmentStatus.SCHEDULED))
        );

        when(appointmentService.getAppointmentsForDoctor(
                eq(20L),
                isNull(),
                any(Pageable.class),
                eq(true),
                eq(false),
                eq(false),
                eq(LocalDate.of(2026, 10, 20))
        )).thenReturn(page);

        mockMvc.perform(
                        get("/api/appointments")
                                .param("upcomingOnly", "true")
                                .param("date", "2026-10-20")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].doctorId").value(20));

        verify(currentUserService).requireDoctorProfile(doctorUser);

        verify(appointmentService).getAppointmentsForDoctor(
                eq(20L),
                isNull(),
                any(Pageable.class),
                eq(true),
                eq(false),
                eq(false),
                eq(LocalDate.of(2026, 10, 20))
        );
    }

    @Test
    void shouldRejectPatientWhenUpdatingAppointmentStatus() throws Exception {

        User patient = new User(
                "patient@example.com",
                "test-password",
                UserRole.PATIENT
        );

        when(currentUserService.requireUser(anyString()))
                .thenReturn(patient);

        UpdateAppointmentStatusRequest request =
                new UpdateAppointmentStatusRequest();

        request.setStatus(AppointmentStatus.COMPLETED);

        mockMvc.perform(
                        patch("/api/appointments/1/status")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isForbidden());

        verifyNoInteractions(appointmentService);
    }

    @Test
    void shouldReturn400WhenAppointmentStatusIsMissing() throws Exception {

        String jsonRequest = """
            {
                "changeReason": "Test reason"
            }
            """;

        mockMvc.perform(
                        patch("/api/appointments/1/status")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(jsonRequest)
                )
                .andExpect(status().isBadRequest());

        verifyNoInteractions(appointmentService);
    }
}
