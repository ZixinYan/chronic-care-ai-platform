package com.zixin.thirdpartyprovider.service;

import com.zixin.thirdpartyapi.api.EmailAPI;
import com.zixin.thirdpartyapi.dto.SendAttachEmailRequest;
import com.zixin.thirdpartyapi.dto.SendEmailRequest;
import com.zixin.thirdpartyapi.dto.SendEmailResponse;
import com.zixin.utils.exception.ToBCodeEnum;
import jakarta.activation.DataHandler;
import jakarta.activation.DataSource;
import jakarta.activation.FileDataSource;
import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;

import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Properties;

@Slf4j
@DubboService
public class EmailServiceImpl implements EmailAPI {
    @Value("${spring.mail.from}")
    private String from; // 收件人
    @Value("${spring.mail.host}")
    private String host; // 邮件服务器配置
    @Value("${spring.mail.password}")
    private String password; // 发件人密码

    @Override
    public SendEmailResponse sendMail(SendEmailRequest sendEmailRequest) {
        Session session = getSession();
        SendEmailResponse sendEmailResponse = new SendEmailResponse();
        try{
            Message msg = new MimeMessage(session);
            msg.setFrom(new InternetAddress(from));
            msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(sendEmailRequest.getTo()));
            msg.setSubject(sendEmailRequest.getTheme());
            msg.setText(sendEmailRequest.getContent());
            log.info("send mail:{}",msg);
            Transport.send(msg);
            log.info("mail send successfully");
        }catch (MessagingException e){
            log.info("fail to send mail:{}",e.getMessage());
            sendEmailResponse.setCode(ToBCodeEnum.FAIL);
            sendEmailResponse.setMessage(e.getMessage());
            return sendEmailResponse;
        }
        sendEmailResponse.setCode(ToBCodeEnum.SUCCESS);
        return sendEmailResponse;
    }

    @Override
    public SendEmailResponse sendHTMLMail(SendEmailRequest sendEmailRequest) {
        Session session = getSession();
        SendEmailResponse sendEmailResponse = new SendEmailResponse();
        try {
            Message msg = new MimeMessage(session);
            msg.setFrom(new InternetAddress(from));
            msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(sendEmailRequest.getTo()));
            msg.setSubject(sendEmailRequest.getTheme());
            msg.setContent(sendEmailRequest.getContent(), "text/html;charset=utf-8");
            log.info("send html mail:{}",msg);
            Transport.send(msg);
            log.info("html mail send successfully");
        } catch (MessagingException e) {
            log.info("fail to send html mail:{}",e.getMessage());
            sendEmailResponse.setCode(ToBCodeEnum.FAIL);
            sendEmailResponse.setMessage(e.getMessage());
            return sendEmailResponse;
        }
        log.info("html mail send successfully");
        sendEmailResponse.setCode(ToBCodeEnum.SUCCESS);
        return sendEmailResponse;
    }

    @Override
    public SendEmailResponse sendAttachmentEmail(SendAttachEmailRequest sendAttachEmailRequest) {
        Session session = getSession();
        SendEmailResponse sendEmailResponse = new SendEmailResponse();
        try {
            Message msg = new MimeMessage(session);
            msg.setFrom(new InternetAddress(from));
            msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(sendAttachEmailRequest.getTo()));
            msg.setSubject(sendAttachEmailRequest.getTheme());
            // 1. 创建多部分
            Multipart multipart = new MimeMultipart();
            BodyPart messageBodyPart = new MimeBodyPart();
            multipart.addBodyPart(messageBodyPart);
            // 2. 创建消息部分
            messageBodyPart.setText(sendAttachEmailRequest.getContent());
            // 3. 添加附件
            DataSource source = new FileDataSource(sendAttachEmailRequest.getFilePath());
            messageBodyPart.setDataHandler(new DataHandler(source));
            messageBodyPart.setFileName(sendAttachEmailRequest.getFileName());
            // 4. 合并完整消息
            multipart.addBodyPart(messageBodyPart);
            msg.setContent(multipart);
            log.info("send attach mail:{}",msg);
            Transport.send(msg);
            log.info("attach mail send successfully");
        } catch (MessagingException e) {
            log.info("fail to send attach mail:{}",e.getMessage());
            sendEmailResponse.setCode(ToBCodeEnum.FAIL);
            sendEmailResponse.setMessage(e.getMessage());
            return sendEmailResponse;
        }
        log.info("attach mail send successfully");
        sendEmailResponse.setCode(ToBCodeEnum.SUCCESS);
        return sendEmailResponse;
    }


    public Session getSession() {
        System.setProperty("javax.net.debug", "ssl,handshake");
        SendEmailResponse sendEmailResponse = new SendEmailResponse();

        Properties props = new Properties();
        props.put("mail.smtp.host", host);
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.port", "587");
        props.put("mail.smtp.starttls.enable", "true");
        return Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(from, password);
            }
        });
    }

}
