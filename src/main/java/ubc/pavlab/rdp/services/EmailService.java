package ubc.pavlab.rdp.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import ubc.pavlab.rdp.model.PasswordResetToken;
import ubc.pavlab.rdp.model.User;
import ubc.pavlab.rdp.settings.SiteSettings;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import javax.servlet.http.HttpServletRequest;
import java.util.Locale;

/**
 * Created by mjacobson on 19/01/18.
 */
@Service
public class EmailService {

    @Autowired
    private SiteSettings siteSettings;

    @Autowired
    private JavaMailSender emailSender;

    @Autowired
    MessageSource messageSource;

    private void sendSimpleMessage( String subject, String content, String to ) {

        SimpleMailMessage email = new SimpleMailMessage();

        email.setSubject( subject );
        email.setText( content );
        email.setTo( to );
        email.setFrom( siteSettings.getAdminEmail() );

        emailSender.send( email );

    }

    private void sendMessage( String subject, String content, String to, MultipartFile attachment ) throws MessagingException {

        if ( attachment == null ) {
            sendSimpleMessage( subject, content, to );
        } else {

            MimeMessage message = emailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper( message, true );

            helper.setSubject( subject );
            helper.setText( content );
            helper.setTo( to );
            helper.setFrom( siteSettings.getAdminEmail() );

            helper.addAttachment( attachment.getOriginalFilename(), attachment );

            emailSender.send( message );
        }

    }

    public void sendSupportMessage( String message, String name, User user, HttpServletRequest request,
                                    MultipartFile attachment ) throws MessagingException {
        String content =
                "Name: " + name + "\r\n" +
                        "Email: " + user.getEmail() + "\r\n" +
                        "User-Agent: " + request.getHeader( "User-Agent" ) + "\r\n" +
                        "Message: " + message + "\r\n" +
                        "File Attached: " + String.valueOf( attachment != null && !attachment.getOriginalFilename().equals( "" ) );

        sendMessage( "Registry Help - Contact Support", content, siteSettings.getAdminEmail(), attachment );
    }

    public void sendResetTokenMessage( String token, User user ) throws MessagingException {
        String url = siteSettings.getFullUrl() + "updatePassword?id=" + user.getId() + "&token=" + token;

        String content =
                "Hello " + user.getProfile().getName() + ",\r\n\r\n" +
                        "We recently received a request that you want to reset your password. " +
                        "In order to reset your password, please click the confirmation link below:\r\n\r\n" +
                        url + "\r\n\r\n" +
                        "If you did not initiate this request, please disregard and delete this e-mail. " +
                        "Please note that this link will expire in " + PasswordResetToken.EXPIRATION + " hours.";


        sendMessage( "Reset Password", content, user.getEmail(), null );
    }

    public void sendRegistrationMessage( User user, String token ) {
        String registrationWelcome = messageSource.getMessage( "rdp.site.email.registration-welcome", new String[] {siteSettings.getFullUrl()}, Locale.getDefault() );
        String registrationEnding = messageSource.getMessage( "rdp.site.email.registration-ending", new String[] {siteSettings.getContactEmail()}, Locale.getDefault() );
        String recipientAddress = user.getEmail();
        String subject = "Registration Confirmation";
        String confirmationUrl = siteSettings.getFullUrl() + "registrationConfirm?token=" + token;
        String message = registrationWelcome +
                "\r\n\r\nPlease confirm your registration by clicking on the following link:\r\n\r\n" +
                confirmationUrl +
                "\r\n\r\n" +
                registrationEnding;
        sendSimpleMessage( subject, message, recipientAddress );
    }

    public void sendUserRegisteredEmail( User user ) {
        sendSimpleMessage( messageSource.getMessage( "rdp.site.shortname", null, Locale.getDefault() ) + " - User Registered", "New user registration: " + user.getEmail(), siteSettings.getAdminEmail() );
    }

}
