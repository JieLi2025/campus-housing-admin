package com.campus.dorm.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class RoommateInviteVO {
    private Long id;
    private Long senderId;
    private String senderName;
    private String senderNumber;
    private Long receiverId;
    private String receiverName;
    private String receiverNumber;
    private String roomNumber;
    private String buildingName;
    private String status;
    private String statusLabel;
    private Integer matchScore;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
}

