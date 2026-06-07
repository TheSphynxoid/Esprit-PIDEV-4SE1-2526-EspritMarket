package net.thesphynx.espritmarket.Delivery.Controller;

import lombok.RequiredArgsConstructor;
import net.thesphynx.espritmarket.Delivery.Dto.MessageDTO;
import net.thesphynx.espritmarket.Delivery.Entity.Message;
import net.thesphynx.espritmarket.Delivery.Service.MessageService;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/messages")
public class MessageController {

    private final MessageService messageService;
    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/chat.send")
    public void sendMessage(@Payload MessageDTO dto, Principal principal) {
        Message saved = messageService.save(dto, principal.getName());
        // Envoie au destinataire spécifique
        messagingTemplate.convertAndSendToUser(
                dto.getReceiverId(), "/queue/messages", saved
        );
    }

    @GetMapping("/{receiverId}")
    public ResponseEntity<List<Message>> getHistory(
            @PathVariable String receiverId, Principal principal
    ) {
        return ResponseEntity.ok(
                messageService.getConversation(principal.getName(), receiverId)
        );
    }
}