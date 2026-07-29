package com.transport.ticketservice.util;

import org.springframework.stereotype.Component;
import java.util.UUID;


@Component
public class TicketNumberGenerator {
    public String generate() {
        return "TKT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}