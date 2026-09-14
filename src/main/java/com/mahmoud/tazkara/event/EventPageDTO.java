package com.mahmoud.tazkara.event;

import java.util.List;

public record EventPageDTO(List<Event> items, int page, int size, int totalItems) {}