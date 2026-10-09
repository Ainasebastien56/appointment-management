package com.sebastien.appointmentmanagement.service;

import com.sebastien.appointmentmanagement.dto.AppointmentRequest;
import com.sebastien.appointmentmanagement.dto.AppointmentResponse;
import com.sebastien.appointmentmanagement.entity.Appointment;
import com.sebastien.appointmentmanagement.entity.AppointmentStatus;
import com.sebastien.appointmentmanagement.exception.AppointmentNotFoundException;
import com.sebastien.appointmentmanagement.repository.AppointmentRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class AppointmentServiceTest {

    @Test
    void shouldReturnAppointmentById() {
        AppointmentRepository repository =
                mock(AppointmentRepository.class);

        AppointmentService service =
                new AppointmentService(repository);

        Appointment appointment = new Appointment(
                "Jean Dupont",
                "Dr Martin",
                LocalDate.of(2026, 10, 1),
                AppointmentStatus.SCHEDULED
        );

        when(repository.findById(1L))
                .thenReturn(Optional.of(appointment));

        AppointmentResponse response = service.getAppointmentById(1L);

        assertEquals(
                "Jean Dupont",
                response.getPatientName()
        );

        assertEquals(
                "Dr Martin",
                response.getDoctorName()
        );

        assertEquals(
                LocalDate.of(2026, 10, 1),
                response.getAppointmentDate()
        );

        assertEquals(
                AppointmentStatus.SCHEDULED,
                response.getStatus()
        );

        verify(repository).findById(1L);
    }

    @Test
    void shouldThrowExceptionWhenAppointmentDoesNotExist() {
        AppointmentRepository repository = mock(AppointmentRepository.class);

        AppointmentService service = new AppointmentService(repository);

        when(repository.findById(999L))
                .thenReturn(Optional.empty());

        AppointmentNotFoundException exception = assertThrows(
                AppointmentNotFoundException.class,
                () -> {
                    service.getAppointmentById(999L);
                }
        );

        assertEquals(
                "No appointment found with id: 999",
                exception.getMessage()
        );
    }

    @Test
    void shouldCreateAppointment() {
        AppointmentRepository repository = mock(AppointmentRepository.class);

        AppointmentService service = new AppointmentService(repository);

        AppointmentRequest request = new AppointmentRequest(
                "Jean Dupont",
                "Dr Martin",
                LocalDate.of(2026, 10, 1),
                AppointmentStatus.SCHEDULED
        );

        Appointment appointment = new Appointment(
                "Jean Dupont",
                "Dr Martin",
                LocalDate.of(2026, 10, 1),
                AppointmentStatus.SCHEDULED
        );

        when(repository.save(any(Appointment.class)))
                .thenReturn(appointment);

        AppointmentResponse response = service.createAppointment(request);

        ArgumentCaptor<Appointment> captor = ArgumentCaptor.forClass(Appointment.class);

        assertEquals(
                "Jean Dupont",
                response.getPatientName()
        );

        assertEquals(
                "Dr Martin",
                response.getDoctorName()
        );

        assertEquals(
                LocalDate.of(2026, 10, 1),
                response.getAppointmentDate()
        );

        assertEquals(
                AppointmentStatus.SCHEDULED,
                response.getStatus()
        );

        verify(repository).save(captor.capture());

        Appointment savedAppointment = captor.getValue();

        assertEquals(
                "Jean Dupont",
                savedAppointment.getPatientName()
        );

        assertEquals(
                "Dr Martin",
                savedAppointment.getDoctorName()
        );

        assertEquals(
                LocalDate.of(2026, 10, 1),
                savedAppointment.getAppointmentDate()
        );

        assertEquals(
                AppointmentStatus.SCHEDULED,
                savedAppointment.getStatus()
        );

    }

    @Test
    void shouldDeleteAppointment() {

        AppointmentRepository repository = mock(AppointmentRepository.class);

        AppointmentService service = new AppointmentService(repository);

        when(repository.existsById(1L))
                .thenReturn(true);

        service.deleteAppointment(1L);

        verify(repository).deleteById(1L);
    }

    @Test
    void shouldThrowExceptionWhenDeletingNonExistingAppointment() {
        AppointmentRepository repository = mock(AppointmentRepository.class);

        AppointmentService service = new AppointmentService(repository);

        when(repository.existsById(999L))
                .thenReturn(false);

        AppointmentNotFoundException exception = assertThrows(
                AppointmentNotFoundException.class,
                () -> service.deleteAppointment(999L)
        );

        assertEquals(
                "No appointment found with id: 999",
                exception.getMessage()
        );

        verify(repository, never()).deleteById(999L);
    }

    @Test
    void shouldUpdateAppointment(){
        AppointmentRepository repository = mock(AppointmentRepository.class);

        AppointmentService service = new AppointmentService(repository);

        Appointment appointment = new Appointment(
                "Jean Dupont",
                "Dr Martin",
                LocalDate.of(2026, 10, 1),
                AppointmentStatus.SCHEDULED
        );

        when(repository.findById(1L))
                .thenReturn(Optional.of(appointment));

        AppointmentRequest request = new AppointmentRequest(
                "Jean Dupont",
                "Dr Franclin",
                LocalDate.of(2026, 10, 6),
                AppointmentStatus.SCHEDULED
        );

        when(repository.save(any(Appointment.class)))
                .thenReturn(appointment);

        AppointmentResponse response = service.updateAppointment(1L, request);

        assertEquals(
                "Jean Dupont",
                response.getPatientName()
        );

        assertEquals(
                "Dr Franclin",
                response.getDoctorName()
        );

        assertEquals(
                LocalDate.of(2026, 10, 6),
                response.getAppointmentDate()
        );

        assertEquals(
                AppointmentStatus.SCHEDULED,
                response.getStatus()
        );

        verify(repository).findById(1L);
        verify(repository).save(any(Appointment.class));
    }

    @Test
    void shouldThrowExceptionWhenUpdatingNonExistingAppointment(){
        AppointmentRepository repository = mock(AppointmentRepository.class);

        AppointmentService service = new AppointmentService(repository);

        when(repository.findById(999L))
                .thenReturn(Optional.empty());

        AppointmentRequest request = new AppointmentRequest(
                "Jean Dupont",
                "Dr Franclin",
                LocalDate.of(2026, 10, 6),
                AppointmentStatus.SCHEDULED
        );

        assertThrows(
                AppointmentNotFoundException.class,
                () -> service.updateAppointment(999L, request)
        );
        verify(repository, never()).save(any(Appointment.class));
    }

    @Test
    void shouldReturnAllAppointments(){
        AppointmentRepository repository = mock(AppointmentRepository.class);

        AppointmentService service = new AppointmentService(repository);

        Appointment appointment = new Appointment(
                "Jean Dupont",
                "Dr Martin",
                LocalDate.of(2026, 10, 1),
                AppointmentStatus.SCHEDULED
        );

        Pageable pageable = PageRequest.of(0, 10);

        List<Appointment> appointments = List.of(appointment);

        Page<Appointment> appointmentPage = new PageImpl<>(appointments);

        when(repository.findAll(pageable))
                .thenReturn(appointmentPage);

        Page<AppointmentResponse> response = service.getAppointments(null, null, pageable);

        assertEquals(1, response.getTotalElements());

        AppointmentResponse res = response.getContent().get(0);

        assertEquals(
                "Jean Dupont",
                res.getPatientName()
        );

        assertEquals(
                "Dr Martin",
                res.getDoctorName()
        );

        assertEquals(
                LocalDate.of(2026, 10, 1),
                res.getAppointmentDate()
        );

        assertEquals(
                AppointmentStatus.SCHEDULED,
                res.getStatus()
        );

        verify(repository).findAll(pageable);
    }

    @Test
    void shouldReturnAppointmentsByDoctor(){
        AppointmentRepository repository = mock(AppointmentRepository.class);

        AppointmentService service = new AppointmentService(repository);

        Appointment appointment = new Appointment(
                "Jean Dupont",
                "Dr Martin",
                LocalDate.of(2026, 10, 1),
                AppointmentStatus.SCHEDULED
        );

        Pageable pageable = PageRequest.of(0, 10);

        List<Appointment> appointments = List.of(appointment);

        Page<Appointment> appointmentPage = new PageImpl<>(appointments);

        when(repository.findByDoctorName("Dr Martin", pageable))
                .thenReturn(appointmentPage);

        Page<AppointmentResponse> response = service.getAppointments("Dr Martin", null, pageable);

        assertEquals(1, response.getTotalElements());

        AppointmentResponse res = response.getContent().get(0);

        assertEquals(
                "Jean Dupont",
                res.getPatientName()
        );
        assertEquals(
                "Dr Martin",
                res.getDoctorName()
        );

        assertEquals(
                LocalDate.of(2026, 10, 1),
                res.getAppointmentDate()
        );

        assertEquals(
                AppointmentStatus.SCHEDULED,
                res.getStatus()
        );

        verify(repository).findByDoctorName("Dr Martin", pageable);

    }

    @Test
    void shouldReturnAppointmentsByStatus(){
        AppointmentRepository repository = mock(AppointmentRepository.class);

        AppointmentService service = new AppointmentService(repository);

        Appointment appointment = new Appointment(
                "Jean Dupont",
                "Dr Martin",
                LocalDate.of(2026, 10, 1),
                AppointmentStatus.COMPLETED
        );

        Pageable pageable = PageRequest.of(0, 10);

        List<Appointment> appointments = List.of(appointment);

        Page<Appointment> appointmentPage = new PageImpl<>(appointments);

        when(repository.findByStatus(AppointmentStatus.COMPLETED, pageable))
                .thenReturn(appointmentPage);

        Page<AppointmentResponse> response = service.getAppointments(null, AppointmentStatus.COMPLETED, pageable);

        assertEquals(1, response.getTotalElements());

        AppointmentResponse res = response.getContent().get(0);

        assertEquals(
                "Jean Dupont",
                res.getPatientName()
        );
        assertEquals(
                "Dr Martin",
                res.getDoctorName()
        );

        assertEquals(
                LocalDate.of(2026, 10, 1),
                res.getAppointmentDate()
        );

        assertEquals(
                AppointmentStatus.COMPLETED,
                res.getStatus()
        );

        verify(repository).findByStatus(AppointmentStatus.COMPLETED, pageable);
    }

    @Test
    void shouldReturnAppointmentsByDoctorAndStatus(){
        AppointmentRepository repository = mock(AppointmentRepository.class);

        AppointmentService service = new AppointmentService(repository);

        Appointment appointment = new Appointment(
                "Jean Dupont",
                "Dr Martin",
                LocalDate.of(2026, 10, 1),
                AppointmentStatus.COMPLETED
        );

        Pageable pageable = PageRequest.of(0, 10);

        List<Appointment> appointments = List.of(appointment);

        Page<Appointment> appointmentPage = new PageImpl<>(appointments);

        when(repository.findByDoctorNameAndStatus("Dr Martin", AppointmentStatus.COMPLETED, pageable))
                .thenReturn(appointmentPage);

        Page<AppointmentResponse> response = service.getAppointments("Dr Martin", AppointmentStatus.COMPLETED, pageable);

        assertEquals(1, response.getTotalElements());

        AppointmentResponse res = response.getContent().get(0);

        assertEquals(
                "Jean Dupont",
                res.getPatientName()
        );
        assertEquals(
                "Dr Martin",
                res.getDoctorName()
        );

        assertEquals(
                LocalDate.of(2026, 10, 1),
                res.getAppointmentDate()
        );

        assertEquals(
                AppointmentStatus.COMPLETED,
                res.getStatus()
        );
        verify(repository).findByDoctorNameAndStatus("Dr Martin", AppointmentStatus.COMPLETED, pageable);
    }

    @Test
    void shouldReturnPaginatedAppointments(){

        AppointmentRepository repository = mock(AppointmentRepository.class);

        AppointmentService service = new AppointmentService(repository);

        List<Appointment> appointments = new ArrayList<>();

        LocalDate startDate = LocalDate.of(2026, 10, 1);

        for (int i=1; i<=15; i++){
            Appointment appointment = new Appointment(
                    "Jean Dupont",
                    "Dr Martin",
                    startDate.plusDays(i -1),
                    AppointmentStatus.SCHEDULED
            );

            appointments.add(appointment);
        }

        Pageable pageable = PageRequest.of(0, 10);

        Page<Appointment> appointmentPage = new PageImpl<>(appointments.subList(0, 10), pageable, 15);

        when(repository.findAll(pageable))
                .thenReturn(appointmentPage);

        Page<AppointmentResponse> response =
                service.getAppointments(null, null, pageable);

        assertEquals(
                10,
                response.getNumberOfElements()
        );

        assertEquals(
                15,
                response.getTotalElements()
        );

        assertEquals(
                2,
                response.getTotalPages()
        );

        assertEquals(
                0,
                response.getPageable().getPageNumber()
        );

        assertEquals(
                10,
                response.getPageable().getPageSize()
        );

        verify(repository).findAll(pageable);
    } 
}