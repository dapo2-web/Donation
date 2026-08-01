package com.hopeycharlie.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.math.BigDecimal;

/**
 * EmailService — handles all email sending using Spring Mail.
 *
 * @Service  = Spring manages this as a service bean
 * @Async    = Sends emails in a background thread so the API
 *             responds instantly without waiting for email to send
 * @Slf4j    = Gives us a log.info() / log.error() logger
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    // Spring auto-injects JavaMailSender configured from application.properties
    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${app.donation.email}")
    private String adminEmail;

    /**
     * Sent to donor right after they submit a Bitcoin donation.
     * Tells them payment is being tracked.
     */
    @Async
    public void sendBitcoinPendingEmail(String toEmail, String donorName,
                                        BigDecimal amount, String donationId) {
        String subject = "⏳ Your Bitcoin Donation is Being Processed";
        String html = """
            <div style="font-family:Georgia,serif;max-width:560px;margin:0 auto;padding:40px 20px;">
              <h1 style="color:#0A1F5C;">Thank You, %s ♥</h1>
              <p style="color:#555;line-height:1.7;">
                We've received your Bitcoin donation intent of <strong>$%s</strong>
                to <strong>Hopey & Charlie's Hearts</strong>.
              </p>
              <div style="background:#f8f9ff;border-left:4px solid #C8102E;padding:16px 20px;
                          border-radius:0 8px 8px 0;margin:24px 0;">
                <p style="margin:0;color:#333;">
                  <strong>Donation ID:</strong> %s<br/>
                  <strong>Status:</strong> ⏳ Pending Confirmation<br/>
                  <strong>Wallet:</strong> bc1qv34aurx4tx9zr4m5sr8dhrf6wa4ktmv0qpq697
                </p>
              </div>
              <p style="color:#555;line-height:1.7;">
                Once your Bitcoin transaction confirms on the blockchain, our team will
                verify it and you'll receive a final confirmation email.
                This typically takes <strong>10–60 minutes</strong>.
              </p>
              <p style="color:#888;font-size:0.85rem;margin-top:32px;">
                With love — Hopey & Charlie's Hearts Foundation ♥
              </p>
            </div>
            """.formatted(donorName != null ? donorName : "Friend", amount, donationId);

        sendEmail(toEmail, subject, html);
        notifyAdminNewDonation(donorName, toEmail, amount, donationId, "Bitcoin");
    }

    /**
     * Sent to donor after admin clicks "Confirm" on their Bitcoin donation.
     */
    @Async
    public void sendBitcoinConfirmedEmail(String toEmail, String donorName,
                                          BigDecimal amount, String donationId) {
        String subject = "✅ Your Bitcoin Donation Has Been Confirmed!";
        String html = """
            <div style="font-family:Georgia,serif;max-width:560px;margin:0 auto;padding:40px 20px;">
              <h1 style="color:#0A1F5C;">Donation Confirmed ✅</h1>
              <p style="color:#555;line-height:1.7;">Dear %s,</p>
              <p style="color:#555;line-height:1.7;">
                Your Bitcoin donation of <strong>$%s</strong> to
                <strong>Hopey & Charlie's Hearts</strong> has been verified and confirmed!
              </p>
              <div style="background:#f0fff4;border-left:4px solid #28a745;
                          padding:16px 20px;border-radius:0 8px 8px 0;margin:24px 0;">
                <p style="margin:0;color:#333;">
                  <strong>Donation ID:</strong> %s<br/>
                  <strong>Amount:</strong> $%s<br/>
                  <strong>Status:</strong> ✅ Confirmed
                </p>
              </div>
              <p style="color:#555;line-height:1.7;">
                Thank you from the bottom of our hearts. Your generosity is making a real
                difference for families living with Down syndrome — and for people like
                Hopey and Charlie.
              </p>
              <p style="color:#888;font-size:0.85rem;margin-top:32px;">
                With love — Hopey & Charlie's Hearts Foundation ♥
              </p>
            </div>
            """.formatted(
                donorName != null ? donorName : "Friend",
                amount, donationId, amount
        );

        sendEmail(toEmail, subject, html);
    }

    /**
     * Sent to donor when admin rejects their donation.
     */
    @Async
    public void sendRejectionEmail(String toEmail, String donorName,
                                   BigDecimal amount, String donationId, String reason) {
        String subject = "⚠️ Issue With Your Donation";
        String reasonHtml = (reason != null && !reason.isBlank())
                ? "<p style='color:#555;'><strong>Reason:</strong> " + reason + "</p>"
                : "";
        String html = """
            <div style="font-family:Georgia,serif;max-width:560px;margin:0 auto;padding:40px 20px;">
              <h2 style="color:#C8102E;">Donation Could Not Be Verified</h2>
              <p style="color:#555;line-height:1.7;">Dear %s,</p>
              <p style="color:#555;line-height:1.7;">
                We were unable to verify your donation of $%s (ID: %s).
              </p>
              %s
              <p style="color:#555;line-height:1.7;">
                Please reach out to us at lydiahollis@gmail.com and we'll help resolve this.
              </p>
            </div>
            """.formatted(donorName != null ? donorName : "Friend", amount, donationId, reasonHtml);

        sendEmail(toEmail, subject, html);
    }

    /**
     * Sends Apple Card photo to admin email as an attachment.
     */
    @Async
    public void sendAppleCardToAdmin(String donorName, String donorEmail,
                                     BigDecimal amount, String donationId,
                                     byte[] imageBytes, String fileName) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            // true = multipart (needed for attachments)
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            helper.setFrom(fromEmail);
            helper.setTo(adminEmail);
            helper.setSubject("🍎 Apple Card Donation — $" + amount + " from " + donorEmail);
            helper.setText("""
                <div style="font-family:Georgia,serif;max-width:560px;margin:0 auto;padding:40px 20px;">
                  <h2 style="color:#0A1F5C;">New Apple Card Donation</h2>
                  <p><strong>Donor:</strong> %s<br/>
                  <strong>Email:</strong> %s<br/>
                  <strong>Amount:</strong> $%s<br/>
                  <strong>Donation ID:</strong> %s</p>
                  <p>Card photo is attached.</p>
                </div>
                """.formatted(donorName, donorEmail, amount, donationId), true);

            // Attach the card image
            helper.addAttachment(
                    fileName != null ? fileName : "apple-card.jpg",
                    new org.springframework.core.io.ByteArrayResource(imageBytes)
            );

            mailSender.send(message);
            log.info("Apple card photo sent to admin for donation {}", donationId);

        } catch (MessagingException e) {
            log.error("Failed to send Apple Card email for donation {}: {}", donationId, e.getMessage());
        }
    }

    /**
     * Confirmation email to donor after Apple Card submission.
     */
    @Async
    public void sendAppleCardConfirmation(String toEmail, String donorName,
                                          BigDecimal amount, String donationId) {
        String subject = "🍎 Apple Card Donation Received — Thank You!";
        String html = """
            <div style="font-family:Georgia,serif;max-width:560px;margin:0 auto;padding:40px 20px;">
              <h1 style="color:#0A1F5C;">Thank You, %s ♥</h1>
              <p style="color:#555;line-height:1.7;">
                We've received your Apple Card donation of <strong>$%s</strong>.
                Our team will process it within <strong>24 hours</strong>
                and send you a final confirmation.
              </p>
              <p style="color:#555;"><strong>Donation ID:</strong> %s</p>
              <p style="color:#888;font-size:0.85rem;margin-top:32px;">
                With love — Hopey & Charlie's Hearts Foundation ♥
              </p>
            </div>
            """.formatted(donorName != null ? donorName : "Friend", amount, donationId);

        sendEmail(toEmail, subject, html);
    }

    /**
     * Notifies admin of a new donation (called after Bitcoin submission).
     */
    private void notifyAdminNewDonation(String donorName, String donorEmail,
                                        BigDecimal amount, String donationId, String method) {
        String subject = "🔔 New " + method + " Donation — $" + amount + " from " + donorEmail;
        String html = """
            <div style="font-family:Georgia,serif;max-width:560px;margin:0 auto;padding:40px 20px;">
              <h2 style="color:#0A1F5C;">New %s Donation Pending</h2>
              <p><strong>Donor:</strong> %s<br/>
              <strong>Email:</strong> %s<br/>
              <strong>Amount:</strong> $%s<br/>
              <strong>Donation ID:</strong> %s</p>
              <p>Please verify and confirm from your Admin Dashboard.</p>
            </div>
            """.formatted(method, donorName, donorEmail, amount, donationId);

        sendEmail(adminEmail, subject, html);
    }

    /**
     * Core send method — wraps JavaMailSender.
     */
    private void sendEmail(String to, String subject, String htmlBody) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom("\"Hopey & Charlie's Hearts\" <" + fromEmail + ">");
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true); // true = HTML
            mailSender.send(message);
            log.info("Email sent to {}: {}", to, subject);
        } catch (MessagingException e) {
            log.error("Failed to send email to {}: {}", to, e.getMessage());
        }
    }
}
