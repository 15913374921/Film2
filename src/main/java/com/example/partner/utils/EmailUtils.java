package com.example.partner.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Date;

@Component // @Component 是一个注解，它的作用是将类标记为一个Spring 容器中的组件，使得 Spring 可以自动发现、管理和注入这个类
@Slf4j
public class EmailUtils {

    @Autowired
    JavaMailSender javaMailSender;

    @Value("${spring.mail.username}")
    String username;

    public void sendHtml(String title, String html, String to) {
        // 设置 DNS 问题的属性
        System.getProperties().setProperty("mail.mime.address.usecanonicalhostname", "false");
        MimeMessage mailMessage = javaMailSender.createMimeMessage();
        Session session = mailMessage.getSession();
        // 设置 日志打印控制器
        session.setDebug(true);
        //  自动获取本地 IP 并设置
        try {
            InetAddress localHost = InetAddress.getLocalHost();
            String localIp = localHost.getHostAddress();
            session.getProperties().setProperty("mail.smtp.localhost", localIp);
            log.info("本地 IP 地址已设置为: {}", localIp);
        } catch (UnknownHostException e) {
            log.error("获取本地 IP 地址失败", e);
        }


        //需要借助Helper类
        MimeMessageHelper helper;
        try {
            helper = new MimeMessageHelper(mailMessage, true);
            helper.setFrom(username);  // 必填
            helper.setTo(to);   // 必填
//            helper.setBcc("密送人");   // 选填
            helper.setSubject(title);  // 必填
            helper.setSentDate(new Date());//发送时间
            helper.setText(html, true);   // 必填  第一个参数要发送的内容，第二个参数是不是Html格式。
            javaMailSender.send(mailMessage);
        } catch (MessagingException e) {
            log.error("发送邮件失败", e);
        }
    }
}
