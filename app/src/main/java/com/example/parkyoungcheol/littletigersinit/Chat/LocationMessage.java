package com.example.parkyoungcheol.littletigersinit.Chat;

import lombok.Data;

@Data
public class LocationMessage extends Message {
    private String locationText;
    private String longitude;
    private String latitude;
}
