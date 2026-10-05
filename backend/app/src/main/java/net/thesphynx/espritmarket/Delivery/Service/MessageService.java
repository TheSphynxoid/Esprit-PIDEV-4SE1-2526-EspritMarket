package net.thesphynx.espritmarket.Delivery.Service;

import lombok.RequiredArgsConstructor;
import net.thesphynx.espritmarket.Delivery.Dto.MessageDTO;
import net.thesphynx.espritmarket.Delivery.Entity.Message;
import net.thesphynx.espritmarket.Delivery.Repository.MessageRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MessageService {

    private final MessageRepository messageRepository;

    @Transactional
    public Message save(MessageDTO dto, String senderId) {
        Message message = new Message();
        message.setSenderId(senderId);
        message.setReceiverId(dto.getReceiverId());
        message.setContent(dto.getContent());
        message.setSentAt(LocalDateTime.now());
        message.setRead(false);
        return messageRepository.save(message);
    }

    @Transactional(readOnly = true)
    public List<Message> getConversation(String user1, String user2) {
        return messageRepository.findConversation(user1, user2);
    }
}
