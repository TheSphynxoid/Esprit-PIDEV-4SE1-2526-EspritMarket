package net.thesphynx.espritmarket.Srv.Dto;

import lombok.Data;

import java.util.List;

@Data
public class BookingMessageBatchResponse {
    private List<BookingMessageResponse> items;
    private Long nextCursor;
    private boolean hasMore;
}
