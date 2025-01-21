package org.example.datn_website_supershoes.controller;

import jakarta.validation.Valid;
import org.example.datn_website_supershoes.dto.request.AccountUpdateRequest;
import org.example.datn_website_supershoes.dto.request.AccountRequest;
import org.example.datn_website_supershoes.dto.request.EmployeeCreationRequest;
import org.example.datn_website_supershoes.dto.request.EmployeeUpdateRequest;
import org.example.datn_website_supershoes.dto.response.AccountResponse;
import org.example.datn_website_supershoes.dto.response.Response;
import org.example.datn_website_supershoes.model.Account;
import org.example.datn_website_supershoes.service.AccountService;
import org.example.datn_website_supershoes.webconfig.AccountLockedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;


import java.util.List;
import java.util.stream.Collectors;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/account")
public class AccountRestAPI {
    @Autowired
    AccountService accountService;

    @PostMapping("/create")
    public ResponseEntity<?> createAccount(@RequestBody @Valid AccountRequest accountRequest, BindingResult result) {
        try {
            if (result.hasErrors()) {
                List<String> errors = result.getAllErrors().stream()
                        .map(error -> error.getDefaultMessage())
                        .collect(Collectors.toList());
                return ResponseEntity.badRequest().body(errors);
            }
             accountService.createAccount(accountRequest);
            return ResponseEntity.ok("Thêm người dùng mới thành công!");
        } catch (RuntimeException e) {
            return ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .body(Response.builder()
                            .status(HttpStatus.CONFLICT.toString())
                            .mess(e.getMessage())
                            .build()
                    );
        }
    }

    @PostMapping("/createEmployee")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> createEmployee(@RequestBody @Valid EmployeeCreationRequest employeeCreationRequest, BindingResult result) {
        try {
            if (result.hasErrors()) {
                List<String> errors = result.getAllErrors().stream()
                        .map(error -> error.getDefaultMessage())
                        .collect(Collectors.toList());
                return ResponseEntity.badRequest().body(errors);
            }
            System.out.println(employeeCreationRequest.getAccountRequest().getEmail());
            accountService.createAccountEmployee(employeeCreationRequest);
            return ResponseEntity.ok("Thêm nhân viên mới thành công!");
        } catch (RuntimeException e) {
            return ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .body(Response.builder()
                            .status(HttpStatus.CONFLICT.toString())
                            .mess(e.getMessage())
                            .build()
                    );
        }
    }

    @PutMapping("/updateEmployee")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateEmployee(
            @RequestParam(value = "idAccount", required = false) Long idAccount,
            @RequestParam(value = "idAddress", required = false) Long idAddress,
            @RequestBody @Valid EmployeeUpdateRequest employeeUpdateRequest,
            BindingResult result
    ) {
        try {
            // Kiểm tra nếu idAccount bị trống (null)
            if (idAccount == null) {
                return ResponseEntity.badRequest().body(
                        Response.builder()
                                .status(HttpStatus.BAD_REQUEST.toString())
                                .mess("Lỗi: ID tài khoản không được để trống!")
                                .build()
                );
            }
            if (idAddress == null) {
                return ResponseEntity.badRequest().body(
                        Response.builder()
                                .status(HttpStatus.BAD_REQUEST.toString())
                                .mess("Lỗi: ID địa chỉ không được để trống!")
                                .build()
                );
            }
            if (result.hasErrors()) {
                List<String> errors = result.getAllErrors().stream()
                        .map(error -> error.getDefaultMessage())
                        .collect(Collectors.toList());
                return ResponseEntity.badRequest().body(errors);
            }
             accountService.updateAccountEmployee(idAccount, idAddress, employeeUpdateRequest);
            return ResponseEntity.ok("Cập nhật người dùng thành công!");
        } catch (RuntimeException e) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(Response.builder()
                            .status(HttpStatus.NOT_FOUND.toString())
                            .mess(e.getMessage())
                            .build()
                    );
        }
    }

    @PutMapping("/updateAccount")
    @PreAuthorize("hasAnyRole('ADMIN','EMPLOYEE','CUSTOMER')")
    public ResponseEntity<?> updateAccount(
            @RequestParam(value = "idAccount", required = false) Long idAccount,
            @RequestBody @Valid AccountUpdateRequest accountRequest,
            BindingResult result
    ) {
        try {
            // Kiểm tra nếu idAccount bị trống (null)
            if (idAccount == null) {
                return ResponseEntity.badRequest().body(
                        Response.builder()
                                .status(HttpStatus.BAD_REQUEST.toString())
                                .mess("Lỗi: ID địa chỉ không được để trống!")
                                .build()
                );
            }
            if (result.hasErrors()) {
                List<String> errors = result.getAllErrors().stream()
                        .map(error -> error.getDefaultMessage())
                        .collect(Collectors.toList());
                return ResponseEntity.badRequest().body(errors);
            }
             accountService.updateAccount(idAccount, accountRequest);
            return ResponseEntity.ok("Cập nhật nhân viên thành công!");
        } catch (RuntimeException e) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(Response.builder()
                            .status(HttpStatus.NOT_FOUND.toString())
                            .mess(e.getMessage())
                            .build()
                    );
        }
    }

    @PutMapping("/updateStatus")
    @PreAuthorize("hasAnyRole('ADMIN','EMPLOYEE')")
    public ResponseEntity<?> updateStatus(
            @RequestParam(value = "id", required = false) Long id,
            @RequestParam(value = "aBoolean", required = false) boolean aBoolean
    ) {
        try {
            if (id == null) {
                return ResponseEntity.badRequest().body(
                        Response.builder()
                                .status(HttpStatus.BAD_REQUEST.toString())
                                .mess("Lỗi: ID tài khoản không được để trống!")
                                .build()
                );
            }
             accountService.updateStatus(id, aBoolean);
            return ResponseEntity.ok("Cập nhật trạng tài khoản thành công!");
        } catch (RuntimeException e) {
            return ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .body(Response.builder()
                            .status(HttpStatus.CONFLICT.toString())
                            .mess(e.getMessage())
                            .build()
                    );
        }
    }

    @GetMapping("/list-accounts-customer")
