package org.example.datn_website_supershoes.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AccountUpdateRequest {
    @NotBlank(message = "Tên là bắt buộc")
    @Size(min = 2, max = 50, message = "Tên phải chứa ít nhất 2 ký tự không được vượt quá 50 ký tự")
    private String name;

    @NotBlank(message = "Số điện thoại là bắt buộc")
    @Pattern(regexp = "^0[0-9]{9,10}$", message = "Số điện thoại phải bắt đầu bằng số 0 và có từ 10 đến 11 số")
    private String phoneNumber;

    private Integer gender;  // Có thể dùng giá trị 1: Nam, 2: Nữ, 3: Khác

    @Past(message = "Vui lòng nhập ngày sinh hợp lệ (trước ngày hiện tại)")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private Date birthday;

}