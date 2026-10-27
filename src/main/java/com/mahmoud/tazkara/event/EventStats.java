package com.mahmoud.tazkara.event;

import java.io.Serializable;

public record EventStats(
    Long totalEvents, Long totalSeats,Long cancellations
) implements Serializable{
    
}
