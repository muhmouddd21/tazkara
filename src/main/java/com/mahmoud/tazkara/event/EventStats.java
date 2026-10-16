package com.mahmoud.tazkara.event;

import java.io.Serializable;

public record EventStats(
    long totalEvents, Long totalSeats
) implements Serializable{
    
}