//    @PreAuthorize("hasAnyRole('ADMIN','EMPLOYEE')")
    public ResponseEntity<?> getAllAccount() {
        try{
        List<AccountResponse> accountResponses = accountService.getAllAccountCustomerActive();
        return ResponseEntity.ok(accountResponses);
        }catch (RuntimeException e){
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/list-accounts-customer-search")
    @PreAuthorize("hasAnyRole('ADMIN','EMPLOYEE')")
    public ResponseEntity<?> getAllAccountCustomerSearch(
            @RequestParam("search") String search,
            @RequestParam("status") String status) {
        try{
        String searchLower = search.trim().toLowerCase();
        List<AccountResponse> accountResponses = accountService.getAllAccountCustomerActive().stream()
                .filter(account -> {
                    String accountName = account.getName().toLowerCase();
                    String accountPhone = account.getPhoneNumber().toLowerCase();
                    return accountName.contains(searchLower) || accountPhone.contains(searchLower);
                })
                .filter(account -> account.getStatus().equalsIgnoreCase(status.trim().toLowerCase()))
                .collect(Collectors.toList());
        return ResponseEntity.ok(accountResponses);
        }catch (RuntimeException e){
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/findAccounts")
    @PreAuthorize("hasAnyRole('ADMIN','EMPLOYEE','CUSTOMER')")
    public ResponseEntity<?> findAccounts(@RequestParam(value = "idAccount", required = false) Long idAccount) {
        try {
            if (idAccount == null) {
                return ResponseEntity.badRequest().body(
                        Response.builder()
                                .status(HttpStatus.BAD_REQUEST.toString())
                                .mess("Lỗi: ID của tài khoản không được để trống!")
                                .build()
                );
            }
            return ResponseEntity.ok().body(accountService.findAccountById(idAccount));
        } catch (RuntimeException e) {
            return ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .body(Response.builder()
                            .status(HttpStatus.CONFLICT.toString())
                            .mess(e.getMessage())
                            .build()
                    );
        }
    }

    @GetMapping("/list-accounts-employee")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getAllAccountEmployee() {
        try{
            List<AccountResponse> accountResponses = accountService.getAllAccountEmployeeActive();
            return ResponseEntity.ok(accountResponses);
        }catch (RuntimeException e){
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/list-accounts-employee-search")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getAllAccountEmployeeSearch(
            @RequestParam("search") String search,
            @RequestParam("status") String status) {
        try{
        String searchLower = search.trim().toLowerCase();
        List<AccountResponse> accountResponses = accountService.getAllAccountEmployeeActive().stream()
                .filter(account -> {
                    String accountName = account.getName().toLowerCase();
                    String accountPhone = account.getPhoneNumber().toLowerCase();
                    return accountName.contains(searchLower) || accountPhone.contains(searchLower);
                })
                .filter(account -> account.getStatus().equalsIgnoreCase(status.trim().toLowerCase()))
                .collect(Collectors.toList());
            return ResponseEntity.ok(accountResponses);
        }catch (RuntimeException e){
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/get-account-login")
    public ResponseEntity<?> getAccountLogin() {
        try {
            return ResponseEntity.ok().body(accountService.getUseLogin());
        } catch (AccountLockedException e) {
            return ResponseEntity.status(HttpStatus.LOCKED).body(
                    Response.builder()
                            .status(HttpStatus.LOCKED.toString())
                            .mess(e.getMessage())
                            .build()
            );
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(
                    Response.builder()
                            .status(HttpStatus.CONFLICT.toString())
                            .mess(e.getMessage())
                            .build()
            );
        }catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    Response.builder()
                            .status(HttpStatus.INTERNAL_SERVER_ERROR.toString())
                            .mess("Đã xảy ra lỗi không mong muốn: " + e.getMessage())
                            .build()
            );
        }
    }

}
