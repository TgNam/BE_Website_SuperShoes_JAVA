package org.example.datn_website_supershoes.service;

import org.example.datn_website_supershoes.Enum.Role;
import org.example.datn_website_supershoes.Enum.Status;
import org.example.datn_website_supershoes.dto.accountWithPassword.AccountWithPassword;
import org.example.datn_website_supershoes.dto.request.AccountRequest;
import org.example.datn_website_supershoes.dto.response.AccountResponse;
import org.example.datn_website_supershoes.model.Account;
import org.example.datn_website_supershoes.repository.AccountRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.List;
import java.util.Optional;

@Service
public class AccountService {

    @Autowired
    AccountRepository accountRepository;
    @Autowired
    private EmailService emailService;
    @Autowired
    private RandomPasswordGeneratorService randomPassword;
    PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();


    public Account createAccount(AccountRequest accountRequest) {
        Optional<Account> accountOP = accountRepository.findByEmail(accountRequest.getEmail());
        if (accountOP.isPresent()) {
            throw new RuntimeException("Email " + accountRequest.getEmail() + " đã tồn tại trong hệ thống. Vui lòng sử dụng email khác.");
        }
        AccountWithPassword accountWithPassword = convertAccountRequestDTO(accountRequest);
        Account account = accountRepository.save(accountWithPassword.getAccount());
        if (account != null){
            String password = accountWithPassword.getPassword();
            emailService.sendEmail(
                    account.getEmail(),
                    "Mật khẩu của tài khoản: " + account.getEmail(),
                    "MK của bạn là: "+password);
            return account;
        }else {
            throw new RuntimeException("Lỗi thêm tài khoản mới !");
        }
    }

    public Account updateAccount(Long id, String username) {
        Account account = accountRepository.findById(id).orElseGet(() -> {
            throw new RuntimeException("Account not exits");
        });
        account.setName(username);
        return accountRepository.save(account);
    }

    public void deleteAccount(Long id) {
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Account not exits"));
        account.setStatus(Status.INACTIVE.toString());
        accountRepository.deleteById(id);
    }

    public List<AccountResponse> getAllAccountCustomerActive() {
        return accountRepository.listCustomerResponseByStatus(Role.CUSTOMER.toString());
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
}
