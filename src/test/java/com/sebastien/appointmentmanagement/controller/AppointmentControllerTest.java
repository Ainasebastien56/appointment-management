
package com.sebastien.appointmentmanagement.controller;

import com.sebastien.appointmentmanagement.config.SecurityConfig;
import com.sebastien.appointmentmanagement.dto.AppointmentRequest;
import com.sebastien.appointmentmanagement.dto.AppointmentResponse;
import com.sebastien.appointmentmanagement.entity.AppointmentStatus;
import com.sebastien.appointmentmanagement.entity.AppointmentType;
import com.sebastien.appointmentmanagement.entity.User;
import com.sebastien.appointmentmanagement.entity.UserRole;
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
                1L,
                UserRole.ADMIN,
                null
        );
    }

    @Test
    void shouldReturn404WhenCancellingAppointmentDoesNotExist()
            throws Exception {

        when(appointmentService.cancelAppointment(
                999L,
                UserRole.ADMIN,
                null
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
}
