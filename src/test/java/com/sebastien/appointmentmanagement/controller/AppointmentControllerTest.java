package com.sebastien.appointmentmanagement.controller;

import com.sebastien.appointmentmanagement.dto.AppointmentRequest;
import com.sebastien.appointmentmanagement.dto.AppointmentResponse;
import com.sebastien.appointmentmanagement.entity.AppointmentStatus;
import com.sebastien.appointmentmanagement.exception.AppointmentNotFoundException;
import com.sebastien.appointmentmanagement.service.AppointmentService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AppointmentController.class)
public class AppointmentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AppointmentService appointmentService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldReturnAppointmentById() throws Exception {

        AppointmentResponse response = new AppointmentResponse(
                1L,
                "Jean Dupont",
                "Dr Martin",
                LocalDate.of(2026, 10, 1),
                AppointmentStatus.SCHEDULED
        );

        when(appointmentService.getAppointmentById(1L))
                .thenReturn(response);

        mockMvc.perform(
                get("/api/appointments/1")
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.patientName").value("Jean Dupont"))
                .andExpect(jsonPath("$.doctorName").value("Dr Martin"))
                .andExpect(jsonPath("$.appointmentDate").value("2026-10-01"))
                .andExpect(jsonPath("$.status").value("SCHEDULED"));

    }

