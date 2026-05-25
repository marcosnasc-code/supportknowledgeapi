package com.mpsupport.knowledge.service;

import com.mpsupport.knowledge.domain.ChunkSource;
import com.mpsupport.knowledge.dto.TicketChunkResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.NoSuchElementException;
import java.util.UUID;

@Service
public class TicketChunkService {

    private final JdbcTemplate jdbcTemplate;

    public TicketChunkService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public TicketChunkResponse getChunk(String ticketId, ChunkSource source) {
        var rows = jdbcTemplate.query(
                """
                        SELECT id, ticket_id, source, content
                        FROM knowledge_chunk
                        WHERE ticket_id = ? AND source = ?
                        """,
                (rs, rowNum) -> new TicketChunkResponse(
                        (UUID) rs.getObject("id"),
                        rs.getString("ticket_id"),
                        ChunkSource.valueOf(rs.getString("source")),
                        rs.getString("content")
                ),
                ticketId,
                source.name()
        );
        if (rows.isEmpty()) {
            throw new NoSuchElementException(
                    "Chunk não encontrado para ticketId=" + ticketId + " source=" + source
            );
        }
        return rows.getFirst();
    }
}
