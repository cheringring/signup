package com.example.signup.Form;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UserCreateForm {

    @NotEmpty(message = "아이디는 필수입니다.")
    @Size(min = 3, max = 20, message = "아이디는 3-20자 사이여야 합니다.")
    @Pattern(regexp = "^[A-Za-z0-9]+$", message = "아이디는 영문, 숫자만 사용할 수 있습니다.")
    private String userId;

    @NotEmpty(message = "이름은 필수 항목입니다.")
    @Pattern(regexp = "^[A-Za-z\\u3131-\\u318E\\uAC00-\\uD7A3]+$", message = "이름은 한글, 영문만 사용할 수 있습니다.")
    private String userName;

    @NotEmpty(message = "닉네임은 필수 항목입니다.")
    @Size(min = 2, max = 20, message = "닉네임은 2-20자 사이여야 합니다.")
    @Pattern(regexp = "^[A-Za-z0-9\\u3131-\\u318E\\uAC00-\\uD7A3]+$", message = "닉네임은 한글, 영문, 숫자만 사용할 수 있습니다.")
    private String nickname;

    @NotEmpty(message = "비밀번호는 필수 항목입니다.")
    @Size(min = 8, message = "비밀번호는 최소 8자 이상이어야 합니다.")
    private String password;

    @NotEmpty(message = "비밀번호 확인은 필수항목입니다.")
    private String confirmPassword;

    @NotEmpty(message = "이메일은 필수 항목입니다.")
    @Email(message = "올바른 이메일 형식이 아닙니다.")
    private String email;

    @NotEmpty(message = "이메일 확인은 필수 항목입니다.")
    @Email(message = "올바른 이메일 형식이 아닙니다.")
    private String confirmEmail;

    @NotEmpty(message = "성별은 필수 항목입니다.")
    private String gender;

    @NotEmpty(message = "도/시는 필수 항목입니다.")
    private String province;

    @NotEmpty(message = "시/군/구는 필수 항목입니다.")
    private String city;
}