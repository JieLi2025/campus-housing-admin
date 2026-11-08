package com.campus.dorm.dto;

import lombok.Data;
import javax.validation.constraints.NotNull;

@Data
public class RoommateInviteDTO {
    @NotNull(message = "接收者ID不能为空")
    private Long receiverId;
    private Long roomId;
}

