package com.sebastien.appointmentmanagement.service;

import com.sebastien.appointmentmanagement.dto.AppointmentRequest;
import com.sebastien.appointmentmanagement.dto.AppointmentResponse;
import com.sebastien.appointmentmanagement.entity.Appointment;
import com.sebastien.appointmentmanagement.entity.AppointmentStatus;
import com.sebastien.appointmentmanagement.repository.AppointmentRepository;
import com.sebastien.appointmentmanagement.exception.AppointmentNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;


@Service
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;

    public AppointmentService(AppointmentRepository appointmentRepository) {
        this.appointmentRepository = appointmentRepository;
    }

    private AppointmentResponse toResponse(Appointment appointment){
        return new AppointmentResponse(
                appointment.getId(),
                appointment.getPatientName(),
                appointment.getDoctorName(),
                appointment.getAppointmentDate(),
                appointment.getStatus()
        );
    }


    public Page<AppointmentResponse> getAppointments(String doctorName, AppointmentStatus status, Pageable pageable) {
        Page<Appointment> appointments;

        if (doctorName ==null && status ==null){
           appointments = appointmentRepository.findAll(pageable);

        } else if (doctorName !=null && status == null) {
             appointments = appointmentRepository.findByDoctorName(doctorName, pageable);

        } else if (doctorName == null && status !=null) {
             appointments = appointmentRepository.findByStatus(status, pageable);
        }else{
            appointments = appointmentRepository.findByDoctorNameAndStatus(doctorName,status, pageable);
        }
        return  appointments.map(this::toResponse);
    }

    public AppointmentResponse createAppointment(AppointmentRequest appointmentRequest){
        Appointment appointment =  new Appointment(
                appointmentRequest.getPatientName(),
                appointmentRequest.getDoctorName(),
                appointmentRequest.getAppointmentDate(),
                appointmentRequest.getStatus()
        );
        Appointment savedAppointment = appointmentRepository.save(appointment);

        return toResponse(savedAppointment);
    }

    private Appointment findAppointmentById(Long id){
        return appointmentRepository.findById(id)
                .orElseThrow(()-> new AppointmentNotFoundException(id));
    }

    public AppointmentResponse getAppointmentById(Long id){
        Appointment appointment = findAppointmentById(id);

        return toResponse(appointment);
    }

    public void deleteAppointment(Long id){
        if (!appointmentRepository.existsById(id)){
            throw new AppointmentNotFoundException(id);
        }
        appointmentRepository.deleteById(id);
    }

    public AppointmentResponse updateAppointment(Long id, AppointmentRequest appointmentRequest){
        Appointment appointment = findAppointmentById(id);

        appointment.setPatientName(appointmentRequest.getPatientName());
        appointment.setDoctorName(appointmentRequest.getDoctorName());
        appointment.setAppointmentDate(appointmentRequest.getAppointmentDate());
        appointment.setStatus(appointmentRequest.getStatus());

        Appointment updatedAppointment = appointmentRepository.save(appointment);

        return toResponse(updatedAppointment);
    }

}