    @Test
    void shouldReturn404WhenAppointmentDoesNotExist() throws Exception{
        when(appointmentService.getAppointmentById(999L))
                .thenThrow(new AppointmentNotFoundException(999L));

        mockMvc.perform(
                get("/api/appointments/999")
        )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("No appointment found with id: 999"));
    }

    @Test
    void shouldCreateAppointment() throws Exception{

        AppointmentRequest request = new AppointmentRequest(
                "Jean Dupont",
                "Dr Martin",
                LocalDate.of(2026, 10, 1),
                AppointmentStatus.SCHEDULED
        );

        String jsonRequest = objectMapper.writeValueAsString(request);

        AppointmentResponse response = new AppointmentResponse(
                1L,
                "Jean Dupont",
                "Dr Martin",
                LocalDate.of(2026, 10, 1),
                AppointmentStatus.SCHEDULED
        );

        when(appointmentService.createAppointment(any(AppointmentRequest.class)))
                .thenReturn(response);

        mockMvc.perform(
                post("/api/appointments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest)
        )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.patientName").value("Jean Dupont"))
                .andExpect(jsonPath("$.doctorName").value("Dr Martin"))
                .andExpect(jsonPath("$.appointmentDate").value("2026-10-01"))
                .andExpect(jsonPath("$.status").value("SCHEDULED"));

        verify(appointmentService).createAppointment(any(AppointmentRequest.class));
    }

    @Test
    void shouldReturn400WhenRequestIsInvalid() throws Exception{
        AppointmentRequest request = new AppointmentRequest(
                "",
                "Dr Martin",
                LocalDate.of(2026, 10, 7),
                AppointmentStatus.SCHEDULED
        );

        String jsonRequest = objectMapper.writeValueAsString(request);

        mockMvc.perform(
                post("/api/appointments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest)
        )
                .andExpect(status().isBadRequest());

        verify(appointmentService, never())
                .createAppointment(any(AppointmentRequest.class));
    }

    @Test
    void shouldUpdateAppointment() throws Exception{
        AppointmentRequest request = new AppointmentRequest(
                "Jean Dupont",
                "Dr Martin",
                LocalDate.of(2026, 10, 1),
                AppointmentStatus.SCHEDULED
        );

        String jsonRequest = objectMapper.writeValueAsString(request);

        AppointmentResponse response = new AppointmentResponse(
                1L,
                "Jean Dupont",
                "Dr Martin",
                LocalDate.of(2026, 10, 1),
                AppointmentStatus.SCHEDULED
        );

        when(appointmentService.updateAppointment(eq(1L), any(AppointmentRequest.class)))
                .thenReturn(response);

        mockMvc.perform(
                put("/api/appointments/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest)
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.patientName").value("Jean Dupont"))
                .andExpect(jsonPath("$.doctorName").value("Dr Martin"))
                .andExpect(jsonPath("$.appointmentDate").value("2026-10-01"))
                .andExpect(jsonPath("$.status").value("SCHEDULED"));

        verify(appointmentService).updateAppointment(eq(1L), any(AppointmentRequest.class));
    }

    @Test
    void shouldReturn404WhenUpdatingAppointmentDoesNotExist() throws Exception{

        AppointmentRequest request = new AppointmentRequest(
                "Jean Dupont",
                "Dr Martin",
                LocalDate.of(2026, 10, 1),
                AppointmentStatus.SCHEDULED
        );

        String jsonRequest = objectMapper.writeValueAsString(request);
        when(appointmentService.updateAppointment(eq(999L), any(AppointmentRequest.class)))
                .thenThrow(new AppointmentNotFoundException(999L));


        mockMvc.perform(
                        put("/api/appointments/999")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(jsonRequest)
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("No appointment found with id: 999"));
    }

    @Test
    void shouldDeleteAppointment() throws Exception{

        mockMvc.perform(
                delete("/api/appointments/1")
        )
                .andExpect(status().isNoContent());

        verify(appointmentService).deleteAppointment(1L);
    }

    @Test
    void shouldReturn404WhenDeletingAppointmentDoesNotExist() throws Exception{

        doThrow(new AppointmentNotFoundException(999L))
                .when(appointmentService).deleteAppointment(999L);

        mockMvc.perform(
                delete("/api/appointments/999")
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("No appointment found with id: 999"));
    }

    @Test
    void shouldReturnAppointments() throws Exception{

        AppointmentResponse response = new AppointmentResponse(
                1L,
                "Jean Dupont",
                "Dr Martin",
                LocalDate.of(2026, 10, 1),
                AppointmentStatus.SCHEDULED
        );

        Page<AppointmentResponse> page =
                new PageImpl<>(List.of(response));

        when(appointmentService.getAppointments(
                isNull(),
                isNull(),
                any(Pageable.class)
        )).thenReturn(page);

        mockMvc.perform(
                get("/api/appointments")
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].patientName").value("Jean Dupont"))
                .andExpect(jsonPath("$.content[0].doctorName").value("Dr Martin"))
                .andExpect(jsonPath("$.content[0].appointmentDate").value("2026-10-01"))
                .andExpect(jsonPath("$.content[0].status").value("SCHEDULED"));

        verify(appointmentService).getAppointments(
                isNull(),
                isNull(),
                any(Pageable.class)
        );
    }

    @Test
    void shouldReturnAppointmentsFilteredByDoctor() throws Exception {
        AppointmentResponse response = new AppointmentResponse(
                1L,
                "Jean Dupont",
                "Dr Martin",
                LocalDate.of(2026, 10, 1),
                AppointmentStatus.SCHEDULED
        );

        Page<AppointmentResponse> page =
                new PageImpl<>(List.of(response));

        when(appointmentService.getAppointments(
                eq("Dr Martin"),
                isNull(),
                any(Pageable.class)
        )).thenReturn(page);

        mockMvc.perform(
                get("/api/appointments")
                        .param("doctorName", "Dr Martin")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].patientName").value("Jean Dupont"))
                .andExpect(jsonPath("$.content[0].doctorName").value("Dr Martin"))
                .andExpect(jsonPath("$.content[0].appointmentDate").value("2026-10-01"))
                .andExpect(jsonPath("$.content[0].status").value("SCHEDULED"));

        verify(appointmentService).getAppointments(
                eq("Dr Martin"),
                isNull(),
                any(Pageable.class)
        );
    }

    @Test
    void shouldReturnAppointmentsFilteredByStatus() throws Exception {
        AppointmentResponse response = new AppointmentResponse(
                1L,
                "Jean Dupont",
                "Dr Martin",
                LocalDate.of(2026, 10, 1),
                AppointmentStatus.COMPLETED
        );

        Page<AppointmentResponse> page =
                new PageImpl<>(List.of(response));

        when(appointmentService.getAppointments(
                isNull(),
                eq(AppointmentStatus.COMPLETED),
                any(Pageable.class)
        )).thenReturn(page);

        mockMvc.perform(
                        get("/api/appointments")
                                .param("status", "COMPLETED")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].patientName").value("Jean Dupont"))
                .andExpect(jsonPath("$.content[0].doctorName").value("Dr Martin"))
                .andExpect(jsonPath("$.content[0].appointmentDate").value("2026-10-01"))
                .andExpect(jsonPath("$.content[0].status").value("COMPLETED"));

        verify(appointmentService).getAppointments(
                isNull(),
                eq(AppointmentStatus.COMPLETED),
                any(Pageable.class)
        );
    }

    @Test
    void shouldReturnAppointmentsFilteredByDoctorNameAndStatus() throws Exception {
        AppointmentResponse response = new AppointmentResponse(
                1L,
                "Jean Dupont",
                "Dr Martin",
                LocalDate.of(2026, 10, 1),
                AppointmentStatus.COMPLETED
        );

        Page<AppointmentResponse> page =
                new PageImpl<>(List.of(response));

        when(appointmentService.getAppointments(
                eq("Dr Martin"),
                eq(AppointmentStatus.COMPLETED),
                any(Pageable.class)
        )).thenReturn(page);

        mockMvc.perform(
                        get("/api/appointments")
                                .param("doctorName", "Dr Martin")
                                .param("status", "COMPLETED")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].patientName").value("Jean Dupont"))
                .andExpect(jsonPath("$.content[0].doctorName").value("Dr Martin"))
                .andExpect(jsonPath("$.content[0].appointmentDate").value("2026-10-01"))
                .andExpect(jsonPath("$.content[0].status").value("COMPLETED"));

        verify(appointmentService).getAppointments(
                eq("Dr Martin"),
                eq(AppointmentStatus.COMPLETED),
                any(Pageable.class)
        );
    }

    @Test
    void shouldHandlePagination() throws Exception {
        AppointmentResponse response = new AppointmentResponse(
                1L,
                "Jean Dupont",
                "Dr Martin",
                LocalDate.of(2026, 10, 1),
                AppointmentStatus.SCHEDULED
        );

        Page<AppointmentResponse> page =
                new PageImpl<>(List.of(response));

        when(appointmentService.getAppointments(
                isNull(),
                isNull(),
                any(Pageable.class)
        )).thenReturn(page);

        mockMvc.perform(
                        get("/api/appointments")
                                .param("page", "0")
                                .param("size", "10")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(1));

        ArgumentCaptor<Pageable> pageableCaptor =
                ArgumentCaptor.forClass(Pageable.class);

        verify(appointmentService).getAppointments(
                isNull(),
                isNull(),
                pageableCaptor.capture()
        );

        Pageable pageable = pageableCaptor.getValue();

        assertEquals(
                0,
                pageable.getPageNumber()
        );

        assertEquals(
                10,
                pageable.getPageSize()
        );
    }

    @Test
    void shouldHandleSortingByAppointmentDateDescending() throws Exception {
        AppointmentResponse response = new AppointmentResponse(
                1L,
                "Jean Dupont",
                "Dr Martin",
                LocalDate.of(2026, 10, 1),
                AppointmentStatus.SCHEDULED
        );

        Page<AppointmentResponse> page =
                new PageImpl<>(List.of(response));

        when(appointmentService.getAppointments(
                isNull(),
                isNull(),
                any(Pageable.class)
        )).thenReturn(page);

        mockMvc.perform(
                        get("/api/appointments")
                                .param("page", "0")
                                .param("size", "10")
                                .param("sort", "appointmentDate,desc")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(1));

        ArgumentCaptor<Pageable> pageableCaptor =
                ArgumentCaptor.forClass(Pageable.class);

        verify(appointmentService).getAppointments(
                isNull(),
                isNull(),
                pageableCaptor.capture()
        );

        Pageable pageable = pageableCaptor.getValue();

        assertEquals(
                0,
                pageable.getPageNumber()
        );

        assertEquals(
                10,
                pageable.getPageSize()
        );

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