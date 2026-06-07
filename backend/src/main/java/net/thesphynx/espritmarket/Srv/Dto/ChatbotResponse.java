package net.thesphynx.espritmarket.Srv.Dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ChatbotResponse {
    private String message;
    private String intent;
    private List<String> suggestions;
}
