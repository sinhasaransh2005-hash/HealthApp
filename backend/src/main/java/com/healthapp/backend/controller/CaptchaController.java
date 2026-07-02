package com.healthapp.backend.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Random;

@Controller
public class CaptchaController {

    @GetMapping("/captcha")
    public void getCaptcha(HttpServletRequest request, HttpServletResponse response) throws IOException {
        // Set response headers to prevent caching
        response.setContentType("image/png");
        response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");
        response.setHeader("Pragma", "no-cache");
        response.setDateHeader("Expires", 0);

        String captchaText = generateRandomText(5);
        request.getSession().setAttribute("captcha", captchaText);
        System.out.println("[CAPTCHA SERVICE] Generated Captcha: " + captchaText);

        int width = 140;
        int height = 48;
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = image.createGraphics();

        // Anti-aliased text rendering
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // Fill background with light gradient
        GradientPaint gp = new GradientPaint(0, 0, new Color(240, 242, 245), width, height, new Color(203, 213, 225));
        g2d.setPaint(gp);
        g2d.fillRect(0, 0, width, height);

        // Draw some random noise lines
        Random r = new Random();
        g2d.setColor(new Color(148, 163, 184, 150));
        for (int i = 0; i < 8; i++) {
            g2d.drawLine(r.nextInt(width), r.nextInt(height), r.nextInt(width), r.nextInt(height));
        }

        // Draw captcha text with slight rotation
        g2d.setColor(new Color(15, 118, 110)); // primary/teal color
        g2d.setFont(new Font("Courier New", Font.BOLD | Font.ITALIC, 26));

        // Draw characters with distinct skew
        for (int i = 0; i < captchaText.length(); i++) {
            char ch = captchaText.charAt(i);
            int x = 18 + (i * 22);
            int y = 30 + r.nextInt(8) - 4;
            g2d.drawString(String.valueOf(ch), x, y);
        }

        g2d.dispose();
        ImageIO.write(image, "png", response.getOutputStream());
    }

    private String generateRandomText(int length) {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789";
        StringBuilder sb = new StringBuilder();
        Random r = new Random();
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(r.nextInt(chars.length())));
        }
        return sb.toString();
    }

    @GetMapping("/captcha/value")
    @ResponseBody
    public String getCaptchaValue(HttpServletRequest request) {
        return (String) request.getSession().getAttribute("captcha");
    }
}
