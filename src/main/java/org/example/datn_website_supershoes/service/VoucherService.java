package org.example.datn_website_supershoes.service;

import org.springframework.transaction.annotation.Transactional;
import org.apache.commons.lang3.RandomStringUtils;
import org.example.datn_website_supershoes.Enum.Status;
import org.example.datn_website_supershoes.dto.request.VoucherRequest;
import org.example.datn_website_supershoes.dto.response.VoucherBillResponse;
import org.example.datn_website_supershoes.dto.response.VoucherResponse;
import org.example.datn_website_supershoes.model.Account;
import org.example.datn_website_supershoes.model.AccountVoucher;
import org.example.datn_website_supershoes.model.Voucher;
import org.example.datn_website_supershoes.repository.AccountRepository;
import org.example.datn_website_supershoes.repository.AccountVoucherRepository;
import org.example.datn_website_supershoes.repository.VoucherRepository;
import org.example.datn_website_supershoes.webconfig.NotificationController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;


@Service
public class VoucherService {

    @Autowired
    VoucherRepository voucherRepository;

    @Autowired
    AccountRepository accountRepository;

    @Autowired
    AccountVoucherRepository accountVoucherRepository;

    @Autowired
    NotificationController notificationController;

    public Account getUseLogin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Account user = accountRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("Lỗi liên quan đến đăng nhập vui lòng thử lại"));
        return user;
    }

    private static final Logger logger = LoggerFactory.getLogger(VoucherService.class);

    private String generateVoucherCode() {
        int length = 6 + (int) (Math.random() * 3);
        return RandomStringUtils.randomAlphanumeric(length).toUpperCase();
    }

    @Transactional
    @Scheduled(cron = "0 * * * * *")
    public void updateVoucherStatuses() {
        List<Voucher> vouchers = voucherRepository.findAll();
        Date now = new Date();

        for (Voucher voucher : vouchers) {
            String oldStatus = voucher.getStatus();

            if (!oldStatus.equals(Status.EXPIRED.toString()) && !oldStatus.equals(Status.ENDED_EARLY.toString())) {
                if (voucher.getStartAt().after(now)) {
                    voucher.setStatus(Status.UPCOMING.toString());
                } else if (voucher.getStartAt().before(now) && voucher.getEndAt().after(now)) {
                    voucher.setStatus(Status.ONGOING.toString());
                } else if (voucher.getEndAt().before(now)) {
                    voucher.setStatus(Status.EXPIRED.toString());
                }
            }

            if (!oldStatus.equals(voucher.getStatus())) {
                logger.info("Voucher {} status changed from {} to {}", voucher.getCodeVoucher(), oldStatus, voucher.getStatus());
                voucherRepository.save(voucher);
                notificationController.sendNotification();
            }
        }
    }

    public long countVouchers(Specification<Voucher> spec) {
        return voucherRepository.count(spec);
    }

    public Voucher createVoucher(VoucherRequest voucherRequest) {
        if (voucherRequest.getIsPrivate() && (voucherRequest.getAccountIds() == null || voucherRequest.getAccountIds().isEmpty())) {
            throw new IllegalArgumentException("For private vouchers, account IDs must be provided.");
        }

        Voucher voucher = convertVoucherRequestDTO(voucherRequest);
        voucher.setCodeVoucher(generateVoucherCode());
        Date currentDate = new Date();

        voucher.setStartAt(convertToUTC(voucherRequest.getStartAt()));
        voucher.setEndAt(convertToUTC(voucherRequest.getEndAt()));

        Account creator = getUseLogin(); // Lấy thông tin người đăng nhập
        voucher.setCreatedBy(creator.getName());
        updateVoucherStatus(voucher, currentDate);
        Voucher savedVoucher = voucherRepository.save(voucher);

        // voucher riêng tư
        if (voucher.getIsPrivate()) {
            List<AccountVoucher> accountVouchers = voucherRequest.getAccountIds().stream()
                    .filter(Objects::nonNull)
                    .map(accountId -> {
                        Account account = accountRepository.findById(accountId)
                                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản với ID: " + accountId));

                          AccountVoucher accountVoucher = new AccountVoucher();
                        accountVoucher.setVoucher(savedVoucher);
                        accountVoucher.setAccount(account);
                        accountVoucher.setCreatedBy(creator.getName());
                        accountVoucher.setStatus("ACTIVE");
                        accountVoucher.setCreatedAt(new Date());

                        return accountVoucher;
                    })
                    .collect(Collectors.toList());
            accountVoucherRepository.saveAll(accountVouchers);
        }
        return savedVoucher;
    }

    public Voucher updateVoucher(Long id, VoucherRequest voucherRequest) {

        Voucher voucher = voucherRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Voucher not found"));

        Account updater = getUseLogin();

        voucher.setStartAt(convertToUTC(voucherRequest.getStartAt()));
        voucher.setEndAt(convertToUTC(voucherRequest.getEndAt()));
        voucher.setQuantity(voucherRequest.getQuantity());
        voucher.setUpdatedBy(updater.getName());

        Date currentDate = new Date();
        updateVoucherStatus(voucher, currentDate);

        Voucher updatedVoucher = voucherRepository.save(voucher);
        notificationController.sendNotification();
        return updatedVoucher;
    }

    private Date convertToUTC(Date date) {
        if (date == null) {
            return null;
        }
        return Date.from(ZonedDateTime.ofInstant(date.toInstant(), ZoneOffset.UTC).toInstant());
    }

    public VoucherResponse getVoucherById(Long id) {
        Voucher voucher = voucherRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Voucher not found"));

        VoucherResponse voucherResponse = convertToVoucherResponse(voucher);
        if (voucher.getIsPrivate()) {
            List<Long> accountIds = accountVoucherRepository.findAccountIdsByVoucherId(id);
            voucherResponse.setAccountIds(accountIds);
        }
        return voucherResponse;
    }

    public VoucherResponse getVoucherBycodeVoucher(String codeVoucher) {
        Voucher voucher = voucherRepository.findByCodeVoucher(codeVoucher)
                .orElseThrow(() -> new RuntimeException("Voucher not found"));

        VoucherResponse voucherResponse = convertToVoucherResponse(voucher);
        return voucherResponse;
    }

    public Voucher deleteVoucher(Long id) {
        Voucher voucher = voucherRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Voucher not found"));

        if (Status.EXPIRED.toString().equals(voucher.getStatus())) {
            throw new RuntimeException("Cannot delete an expired voucher.");
        }

        voucher.setStatus(Status.EXPIRED.toString());
        accountVoucherRepository.updateStatusByVoucherId(id, "INACTIVE");
        return voucherRepository.save(voucher);
    }

    public Voucher endVoucherEarly(Long id) {
        Voucher voucher = voucherRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Voucher not found"));

        Account updater = getUseLogin();

        voucher.setStatus(Status.ENDED_EARLY.toString());
        voucher.setUpdatedBy(updater.getName());
        accountVoucherRepository.updateStatusByVoucherId(id, "INACTIVE");
        return voucherRepository.save(voucher);
    }

    public Voucher reactivateVoucher(Long id) {
        Voucher voucher = voucherRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Voucher not found"));

        if (!Status.ENDED_EARLY.toString().equals(voucher.getStatus())) {
            throw new RuntimeException("Only vouchers with 'Ended Early' status can be reactivated.");
        }

        Account updater = getUseLogin();
        voucher.setUpdatedBy(updater.getName());

        Date currentDate = new Date();
        updateVoucherStatus(voucher, currentDate);
        accountVoucherRepository.updateStatusByVoucherId(id, "ACTIVE");
        return voucherRepository.save(voucher);
    }

    private void updateVoucherStatus(Voucher voucher, Date currentDate) {
        if (voucher.getStartAt().after(currentDate)) {
            voucher.setStatus(Status.UPCOMING.toString());
        } else if (voucher.getStartAt().before(currentDate) && voucher.getEndAt().after(currentDate)) {
            voucher.setStatus(Status.ONGOING.toString());
        } else {
            voucher.setStatus(Status.EXPIRED.toString());
        }
        notificationController.sendNotification();
    }

    private VoucherResponse convertToVoucherResponse(Voucher voucher) {
        VoucherResponse response = new VoucherResponse();
        BeanUtils.copyProperties(voucher, response);
        return response;
    }

    private Voucher convertVoucherRequestDTO(VoucherRequest voucherRequest) {
        Voucher voucher = Voucher.builder()
                .codeVoucher(voucherRequest.getCodeVoucher())
                .name(voucherRequest.getName())
                .note(voucherRequest.getNote())
                .value(voucherRequest.getValue())
                .quantity(voucherRequest.getQuantity())
                .maximumDiscount(voucherRequest.getMaximumDiscount())
                .type(voucherRequest.getType())
                .minBillValue(voucherRequest.getMinBillValue())
                .isPrivate(voucherRequest.getIsPrivate())
                .build();

        if (voucherRequest.getStartAt() != null) {
            voucher.setStartAt(convertToUTC(voucherRequest.getStartAt()));
        }
        if (voucherRequest.getEndAt() != null) {
            voucher.setEndAt(convertToUTC(voucherRequest.getEndAt()));
        }

        return voucher;
    }

    public Page<VoucherResponse> getVouchers(Specification<Voucher> spec, Pageable pageable) {
        Page<Voucher> voucherPage = voucherRepository.findAll(spec, pageable);
        return voucherPage.map(this::convertToVoucherResponse);
    }

    public List<VoucherBillResponse> findListVoucherByStatusAndIsPublic() {
        return voucherRepository.findListVoucherByStatusAndIsPublic(Status.ONGOING.toString(), false);
    }

    public List<VoucherBillResponse> findListVoucherByStatusAndIsPrivate(Long idAccount) {
        List<Long> idVoucher = accountVoucherRepository.findIdVoucherByIdAccount(idAccount);
        return voucherRepository.findListVoucherByListIdAndStatusAndPrivate(Status.ONGOING.toString(), idVoucher, true);
    }

    public VoucherBillResponse findVoucherByListIdAndStatus(Long idVoucher) {
        Optional<VoucherBillResponse> voucherBillResponse = voucherRepository.findVoucherByListIdAndStatus(Status.ONGOING.toString(), idVoucher);
        if (!voucherBillResponse.isPresent()) {
            throw new RuntimeException("Phiếu giảm giá không tồn tại .");
        }
        return voucherBillResponse.get();
    }
}
