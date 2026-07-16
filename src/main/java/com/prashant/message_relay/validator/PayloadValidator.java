package com.prashant.message_relay.validator;


import com.prashant.message_relay.model.NotificationEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Slf4j
@Component
public class PayloadValidator {

    private static final Pattern PHONE_PATTERN = Pattern.compile("^\\+?[1-9]\\d{6,14}$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[\\w._%+\\-]+@[\\w.\\-]+\\.[a-zA-Z]{2,}$");
    private static final int MAX_PARAMS_SIZE = 20;

    public ValidationResult validate(NotificationEvent event) {
        List<String> errors = new ArrayList<>();

        if (event == null) {
            return ValidationResult.fail(List.of("Event payload is null"));
        }

        if (isBlank(event.getEventId())) errors.add("eventId is required");
        if (isBlank(event.getClientId())) errors.add("clientId is required");
        if (isBlank(event.getRecipient())) errors.add("recipient is required");
        if (event.getChannel() == null) errors.add("channel is required");
        if (event.getPriority() == null) errors.add("priority is required");

        if (event.getRecipient() != null && event.getChannel() != null) {
            validateRecipientFormat(event.getRecipient(), event.getChannel(), errors);
        }

        if (event.getTemplateParams() != null && event.getTemplateParams().size() > MAX_PARAMS_SIZE) {
            errors.add("templateParams exceeds max size of " + MAX_PARAMS_SIZE);
        }

        if (!errors.isEmpty()) {
            log.warn("Validation failed for eventId={} errors={}", event.getEventId(), errors);
            return ValidationResult.fail(errors);
        }

        return ValidationResult.ok();
    }

    private void validateRecipientFormat(String recipient, NotificationEvent.Channel channel,
                                          List<String> errors) {
        switch (channel) {
            case SMS, WHATSAPP -> {
                if (!PHONE_PATTERN.matcher(recipient).matches()) {
                    errors.add("recipient must be a valid phone number for channel " + channel);
                }
            }
            case EMAIL -> {
                if (!EMAIL_PATTERN.matcher(recipient).matches()) {
                    errors.add("recipient must be a valid email for channel EMAIL");
                }
            }
        }
    }

    public String maskPii(String recipient, NotificationEvent.Channel channel) {
        if (recipient == null) return null;
        return switch (channel) {
            case SMS, WHATSAPP -> {
                // +911234567890 → +91****7890
                String digits = recipient.startsWith("+") ? recipient.substring(1) : recipient;
                if (digits.length() >= 7) {
                    int countryCodeLength = Math.min(3, Math.max(1, digits.length() - 10));
                    String countryCode = digits.substring(0, countryCodeLength);
                    String suffix = digits.substring(digits.length() - 4);
                    yield (recipient.startsWith("+") ? "+" : "") + countryCode + "****" + suffix;
                }
                yield "****";
            }
            case EMAIL -> {
                // user@domain.com → us**@domain.com
                int atIndex = recipient.indexOf('@');
                if (atIndex > 2) {
                    yield recipient.substring(0, 2) + "**" + recipient.substring(atIndex);
                }
                if (atIndex > 0) {
                    yield "**" + recipient.substring(atIndex);
                }
                yield "****";
            }
        };
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    public record ValidationResult(boolean valid, List<String> errors) {
        public static ValidationResult ok() { return new ValidationResult(true, List.of()); }
        public static ValidationResult fail(List<String> errors) { return new ValidationResult(false, errors); }
    }
}
