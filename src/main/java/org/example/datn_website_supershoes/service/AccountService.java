
package org.example.datn_website_supershoes.service;

import org.example.datn_website_supershoes.Enum.Role;
import org.example.datn_website_supershoes.Enum.Status;
import org.example.datn_website_supershoes.dto.accountWithPassword.AccountWithPassword;
import org.example.datn_website_supershoes.dto.request.AccountRequest;
import org.example.datn_website_supershoes.dto.request.AccountUpdateRequest;
import org.example.datn_website_supershoes.dto.request.AddressRequest;
import org.example.datn_website_supershoes.dto.request.EmployeeCreationRequest;
import org.example.datn_website_supershoes.dto.request.EmployeeUpdateRequest;
import org.example.datn_website_supershoes.dto.response.AccountResponse;
import org.example.datn_website_supershoes.model.Account;
import org.example.datn_website_supershoes.repository.AccountRepository;
import org.example.datn_website_supershoes.repository.AccountVoucherRepository;
import org.example.datn_website_supershoes.webconfig.AccountLockedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

@Service
public class AccountService {

    @Autowired
     AccountRepository accountRepository;
    @Autowired
     AddressService addressService;
    @Autowired
     AccountVoucherRepository accountVoucherRepository;
    @Autowired
     EmailService emailService;
    @Autowired
     RandomPasswordGeneratorService randomPassword;
    PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    
    public void createAccount(AccountRequest accountRequest) {
        Optional<Account> accountOP = accountRepository.findByEmail(accountRequest.getEmail());
        if (accountOP.isPresent()) {
            throw new RuntimeException("Email " + accountRequest.getEmail() + " đã tồn tại trong hệ thống. Vui lòng sử dụng email khác.");
        }
        AccountWithPassword accountWithPassword = convertAccountRequestDTO(accountRequest);
        Account account = accountRepository.save(accountWithPassword.getAccount());
        if (account != null) {
            String password = accountWithPassword.getPassword();
            String email = account.getEmail();

            String emailContent = String.format(
                    "Kính chào Quý khách hàng,%n" +
                            "Chào mừng bạn đến với %s!%n" +
                            "Thông tin tài khoản của bạn như sau:%n" +
                            "• Tên tài khoản: %s%n" +
                            "• Mật khẩu: %s%n" +
                            "Nếu bạn có bất kỳ câu hỏi nào hoặc cần hỗ trợ, vui lòng liên hệ với chúng tôi qua email %s hoặc số điện thoại %s.%n%n" +
                            "Trân trọng,%n%s",
                    "SuperShoes",
                    email,
                    password,
                    "namntph33821@gmail.com",
                    "0909 123 456",
                    "SuperShoes"
            );

            emailService.sendEmail(
                    email,
                    "Chào mừng bạn đến với SuperShoes!",
                    emailContent
            );
        } else {
            throw new RuntimeException("Lỗi thêm tài khoản mới!");
        }
    }

    public void createAccountEmployee(EmployeeCreationRequest employeeCreationRequest) {
        Optional<Account> accountOP = accountRepository.findByEmail(employeeCreationRequest.getAccountRequest().getEmail());
        if (accountOP.isPresent()) {
            throw new RuntimeException("Email " + employeeCreationRequest.getAccountRequest().getEmail() + " đã tồn tại trong hệ thống. Vui lòng sử dụng email khác.");
        }

        AccountWithPassword accountWithPassword = convertAccountRequestDTO(employeeCreationRequest.getAccountRequest());
        Account account = accountRepository.save(accountWithPassword.getAccount());

        if (account != null) {
            String password = accountWithPassword.getPassword();
            String email = account.getEmail();

            String emailContent = String.format(
                    "Kính chào Quý khách hàng,%n" +
                            "Chào mừng bạn đến với %s!%n" +
                            "Thông tin tài khoản của bạn như sau:%n" +
                            "• Tên tài khoản: %s%n" +
                            "• Mật khẩu: %s%n" +
                            "Nếu bạn có bất kỳ câu hỏi nào hoặc cần hỗ trợ, vui lòng liên hệ với chúng tôi qua email %s hoặc số điện thoại %s.%n%n" +
                            "Trân trọng,%n%s",
                    "SuperShoes",
                    email,
                    password,
                    "namntph33821@gmail.com",
                    "0909 123 456",
                    "SuperShoes"
            );

            emailService.sendEmail(
                    email,
                    "Chào mừng bạn đến với SuperShoes!",
                    emailContent
            );

            AddressRequest addressRequest = AddressRequest.builder()
                    .idAccount(account.getId())
                    .codeCity(employeeCreationRequest.getAddressRequest().getCodeCity())
                    .codeDistrict(employeeCreationRequest.getAddressRequest().getCodeDistrict())
                    .codeWard(employeeCreationRequest.getAddressRequest().getCodeWard())
                    .address(employeeCreationRequest.getAddressRequest().getAddress())
                    .build();
            addressService.createAddress(addressRequest);

        } else {
            throw new RuntimeException("Lỗi thêm tài khoản mới!");
        }
    }


