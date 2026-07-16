package com.prashant.message_relay.validator;

import com.prashant.message_relay.model.NotificationEvent;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PayloadValidatorTest {

    private final PayloadValidator validator = new PayloadValidator();

    @Test
    void shouldMaskPhoneAndEmailAsPerPrdExamples() {
        String maskedPhone = validator.maskPii("+911234567890", NotificationEvent.Channel.SMS);
        String maskedEmail = validator.maskPii("prashant@example.com", NotificationEvent.Channel.EMAIL);

        assertEquals("+91****7890", maskedPhone);
        assertEquals("pr**@example.com", maskedEmail);
    }

    @Test
    void shouldRejectInvalidRecipientFormats() {
        NotificationEvent sms = NotificationEvent.builder()
                .eventId("evt-1")
                .clientId("client-1")
                .recipient("not-a-phone")
                .channel(NotificationEvent.Channel.SMS)
                .priority(NotificationEvent.Priority.HIGH)
                .build();

        NotificationEvent email = NotificationEvent.builder()
                .eventId("evt-2")
                .clientId("client-1")
                .recipient("not-an-email")
                .channel(NotificationEvent.Channel.EMAIL)
                .priority(NotificationEvent.Priority.HIGH)
                .build();

        PayloadValidator.ValidationResult smsResult = validator.validate(sms);
        PayloadValidator.ValidationResult emailResult = validator.validate(email);

        assertFalse(smsResult.valid());
        assertTrue(smsResult.errors().stream().anyMatch(e -> e.contains("valid phone number")));

        assertFalse(emailResult.valid());
        assertTrue(emailResult.errors().stream().anyMatch(e -> e.contains("valid email")));
    }
}

