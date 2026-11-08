package com.campus.dorm.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * 文件上传服务
 * 
 * @author Campus Dorm System
 * @date 2025-11-03
 */
@Slf4j
@Service
public class FileUploadService {
    
    /**
     * 允许的图片格式
     */
    private static final List<String> ALLOWED_IMAGE_TYPES = Arrays.asList(
        "image/jpeg", "image/jpg", "image/png", "image/gif", "image/webp"
    );
    
    /**
     * 允许的文件扩展名
     */
    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList(
        ".jpg", ".jpeg", ".png", ".gif", ".webp"
    );
    
    /**
     * 最大文件大小（2MB）
     */
    private static final long MAX_FILE_SIZE = 2 * 1024 * 1024;
    
    /**
     * 上传目录（相对于项目根目录）
     */
    @Value("${file.upload.path:src/main/resources/static/uploads/avatars/}")
    private String uploadPath;
    
    /**
     * 访问URL前缀
     */
    @Value("${file.upload.url-prefix:/uploads/avatars/}")
    private String urlPrefix;
    
    /**
     * 上传头像
     * 
     * @param file 上传的文件
     * @return 文件访问URL
     */
    public String uploadAvatar(MultipartFile file) {
        // 1. 验证文件是否为空
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("文件不能为空");
        }
        
        // 2. 验证文件大小
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new RuntimeException("文件大小不能超过2MB");
        }
        
        // 3. 验证文件类型
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_IMAGE_TYPES.contains(contentType.toLowerCase())) {
            throw new RuntimeException("只支持JPG、PNG、GIF、WEBP格式的图片");
        }
        
        // 4. 验证文件扩展名
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isEmpty()) {
            throw new RuntimeException("文件名不能为空");
        }
        
        String extension = getFileExtension(originalFilename);
        if (!ALLOWED_EXTENSIONS.contains(extension.toLowerCase())) {
            throw new RuntimeException("不支持的文件格式");
        }
        
        // 5. 生成唯一文件名
        String newFileName = UUID.randomUUID().toString() + extension;
        
        // 6. 确保上传目录存在
        File uploadDir = new File(uploadPath);
        if (!uploadDir.exists()) {
            boolean created = uploadDir.mkdirs();
            if (!created) {
                log.error("创建上传目录失败: {}", uploadPath);
                throw new RuntimeException("创建上传目录失败");
            }
        }
        
        // 7. 保存文件
        try {
            Path destinationPath = Paths.get(uploadPath, newFileName);
            Files.copy(file.getInputStream(), destinationPath);
            log.info("文件上传成功: {}", newFileName);
        } catch (IOException e) {
            log.error("文件保存失败", e);
            throw new RuntimeException("文件保存失败: " + e.getMessage());
        }
        
        // 8. 返回访问URL
        String fileUrl = urlPrefix + newFileName;
        log.info("生成文件访问URL: {}", fileUrl);
        return fileUrl;
    }
    
    /**
     * 上传聊天文件（图片或文件）
     * 
     * @param file 上传的文件
     * @return 文件信息（URL、文件名、大小）
     */
    public ChatFileUploadResult uploadChatFile(MultipartFile file) {
        // 1. 验证文件是否为空
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("文件不能为空");
        }
        
        // 2. 验证文件大小（10MB）
        long maxSize = 10 * 1024 * 1024;
        if (file.getSize() > maxSize) {
            throw new RuntimeException("文件大小不能超过10MB");
        }
        
        // 3. 获取文件扩展名
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isEmpty()) {
            throw new RuntimeException("文件名不能为空");
        }
        
        String extension = getFileExtension(originalFilename);
        String contentType = file.getContentType();
        
        // 4. 确定文件类型（图片或文件）
        boolean isImage = contentType != null && contentType.startsWith("image/");
        
        // 5. 生成唯一文件名
        String newFileName = UUID.randomUUID().toString() + extension;
        
        // 6. 确定上传目录（聊天文件）
        String chatUploadPath = "uploads/chat/";
        File uploadDir = new File(chatUploadPath);
        if (!uploadDir.exists()) {
            boolean created = uploadDir.mkdirs();
            if (!created) {
                log.error("创建聊天文件上传目录失败: {}", chatUploadPath);
                throw new RuntimeException("创建上传目录失败");
            }
        }
        
        // 7. 保存文件
        try {
            Path destinationPath = Paths.get(chatUploadPath, newFileName);
            Files.copy(file.getInputStream(), destinationPath);
            log.info("聊天文件上传成功: {}", newFileName);
        } catch (IOException e) {
            log.error("聊天文件保存失败", e);
            throw new RuntimeException("文件保存失败: " + e.getMessage());
        }
        
        // 8. 返回文件信息
        String fileUrl = "/uploads/chat/" + newFileName;
        ChatFileUploadResult result = new ChatFileUploadResult();
        result.setFileUrl(fileUrl);
        result.setFileName(originalFilename);
        result.setFileSize(file.getSize());
        result.setMessageType(isImage ? "image" : "file");
        
        return result;
    }
    
    /**
     * 获取文件扩展名
     */
    private String getFileExtension(String filename) {
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex == -1 || lastDotIndex == filename.length() - 1) {
            return "";
        }
        return filename.substring(lastDotIndex);
    }
    
    /**
     * 聊天文件上传结果
     */
    public static class ChatFileUploadResult {
        private String fileUrl;
        private String fileName;
        private Long fileSize;
        private String messageType;
        
        public String getFileUrl() {
            return fileUrl;
        }
        
        public void setFileUrl(String fileUrl) {
            this.fileUrl = fileUrl;
        }
        
        public String getFileName() {
            return fileName;
        }
        
        public void setFileName(String fileName) {
            this.fileName = fileName;
        }
        
        public Long getFileSize() {
            return fileSize;
        }
        
        public void setFileSize(Long fileSize) {
            this.fileSize = fileSize;
        }
        
        public String getMessageType() {
            return messageType;
        }
        
        public void setMessageType(String messageType) {
            this.messageType = messageType;
        }
    }
}

