package com.mahmoud.tazkara.event;

import java.util.List;

public record EventPageDTO(List<EventResponseDTO> items, int page, int size, Long totalItems) {}