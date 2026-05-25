package com.mpsupport.knowledge.controller;

import com.mpsupport.knowledge.domain.ChunkSource;
import com.mpsupport.knowledge.dto.TicketChunkResponse;
import com.mpsupport.knowledge.service.TicketChunkService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/tickets")
public class TicketChunkController {

    private final TicketChunkService ticketChunkService;

    public TicketChunkController(TicketChunkService ticketChunkService) {
        this.ticketChunkService = ticketChunkService;
    }

    /**
     * Texto completo de um chunk do chamado (ex.: SOLUCAO ao clicar em Expandir no front).
     */
    @GetMapping("/{ticketId}/chunks/{source}")
    public TicketChunkResponse getChunk(
            @PathVariable String ticketId,
            @PathVariable ChunkSource source
    ) {
        return ticketChunkService.getChunk(ticketId, source);
    }
}
