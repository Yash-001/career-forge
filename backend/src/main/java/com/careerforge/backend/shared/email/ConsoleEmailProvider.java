package com.careerforge.backend.shared.email;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ConsoleEmailProvider implements EmailService {

    @Override
    public void sendPasswordResetEmail(String toEmail, String resetLink) {
        log.info("""
                ╔══════════════════════════════════════════════════════╗
                ║          [DEV] PASSWORD RESET EMAIL                  ║
                ╠══════════════════════════════════════════════════════╣
                ║  To:   {}
                ║  Link: {}
                ╚══════════════════════════════════════════════════════╝
                """, toEmail, resetLink);
    }
}
