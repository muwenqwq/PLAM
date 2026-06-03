package com.learnagent.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.learnagent.dto.request.LoginRequest;
import com.learnagent.dto.response.LoginResponse;
import com.learnagent.entity.Student;
import com.learnagent.exception.BizException;
import com.learnagent.exception.ErrorCode;
import com.learnagent.mapper.StudentMapper;
import com.learnagent.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final StudentMapper studentMapper;
    private final JwtUtil jwtUtil;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    /**
     * 登录：验证学号密码，返回 JWT Token
     */
    public LoginResponse login(LoginRequest req) {
        // 查询学生
        Student student = studentMapper.selectOne(
                new QueryWrapper<Student>()
                        .eq("student_no", req.getStudentNo())
                        .eq("status", "active"));

        if (student == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED, "学号或密码错误");
        }

        // 验证密码
        if (!passwordEncoder.matches(req.getPassword(), student.getPasswordHash())) {
            throw new BizException(ErrorCode.UNAUTHORIZED, "学号或密码错误");
        }

        // 生成 Token
        String token = jwtUtil.generateToken(student.getId(), student.getStudentNo());

        log.info("学生登录成功: id={}, studentNo={}", student.getId(), student.getStudentNo());

        return new LoginResponse(
                student.getId(),
                student.getName(),
                student.getStudentNo(),
                student.getMajor(),
                student.getGrade(),
                token
        );
    }

    /**
     * 注册：创建新学生
     */
    public LoginResponse register(String name, String studentNo, String password,
                                   String major, String grade) {
        // 检查学号是否已存在
        Long count = studentMapper.selectCount(
                new QueryWrapper<Student>().eq("student_no", studentNo));
        if (count > 0) {
            throw new BizException(ErrorCode.BAD_REQUEST, "学号已存在");
        }

        // 创建学生
        Student student = new Student();
        student.setName(name);
        student.setStudentNo(studentNo);
        student.setPasswordHash(passwordEncoder.encode(password));
        student.setMajor(major);
        student.setGrade(grade);
        student.setStatus("active");
        studentMapper.insert(student);

        // 生成 Token
        String token = jwtUtil.generateToken(student.getId(), student.getStudentNo());

        log.info("学生注册成功: id={}, studentNo={}", student.getId(), studentNo);

        return new LoginResponse(
                student.getId(),
                student.getName(),
                student.getStudentNo(),
                student.getMajor(),
                student.getGrade(),
                token
        );
    }
}
