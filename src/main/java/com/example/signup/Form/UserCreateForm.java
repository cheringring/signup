package com.example.signup.Form;

import com.example.signup.entity.enum_.Gender;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UserCreateForm {

    @NotEmpty(message = "사용자 ID는 필수입니다.")
    @Size(min = 3, max = 20, message = "사용자 ID는 3-20자 사이여야 합니다.")
    private String userId;


    @NotEmpty(message = "사용자 이름은 필수 항목입니다.")
    private String userName;

    @NotEmpty(message = "비밀번호는 필수 항목입니다.")
    @Size(min = 8, message = "비밀번호는 최소 8자 이상이어야 합니다.")
    private String password;

    @NotEmpty(message = "비밀번호 확인은 필수항목입니다.")
    private String confirmPassword;

    @NotEmpty(message = "성별은 필수 항목입니다.")
    private String gender;

    @NotEmpty(message = "이메일 형식이 올바르지 않습니다.")
    @Email
    private String email;

    @NotEmpty(message = "주소는 필수 항목입니다.")
    private String addr;

    @NotEmpty(message = "직업은 필수 항목입니다.")
    private String occupation;

    @NotEmpty(message = "취미는 필수 항목입니다.")
    private String interest;

}