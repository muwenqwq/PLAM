package com.learnagent.auth.dto;

import lombok.Data;

@Data
public class LoginResponse {

    private Long studentId;
    private String name;
    private String studentNo;
    private String major;
    private String grade;
    private String token;

    public LoginResponse(Long studentId, String name, String studentNo,
                         String major, String grade, String token) {
        this.studentId = studentId;
        this.name = name;
        this.studentNo = studentNo;
        this.major = major;
        this.grade = grade;
        this.token = token;
    }
}
