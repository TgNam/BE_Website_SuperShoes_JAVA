package org.example.datn_website_supershoes.dto.request;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.example.datn_website_supershoes.dto.customs.MinAge;

import java.util.Date;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AccountRequest {

    @NotBlank(message = "Tên là bắt buộc")
    @Size(min = 2, max = 50, message = "Tên phải chứa ít nhất 2 ký tự và không được vượt quá 50 ký tự")
    @Pattern(regexp = "^[A-Za-zÀ-ỹ\\s]+$", message = "Tên không được chứa ký tự đặc biệt hoặc số")
    private String name;

    @NotBlank(message = "Email là bắt buộc")
    @Email(message = "Email không hợp lệ")
    private String email;

    @NotBlank(message = "Số điện thoại là bắt buộc")
    @Pattern(regexp = "^0[0-9]{9,10}$", message = "Số điện thoại phải bắt đầu bằng số 0 và có từ 10 đến 11 số")
    private String phoneNumber;

    @NotBlank(message = "Vai trò là bắt buộc")
    private String role;

    private Integer gender;  // Có thể dùng giá trị 1: Nam, 2: Nữ, 3: Khác

    @MinAge(value = 18, message = "Bạn phải ít nhất 18 tuổi")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private Date birthday;

    @NotBlank(message = "Trạng thái tài khoản là bắt buộc")
    private String status;

}