package com.healthapp.backend.model;

public class AppointmentBooking {
    private String doctorName;
    private String bookingDate; // e.g. "2026-06-19"
    private String timeSlot; // e.g. "11:30 AM"

    public AppointmentBooking() {}

    public AppointmentBooking(String doctorName, String bookingDate, String timeSlot) {
        this.doctorName = doctorName;
        this.bookingDate = bookingDate;
        this.timeSlot = timeSlot;
    }

    public String getDoctorName() {
        return doctorName;
    }

    public void setDoctorName(String doctorName) {
        this.doctorName = doctorName;
    }

    public String getBookingDate() {
        return bookingDate;
    }

    public void setBookingDate(String bookingDate) {
        this.bookingDate = bookingDate;
    }

    public String getTimeSlot() {
        return timeSlot;
    }

    public void setTimeSlot(String timeSlot) {
        this.timeSlot = timeSlot;
    }
}
