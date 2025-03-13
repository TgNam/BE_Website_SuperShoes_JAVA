package org.example.datn_website_supershoes.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AddressRequest {

    @NotNull(message = "id của tài khoản không được để trống")
    private Long idAccount;

    @NotBlank(message = "Mã thành phố là bắt buộc")
    private String codeCity;

    @NotBlank(message = "Mã tỉnh thành là bắt buộc")
    private String codeDistrict;

    @NotBlank(message = "Mã huyện/ phường/ xã là bắt buộc")
    private String codeWard;

    @NotBlank(message = "Địa chỉ là bắt buộc")
    @Size(min = 2, max = 100, message = "Tên phải chứa ít nhất 2 ký tự không được vượt quá 100 ký tự")
    private String address;

}