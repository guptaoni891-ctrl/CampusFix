package com.campusfix.service;
import com.campusfix.model.Complaint;
@FunctionalInterface public interface TicketListener { void onTicketChanged(Complaint ticket, String action); }