    public void updateAccountEmployee(Long idAccount, Long idAddress, EmployeeUpdateRequest employeeUpdateRequest) {
        Account account = accountRepository.findById(idAccount).orElseGet(() -> {
            throw new RuntimeException("Tài khoản không tồn tại");
        });
        account.setName(employeeUpdateRequest.getAccountRequest().getName());
        account.setPhoneNumber(employeeUpdateRequest.getAccountRequest().getPhoneNumber());
        account.setGender(employeeUpdateRequest.getAccountRequest().getGender());
        account.setBirthday(employeeUpdateRequest.getAccountRequest().getBirthday());
        Account UpdateAccount = accountRepository.save(account);
        if (UpdateAccount != null) {
            AddressRequest addressRequest = AddressRequest.builder()
                    .idAccount(account.getId())
                    .codeCity(employeeUpdateRequest.getAddressRequest().getCodeCity())
                    .codeDistrict(employeeUpdateRequest.getAddressRequest().getCodeDistrict())
                    .codeWard(employeeUpdateRequest.getAddressRequest().getCodeWard())
                    .address(employeeUpdateRequest.getAddressRequest().getAddress())
                    .build();
            addressService.updateAddress(idAddress, addressRequest);
        } else {
            throw new RuntimeException("Lỗi cập nhật tài khoản !");
        }
    }

    public void updateAccount(Long idAccount, AccountUpdateRequest accountRequest) {
        Account account = accountRepository.findById(idAccount).orElseGet(() -> {
            throw new RuntimeException("Tài khoản không tồn tại");
        });
        account.setName(accountRequest.getName());
        account.setPhoneNumber(accountRequest.getPhoneNumber());
        account.setGender(accountRequest.getGender());
        account.setBirthday(accountRequest.getBirthday());
        accountRepository.save(account);
    }

    public void updateStatus(Long idAccount, boolean aBoolean) {
        Optional<Account> accountOt = accountRepository.findById(idAccount);
        if (!accountOt.isPresent()) {
            throw new RuntimeException("Id " + accountOt.get().getId() + " của tài khoản không tồn tại");
        }
        String newStatus = aBoolean ? Status.ACTIVE.toString() : Status.INACTIVE.toString();
        accountOt.get().setStatus(newStatus);
        accountRepository.save(accountOt.get());
    }

    public List<AccountResponse> getAllAccountCustomerActive() {
        return accountRepository.listCustomerResponseByStatus(Role.CUSTOMER.toString());
    }

    public AccountResponse findAccountById(Long idAccount) {
        Optional<Account> accountOP = accountRepository.findById(idAccount);
        if (!accountOP.isPresent()) {
            throw new RuntimeException("Đối tượng không tồn tại .");
        }
        AccountResponse accountResponse = AccountResponse.builder()
                .id(accountOP.get().getId())
                .name(accountOP.get().getName())
                .email(accountOP.get().getEmail())
                .phoneNumber(accountOP.get().getPhoneNumber())
                .role(accountOP.get().getRole())
                .gender(accountOP.get().getGender())
                .birthday(accountOP.get().getBirthday())
                .rewards(accountOP.get().getRewards())
                .status(accountOP.get().getStatus())
                .build();
        return accountResponse;
    }

    public List<AccountResponse> getAllAccountEmployeeActive() {
        return accountRepository.listEmployeeResponseByStatus(Role.EMPLOYEE.toString());
    }

    public AccountWithPassword convertAccountRequestDTO(AccountRequest accountRequest) {
        String password = randomPassword.getPassword();
        Account account = Account.builder()
                .name(accountRequest.getName())
                .email(accountRequest.getEmail())
                .phoneNumber(accountRequest.getPhoneNumber())
                .password(passwordEncoder.encode(password))
                .role(accountRequest.getRole())
                .gender(accountRequest.getGender())
                .birthday(accountRequest.getBirthday())
                .rewards(0)
                .build();
        account.setStatus(accountRequest.getStatus());
        return new AccountWithPassword(account, password);
    }

    public List<String> findEmailsByCustomerIds(List<Long> customerIds) {
        if (customerIds == null || customerIds.isEmpty()) {
            throw new IllegalArgumentException("Customer IDs list cannot be empty.");
        }
        return accountRepository.findEmailsByCustomerIds(customerIds);
    }

    public Account getUseLogin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // Tìm kiếm tài khoản bằng email
        Account account = accountRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy địa chỉ Email: " + authentication.getName()));

        // Kiểm tra xem tài khoản có bị khóa không
        if (!account.isAccountNonLocked()) {
            throw new AccountLockedException("Tài khoản đã bị khóa!");
        }

        return account;
    }


}

