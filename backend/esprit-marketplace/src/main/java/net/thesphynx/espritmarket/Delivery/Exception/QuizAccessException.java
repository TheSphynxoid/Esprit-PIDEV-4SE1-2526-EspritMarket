package net.thesphynx.espritmarket.Delivery.Exception;

import org.springframework.http.HttpStatus;

public class QuizAccessException extends RuntimeException {

    private final HttpStatus status;

    public QuizAccessException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}


//Cette classe définit une exception personnalisée permettant de gérer
// les erreurs d’accès au quiz en associant un message explicite et un code HTTP spécifique.