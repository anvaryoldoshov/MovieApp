package com.example.movieapp.service;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileStorageService {

    private final RestTemplate restTemplate;

    @Value("${app.upload.dir}")
    private String uploadRootDir;

    public String saveImage(String type, MultipartFile file) {
        try {
            Path uploadPath = Paths.get(uploadRootDir, type);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            String filename = System.currentTimeMillis() + "_" + file.getOriginalFilename();
            Path filePath = uploadPath.resolve(filename);
            file.transferTo(filePath.toFile());

            return "/uploads/" + type + "/" + filename;
        } catch (IOException e) {
            throw new RuntimeException("Rasmni saqlab bo‘lmadi: " + e.getMessage(), e);
        }
    }

    /**
     * Tashqi manzildan (masalan Bunny Stream avtomatik yaratgan thumbnail) rasmni yuklab olib,
     * o'zimizning serverga saqlaydi. Muvaffaqiyatsiz bo'lsa null qaytaradi (chaqiruvchi tomon
     * buni ixtiyoriy - masalan admin qo'lda rasm yuklamagan bo'lsa - fallback sifatida ishlatadi).
     */
    public String saveImageFromUrl(String type, String sourceUrl) {
        try {
            Path uploadPath = Paths.get(uploadRootDir, type);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            byte[] bytes = restTemplate.getForObject(sourceUrl, byte[].class);
            if (bytes == null || bytes.length == 0) {
                log.warn("Tashqi manzildan rasm bo'sh qaytdi: {}", sourceUrl);
                return null;
            }

            String filename = System.currentTimeMillis() + "_thumbnail.jpg";
            Path filePath = uploadPath.resolve(filename);
            Files.write(filePath, bytes);

            return "/uploads/" + type + "/" + filename;
        } catch (Exception e) {
            log.warn("Tashqi manzildan rasm yuklab olinmadi ({}): {}", sourceUrl, e.getMessage());
            return null;
        }
    }

    /**
     * Rasmni belgilangan maksimal o'lcham va sifatgacha siqib JPEG sifatida saqlaydi
     * (masalan push-notification rasmlari - telefon tez yuklab olishi uchun yengil bo'lishi kerak).
     */
    public String saveCompressedImage(String type, MultipartFile file, int maxDimension, float quality) {
        try {
            Path uploadPath = Paths.get(uploadRootDir, type);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            BufferedImage original = ImageIO.read(file.getInputStream());
            if (original == null) {
                return saveImage(type, file);
            }

            BufferedImage resized = resizeIfNeeded(original, maxDimension);
            String filename = System.currentTimeMillis() + "_notif.jpg";
            Path filePath = uploadPath.resolve(filename);

            writeJpeg(resized, filePath.toFile(), quality);

            return "/uploads/" + type + "/" + filename;
        } catch (IOException e) {
            throw new RuntimeException("Rasmni siqib bo‘lmadi: " + e.getMessage(), e);
        }
    }

    private BufferedImage resizeIfNeeded(BufferedImage original, int maxDimension) {
        int width = original.getWidth();
        int height = original.getHeight();

        if (width <= maxDimension && height <= maxDimension) {
            return toRgb(original);
        }

        double scale = Math.min((double) maxDimension / width, (double) maxDimension / height);
        int newWidth = (int) Math.round(width * scale);
        int newHeight = (int) Math.round(height * scale);

        BufferedImage resized = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = resized.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.drawImage(original, 0, 0, newWidth, newHeight, null);
        g.dispose();
        return resized;
    }

    private BufferedImage toRgb(BufferedImage source) {
        if (source.getType() == BufferedImage.TYPE_INT_RGB) {
            return source;
        }
        BufferedImage rgb = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = rgb.createGraphics();
        g.drawImage(source, 0, 0, null);
        g.dispose();
        return rgb;
    }

    private void writeJpeg(BufferedImage image, File output, float quality) throws IOException {
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpg");
        if (!writers.hasNext()) {
            throw new IOException("JPEG writer topilmadi");
        }
        ImageWriter writer = writers.next();
        ImageWriteParam param = writer.getDefaultWriteParam();
        param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        param.setCompressionQuality(quality);

        try (ImageOutputStream ios = ImageIO.createImageOutputStream(output)) {
            writer.setOutput(ios);
            writer.write(null, new IIOImage(image, null, null), param);
        } finally {
            writer.dispose();
        }
    }
}
