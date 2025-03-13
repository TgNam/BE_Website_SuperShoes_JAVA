package org.example.datn_website_supershoes.service;

import org.example.datn_website_supershoes.Enum.Status;
import org.example.datn_website_supershoes.dto.request.PayBillRequest;
import org.example.datn_website_supershoes.dto.request.ProductDetailPromoRequest;
import org.example.datn_website_supershoes.dto.response.*;
import org.example.datn_website_supershoes.model.*;
import org.example.datn_website_supershoes.repository.*;
import org.example.datn_website_supershoes.webconfig.NotificationController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class BillByEmployeeService {

    @Autowired
    BillByEmployeeRepository billByEmployeeRepository;
    @Autowired
    BillRepository billRepository;
    @Autowired
    BillDetailRepository billDetailRepository;
    @Autowired
    VoucherRepository voucherRepository;
    @Autowired
    AccountRepository accountRepository;
    @Autowired
    AccountVoucherRepository accountVoucherRepository;
    @Autowired
    PayBillRepository payBillRepository;
    @Autowired
    PayBillService PayBillService;
    @Autowired
    ProductDetailRepository productDetailRepository;
    @Autowired
    CartDetailRepository cartDetailRepository;
    @Autowired
    PromotionDetailRepository promotionDetailRepository;
    @Autowired
    NotificationController notificationController;
    //Lấy tối đa 5 phần tử
    private static final int MAX_DISPLAY_BILLS = 5;
    private static final BigDecimal SHIPPING_PRICE = BigDecimal.valueOf(30000);
    public Account getUseLogin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Account user = accountRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("Lỗi liên quan đến đăng nhập vui lòng thử lại"));
        return user;
    }

    @Transactional
    public void findBillsOlder() {
        List<Bill> billsDay1 = billByEmployeeRepository.findBillsOlder(1, Arrays.asList(Status.WAITING_FOR_PAYMENT.toString()));
        List<Bill> billsDay7 = billByEmployeeRepository.findBillsOlder(7, Arrays.asList(Status.PENDING.toString(),Status.CONFIRMED.toString(),Status.SHIPPED.toString(), Status.WAITTING_FOR_SHIPPED.toString()));
        if (!billsDay1.isEmpty()) {
            for (Bill bill : billsDay1) {
                List<BillDetail> listBillDetail = billDetailRepository.findByIdBill(bill.getId());
                for (BillDetail billDetail : listBillDetail) {
                    //Tìm kiếm sản phẩm chi tiết theo id và trạng thái
                    Optional<ProductDetail> productDetailOptional = productDetailRepository.findById(billDetail.getProductDetail().getId());
                    // Kiểm tra sản phẩm có tồn tại không
                    if (!productDetailOptional.isPresent()) {
                        throw new RuntimeException("Tài nguyên sản phẩm không tồn tại trong hệ thống.");
                    }
                    ProductDetail productDetail = productDetailOptional.get();
                    //Số lượng sản phẩm của sản phẩm chi tiết
                    int quantityProductDetail = productDetail.getQuantity();
                    //Cộng sô lượng sản phẩm
                    quantityProductDetail = quantityProductDetail + billDetail.getQuantity();
                    productDetail.setQuantity(quantityProductDetail);
                    //Cập nhật lại sản phẩm
                    productDetailRepository.save(productDetail);
                }
                //Xóa hóa đơn
                billRepository.deleteById(bill.getId());
                notificationController.sendNotification();
            }
        }
        if (!billsDay7.isEmpty()) {
            for (Bill bill : billsDay7) {
                if (bill.getStatus().equals(Status.PENDING.toString())){
                    bill.setNote("Đã quá 7 ngày xác nhận đơn hàng!");
                    bill.setStatus(Status.CANCELLED.toString());
                } else if (bill.getStatus().equals(Status.CONFIRMED.toString())){
                    List<BillDetail> listBillDetail = billDetailRepository.findByIdBill(bill.getId());
                    for (BillDetail billDetail : listBillDetail) {
                        //Tìm kiếm sản phẩm chi tiết theo id và trạng thái
                        Optional<ProductDetail> productDetailOptional = productDetailRepository.findById(billDetail.getProductDetail().getId());
                        // Kiểm tra sản phẩm có tồn tại không
                        if (!productDetailOptional.isPresent()) {
                            throw new RuntimeException("Tài nguyên sản phẩm không tồn tại trong hệ thống.");
                        }
                        ProductDetail productDetail = productDetailOptional.get();
                        //Số lượng sản phẩm của sản phẩm chi tiết
                        int quantityProductDetail = productDetail.getQuantity();
                        //Cộng sô lượng sản phẩm
                        quantityProductDetail = quantityProductDetail + billDetail.getQuantity();
                        productDetail.setQuantity(quantityProductDetail);
                        //Cập nhật lại sản phẩm
                        productDetailRepository.save(productDetail);

                    }
                    bill.setNote("Đã quá 7 ngày giao hàng!");
                    bill.setStatus(Status.CANCELLED.toString());
                    notificationController.sendNotification();
                }else {
                    bill.setStatus(Status.FAILED.toString());
                }
                billRepository.save(bill);
            }
        }
    }

    //Lấy danh sách hóa đơn hiển thị và hóa đơn chờ
    public Map<String, List<String>> getDisplayAndWaitingBills() {

        Long idEmployees = getUseLogin().getId();
        //Lấy tất cả hóa đơn
        List<String> allBills = billByEmployeeRepository.findCodeBillWaitingForPayment(idEmployees, Status.WAITING_FOR_PAYMENT.toString());
        Map<String, List<String>> response = new HashMap<>();
        //Hóa đơn hiển thị
        List<String> displayBills;
        //Hóa đơn chờ
        List<String> waitingBills = new ArrayList<>();
        //Nếu allBills lớn hơn 5 phần tử
        if (allBills.size() > MAX_DISPLAY_BILLS) {
            displayBills = new ArrayList<>(allBills.subList(allBills.size() - 5, allBills.size()));
            waitingBills = allBills.subList(0, allBills.size() - 5);
        } else {
            displayBills = new ArrayList<>(allBills.subList(0, Math.min(MAX_DISPLAY_BILLS, allBills.size())));
        }
        response.put("displayBills", displayBills);
        response.put("waitingBills", waitingBills);

        return response;
    }

    public Map<String, List<String>> sortDisplayBills(List<String> displayBills, List<String> selectills) {
        // Khởi tạo map phản hồi
        Map<String, List<String>> response = new HashMap<>();

        Long idEmployees = getUseLogin().getId(); // ID của nhân viên (có thể thay đổi nếu cần)

        // Lấy danh sách tất cả hóa đơn đang chờ thanh toán
        List<String> allBills = billByEmployeeRepository.findCodeBillWaitingForPayment(idEmployees, Status.WAITING_FOR_PAYMENT.toString());

        // Kiểm tra xem số lượng bill trong selectills có vượt quá 5 không
        if (selectills != null && selectills.size() > 5) {
            throw new RuntimeException("Số lượng hóa đơn chọn hiển thị vượt quá 5 hóa đơn");
        }

        // Kiểm tra nếu bất kỳ hóa đơn nào trong selectills đã nằm trong displayBills
        if (selectills != null && displayBills != null) {
            List<String> duplicateInDisplay = selectills.stream()
                    .filter(displayBills::contains) // Lọc các hóa đơn đã có trong displayBills
                    .collect(Collectors.toList());

            // Nếu có bất kỳ hóa đơn nào trong selectills đã nằm trong displayBills, ném ngoại lệ
            if (!duplicateInDisplay.isEmpty()) {
                throw new RuntimeException("Các hóa đơn sau đã nằm trong hóa đơn hiển thị: " + duplicateInDisplay);
            }
        }

        // Kiểm tra xem các hóa đơn trong selectills có tồn tại trong allBills hay không
        if (selectills != null) {
            List<String> missingInAllBills = selectills.stream()
                    .filter(billCode -> !allBills.contains(billCode)) // Lọc các hóa đơn không tồn tại trong allBills
                    .collect(Collectors.toList());

            // Nếu có hóa đơn nào không tồn tại trong allBills, ném ngoại lệ
            if (!missingInAllBills.isEmpty()) {
                throw new RuntimeException("Các hóa đơn sau không tồn tại trong hệ thống: " + missingInAllBills);
            }
        }

        // Kiểm tra các hóa đơn bị trùng lặp trong selectills
        if (selectills != null) {
            Set<String> uniqueSelectBills = new HashSet<>();
            List<String> duplicateInSelectills = selectills.stream()
                    .filter(billCode -> !uniqueSelectBills.add(billCode)) // Nếu hóa đơn đã tồn tại trong uniqueSelectBills thì thêm vào duplicateInSelectills
                    .collect(Collectors.toList());

            // Nếu có bất kỳ hóa đơn nào bị trùng lặp trong selectills, ném ngoại lệ
            if (!duplicateInSelectills.isEmpty()) {
                throw new RuntimeException("Các hóa đơn sau bị trùng lặp trong danh sách chọn: " + duplicateInSelectills);
            }
        }

        // Kiểm tra xem hóa đơn nào trong displayBills không tồn tại trong allBills
        if (displayBills != null) {
            List<String> missingInDisplayBills = displayBills.stream()
                    .filter(billCode -> !allBills.contains(billCode)) // Lọc ra các hóa đơn không tồn tại trong allBills
                    .collect(Collectors.toList());

            // Nếu có bất kỳ hóa đơn nào không tồn tại trong allBills, ném ngoại lệ
            if (!missingInDisplayBills.isEmpty()) {
                throw new RuntimeException("Các hóa đơn sau không tồn tại trong hệ thống: " + missingInDisplayBills);
            }
        }

        // Kiểm tra các hóa đơn bị trùng lặp trong displayBills
        if (displayBills != null) {
            Set<String> uniqueDisplayBills = new HashSet<>();
            List<String> duplicateInDisplayBills = displayBills.stream()
                    .filter(billCode -> !uniqueDisplayBills.add(billCode)) // Nếu hóa đơn đã tồn tại trong uniqueDisplayBills thì thêm vào duplicateInDisplayBills
                    .collect(Collectors.toList());

            // Nếu có bất kỳ hóa đơn nào bị trùng lặp trong displayBills, ném ngoại lệ
            if (!duplicateInDisplayBills.isEmpty()) {
                throw new RuntimeException("Các hóa đơn sau bị trùng lặp trong danh sách hiển thị: " + duplicateInDisplayBills);
            }
        }

        // Khởi tạo danh sách hóa đơn chờ
        List<String> waitingBills;

        // Nếu số hóa đơn lớn hơn hoặc bằng MAX_DISPLAY_BILLS
        if (allBills.size() >= MAX_DISPLAY_BILLS) {
            // Kiểm tra xem danh sách displayBills có hợp lệ hay không
            if (selectills != null && displayBills != null && displayBills.size() == MAX_DISPLAY_BILLS) {
                // Thêm mã hóa đơn mới vào danh sách displayBills
                displayBills.addAll(selectills);
                // Lấy danh sách hóa đơn cần xóa
                List<String> removeBills = new ArrayList<>(displayBills.subList(0, selectills.size()));
                // Xóa hóa đơn trong danh sách displayBills (FIFO)
                displayBills.removeAll(removeBills);
            } else {
                // Nếu displayBills không hợp lệ, chỉ cần giữ lại hóa đơn đã có
                displayBills = new ArrayList<>(allBills.subList(Math.max(0, allBills.size() - MAX_DISPLAY_BILLS), allBills.size()));
            }

            // Cập nhật danh sách waitingBills bằng cách loại bỏ các hóa đơn đã hiển thị
            List<String> finalDisplayBills = displayBills;
            waitingBills = allBills.stream()
                    .filter(number -> !finalDisplayBills.contains(number)) // Chỉ giữ lại các phần tử không có trong displayBills
                    .collect(Collectors.toList());
        } else {
            // Nếu không đủ hóa đơn, hiển thị toàn bộ hóa đơn hiện có
            displayBills = new ArrayList<>(allBills);
            waitingBills = new ArrayList<>();
        }

        response.put("displayBills", displayBills);
        response.put("waitingBills", waitingBills);

        return response;
    }

    public Map<String, List<String>> createBillByEmployee(List<String> displayBills) {
        // Tạo hóa đơn mới và lưu vào cơ sở dữ liệu
        Bill bill = billByEmployeeRepository.save(convertBill());

        // Khởi tạo map phản hồi
        Map<String, List<String>> response = new HashMap<>();

        Long idEmployees = getUseLogin().getId(); // ID của nhân viên (có thể thay đổi nếu cần)

        // Lấy danh sách tất cả hóa đơn đang chờ thanh toán
        List<String> allBills = billByEmployeeRepository.findCodeBillWaitingForPayment(idEmployees, Status.WAITING_FOR_PAYMENT.toString());

        // Kiểm tra xem hóa đơn nào trong displayBills không tồn tại trong allBills
        if (displayBills != null) {
            // Kiểm tra các hóa đơn bị trùng lặp trong displayBills
            Set<String> uniqueBills = new HashSet<>();
            List<String> duplicateBills = displayBills.stream()
                    .filter(billCode -> !uniqueBills.add(billCode)) // Nếu hóa đơn đã tồn tại trong uniqueBills thì thêm vào duplicateBills
                    .collect(Collectors.toList());

            // Nếu có bất kỳ hóa đơn nào bị trùng, ném ngoại lệ
            if (!duplicateBills.isEmpty()) {
                throw new RuntimeException("Các hóa đơn sau bị trùng lặp: " + duplicateBills);
            }

            List<String> missingBills = displayBills.stream()
                    .filter(billCode -> !allBills.contains(billCode)) // Lọc ra các hóa đơn không tồn tại trong allBills
                    .collect(Collectors.toList());

            // Nếu có bất kỳ hóa đơn nào không tồn tại, ném ngoại lệ
            if (!missingBills.isEmpty()) {
                throw new RuntimeException("Các hóa đơn sau không tồn tại: " + missingBills);
            }
        }

        // Khởi tạo danh sách hóa đơn chờ
        List<String> waitingBills;

        // Nếu số hóa đơn lớn hơn hoặc bằng MAX_DISPLAY_BILLS
        if (allBills.size() >= MAX_DISPLAY_BILLS) {
            // Kiểm tra xem danh sách displayBills có hợp lệ hay không
            if (displayBills != null && displayBills.size() == MAX_DISPLAY_BILLS) {
                // Thêm mã hóa đơn mới vào danh sách displayBills
                displayBills.add(bill.getCodeBill());
                // Xóa hóa đơn đầu tiên trong danh sách displayBills (FIFO)
                displayBills.remove(0);
            } else {
                // Nếu displayBills không hợp lệ, chỉ cần giữ lại hóa đơn đã có
                displayBills = new ArrayList<>(allBills.subList(Math.max(0, allBills.size() - MAX_DISPLAY_BILLS), allBills.size()));
            }

            // Cập nhật danh sách waitingBills bằng cách loại bỏ các hóa đơn đã hiển thị
            List<String> finalDisplayBills = displayBills;
            waitingBills = allBills.stream()
                    .filter(number -> !finalDisplayBills.contains(number)) // Chỉ giữ lại các phần tử không có trong displayBills
                    .collect(Collectors.toList());
        } else {
            // Nếu không đủ hóa đơn, hiển thị toàn bộ hóa đơn hiện có
            displayBills = new ArrayList<>(allBills);
            waitingBills = new ArrayList<>();
        }

        // Cập nhật phản hồi
        response.put("displayBills", displayBills);
        response.put("waitingBills", waitingBills);

        return response;
    }

    public BillResponse findBillResponseByCodeBill(String codeBill) {
        Optional<BillResponse> billResponse = billRepository.findBillResponseByCodeBill(codeBill);
        if (!billResponse.isPresent()) {
            throw new RuntimeException("Hóa đơn không còn tồn tại");
        }
        return billResponse.get();
    }

    public static String generateRandomCode() {
        Random random = new Random();
        // Tạo một số ngẫu nhiên có 6 chữ số
        int randomNumber = 100000 + random.nextInt(900000); // Số ngẫu nhiên từ 100000 đến 999999
        // Kết hợp với tiền tố HD
        return "HD" + randomNumber;
    }

    public Bill convertBill() {
        //get lấy id của người dùng từ đăng nhập
        // vd lấy id của người đăng nhập đó bằng 2
        Long id = getUseLogin().getId();
        Account employee = accountRepository.findById(id).get();
        UUID uuid = UUID.randomUUID();
        String generatedCode = this.generateRandomCode() + "-" + uuid;
        Bill bill = Bill.builder()
                .employees(employee)
                .codeBill(generatedCode)
                .type(2)
                .build();
        bill.setCreatedBy(employee.getName());
        bill.setUpdatedBy(employee.getName());
        bill.setStatus(Status.WAITING_FOR_PAYMENT.toString());
        return bill;
    }

    @Transactional
    public void payBillByEmployee(
            String codeBill,
            boolean delivery,
            boolean postpaid,
            String codeVoucher,
            Long idAccount,
            String name,
            String phoneNumber,
            String address,
            String note
    ) {
        Optional<Bill> billOptional = billRepository.findByCodeBill(codeBill);
        if (billOptional.isEmpty()) {
            throw new RuntimeException("Hóa đơn " + codeBill + " không tồn tại!");
        }
        if (!billOptional.get().getStatus().equals(Status.WAITING_FOR_PAYMENT.toString())) {
            throw new RuntimeException("Hóa đơn " + codeBill + " không còn ở trạng thái thanh toán!");
        }
        Bill bill = billOptional.get(); // Lấy giá trị bill để sử dụng sau này

        boolean checkVoucher = false;
        boolean checkAccountVoucher = false;
        Voucher voucher = null;
        AccountVoucher accountVoucher = null;



        if (idAccount != null) {
            Optional<Account> accountOptional = accountRepository.findByIdAndStatus(idAccount, Status.ACTIVE.toString());
            if (accountOptional.isEmpty()) {
                throw new RuntimeException("Xảy ra lỗi khi tìm kiếm tài khoản");
            }
            bill.setCustomer(accountOptional.get());
            bill.setNameCustomer(accountOptional.get().getName());
            bill.setPhoneNumber(accountOptional.get().getPhoneNumber());
        }

        List<BillDetail> listBillDetail = billDetailRepository.findByIdBill(bill.getId());
        if (listBillDetail == null || listBillDetail.isEmpty()) {
            throw new RuntimeException("Chưa có sản phẩm nào trong giỏ hàng");
        }

        BigDecimal totalMerchandise = listBillDetail.stream()
                .map(billDetail -> billDetail.getPriceDiscount()
                        .multiply(BigDecimal.valueOf(billDetail.getQuantity()))
                        .setScale(0, RoundingMode.DOWN))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        bill.setTotalMerchandise(totalMerchandise.setScale(0, RoundingMode.DOWN));

        BigDecimal priceDiscount = BigDecimal.ZERO;
        BigDecimal totalAmount = totalMerchandise;
        if (delivery) {
            if (name == null || name.isBlank()) {
                throw new RuntimeException("Không được để trống tên người nhận");
            } else if (phoneNumber == null || phoneNumber.isBlank()) {
                throw new RuntimeException("Không được để trống số điện thoại người nhận");
            } else if (address == null || address.isBlank()) {
                throw new RuntimeException("Không được để trống địa chỉ nhận hàng");
            }
            if (note != null && !note.isBlank()) {
                bill.setNote(note);
            }
            bill.setNameCustomer(name);
            bill.setPhoneNumber(phoneNumber);
            bill.setAddress(address);
            totalAmount = totalAmount.add(SHIPPING_PRICE);
        }
        if (codeVoucher != null && !codeVoucher.isBlank()) {
            Optional<Voucher> voucherOptional = voucherRepository.findByCodeVoucher(codeVoucher);
            if (voucherOptional.isPresent()) {
                voucher = voucherOptional.get();

                if (voucher.getQuantity() <= 0) {
                    throw new RuntimeException("Đã hết phiếu giảm giá");
                }
                if (totalMerchandise.compareTo(voucher.getMinBillValue()) < 0) {
                    throw new RuntimeException("Hóa đơn không đủ điều kiện sử dụng phiếu giảm giá");
                }

                BigDecimal priceSale = totalMerchandise.multiply(BigDecimal.valueOf(voucher.getValue() / 100.0))
                        .setScale(0, RoundingMode.DOWN);
                BigDecimal maximumDiscount = voucher.getMaximumDiscount().max(BigDecimal.ZERO);

                priceDiscount = priceSale.compareTo(maximumDiscount) <= 0 ? priceSale : maximumDiscount;
                totalAmount = totalMerchandise.subtract(priceDiscount).setScale(0, RoundingMode.DOWN);

                if (voucher.getIsPrivate()) {
                    if (idAccount == null) {
                        throw new RuntimeException("Bạn không đủ điều kiện sử dụng voucher: " + voucher.getCodeVoucher());
                    }
                    Optional<AccountVoucher> accountVoucherOptional = accountVoucherRepository
                            .findAccountVoucherByIdAccountAndidVoucher(idAccount, voucher.getId());
                    if (accountVoucherOptional.isEmpty()) {
                        throw new RuntimeException("Bạn không đủ điều kiện sử dụng voucher: " + voucher.getCodeVoucher());
                    }
                    accountVoucher = accountVoucherOptional.get();
                    accountVoucher.setStatus(Status.INACTIVE.toString());
                    checkAccountVoucher = true;
                }

                int newQuantityVoucher = voucher.getQuantity() - 1;
                voucher.setQuantity(newQuantityVoucher);
                if (newQuantityVoucher <= 0) {
                    voucher.setStatus(Status.EXPIRED.toString());
                }
                checkVoucher = true;
                bill.setVoucher(voucher);
            }
        }

        bill.setPriceDiscount(priceDiscount.setScale(0, RoundingMode.DOWN));
        bill.setTotalAmount(totalAmount.setScale(0, RoundingMode.DOWN));

        BigDecimal totalPaid = payBillRepository.findByCodeBill(codeBill).stream()
                .map(PayBillOrderResponse::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (postpaid && !delivery) {
            throw new RuntimeException("Chức năng trả sau chỉ áp dụng khi giao hàng");
        } else if (postpaid) {
            if (totalPaid.setScale(0, RoundingMode.DOWN).compareTo(totalAmount.setScale(0, RoundingMode.DOWN)) < 0) {
                PayBillRequest payBillRequest = PayBillRequest.builder()
                        .amount(totalAmount.subtract(totalPaid))
                        .codeBill(codeBill)
                        .type(2)
                        .build();
                PayBillService.createPayBill(payBillRequest, 2, Status.WAITING_FOR_PAYMENT.toString());
            }
            bill.setStatus(Status.WAITTING_FOR_SHIPPED.toString());
        } else if (totalPaid.setScale(0, RoundingMode.DOWN).compareTo(totalAmount.setScale(0, RoundingMode.DOWN)) < 0) {
            throw new RuntimeException("Vui lòng thanh toán đủ số tiền trước khi thanh toán hóa đơn");
        } else {
            bill.setStatus(delivery ? Status.WAITTING_FOR_SHIPPED.toString() : Status.COMPLETED.toString());
        }

        if (checkVoucher) {
            voucherRepository.save(voucher);
        }
        if (checkAccountVoucher) {
            accountVoucherRepository.save(accountVoucher);
        }

        billRepository.save(bill);
        notificationController.sendEvent("PAYBILL_SUCCESS");
    }

    @Transactional
    public void payBillOnline(
            List<Long> IdCartDetail,
            String codeVoucher,
            Long idAccount,
            String name,
            String phoneNumber,
            String address,
            String note
    ) {
        // Ngưỡng tối đa
        BigDecimal limit = new BigDecimal("100000000");
        //Tạo mã hóa đơn
        UUID uuid = UUID.randomUUID();
        String generatedCode = this.generateRandomCode() + "-" + uuid;
        //Kiểm tra xem tài khoản có tồn tại hay không
        Account account = accountRepository.findByIdAndStatus(idAccount, Status.ACTIVE.toString())
                .orElseThrow(() -> new RuntimeException("Lỗi khi tìm kiếm tài khoản!"));
        boolean checkVoucher = false;
        boolean checkAccountVoucher = false;
        Voucher voucher = null;
        AccountVoucher accountVoucher = null;

        List<CartDetailProductDetailResponse> cartDetails = cartDetailRepository.findCartDetailByIdAccountAndIdCartDetail(idAccount, IdCartDetail);
        BigDecimal totalMerchandise = calculateTotalCartPriceForSelected(cartDetails);

        BigDecimal priceDiscount = BigDecimal.ZERO;
        BigDecimal totalAmount = totalMerchandise.add(SHIPPING_PRICE);

        if (cartDetails == null || cartDetails.isEmpty()) {
            throw new RuntimeException("Chưa có sản phẩm nào trong giỏ hàng");
        }
        Bill bill = Bill.builder()
                .customer(account)
                .codeBill(generatedCode)
                .nameCustomer(name)
                .phoneNumber(phoneNumber)
                .address(address)
                .note(note)
                .type(1)
                .build();
        bill.setCreatedBy(account.getName());
        bill.setUpdatedBy(account.getName());
        bill.setStatus(Status.PENDING.toString());

        if (codeVoucher != null && !codeVoucher.isBlank()) {
            Optional<Voucher> voucherOptional = voucherRepository.findByCodeVoucher(codeVoucher);
            if (voucherOptional.isPresent()) {
                voucher = voucherOptional.get();

                if (voucher.getQuantity() <= 0) {
                    throw new RuntimeException("Đã hết phiếu giảm giá");
                }
                if (totalMerchandise.compareTo(voucher.getMinBillValue()) < 0) {
                    throw new RuntimeException("Hóa đơn không đủ điều kiện sử dụng phiếu giảm giá");
                }
                BigDecimal priceSale = totalMerchandise.multiply(BigDecimal.valueOf(voucher.getValue() / 100.0))
                        .setScale(0, RoundingMode.DOWN);
                BigDecimal maximumDiscount = voucher.getMaximumDiscount().max(BigDecimal.ZERO);

                priceDiscount = priceSale.compareTo(maximumDiscount) <= 0 ? priceSale : maximumDiscount;
                totalAmount = totalMerchandise.subtract(priceDiscount).setScale(0, RoundingMode.DOWN);
                if (voucher.getIsPrivate()) {
                    if (idAccount == null) {
                        throw new RuntimeException("Bạn không đủ điều kiện sử dụng voucher: " + voucher.getCodeVoucher());
                    }
                    Optional<AccountVoucher> accountVoucherOptional = accountVoucherRepository
                            .findAccountVoucherByIdAccountAndidVoucher(idAccount, voucher.getId());
                    if (accountVoucherOptional.isEmpty()) {
                        throw new RuntimeException("Bạn không đủ điều kiện sử dụng voucher: " + voucher.getCodeVoucher());
                    }
                    accountVoucher = accountVoucherOptional.get();
                    accountVoucher.setStatus(Status.INACTIVE.toString());
                    checkAccountVoucher = true;
                }
                int newQuantityVoucher = voucher.getQuantity() - 1;
                voucher.setQuantity(newQuantityVoucher);
                if (newQuantityVoucher <= 0) {
                    voucher.setStatus(Status.EXPIRED.toString());
                }
                checkVoucher = true;
                bill.setVoucher(voucher);
            }
        }
        bill.setTotalMerchandise(totalMerchandise.setScale(0, RoundingMode.DOWN));
        bill.setPriceDiscount(priceDiscount);
        bill.setTotalAmount(totalAmount.setScale(0, RoundingMode.DOWN));

        if (totalAmount.compareTo(limit) > 0) {
            throw new RuntimeException("Tổng giá trị hàng hóa vượt quá giới hạn cho phép (100,000,000).");
        }
        // Lưu Bill trước
        Bill saveBill = billRepository.save(bill);

        // Thực hiện thêm hóa đơn chi tiết
        for (CartDetailProductDetailResponse request : cartDetails) {
            // Tìm kiếm sản phẩm chi tiết theo id và trạng thái
            Optional<ProductDetail> productDetailOptional = productDetailRepository.findByIdAndAndStatus(request.getIdProductDetail(), Status.ACTIVE.toString());

            // Kiểm tra sản phẩm có tồn tại không
            if (!productDetailOptional.isPresent()) {
                throw new RuntimeException("Lỗi khi tìm kiếm sản phẩm trong hệ thống.");
            }

            //Số lượng sản phẩm của sản phẩm chi tiết
            int quantityProductDetail = productDetailOptional.get().getQuantity();

            // Kiểm tra số lượng sản phẩm số lượng mua so với sản phẩm còn lại trong kho
            if (request.getQuantityCartDetail() > quantityProductDetail || quantityProductDetail <= 0) {
                throw new RuntimeException("Sản phẩm " + productDetailOptional.get().getProduct().getName() + " đang hết hàng.");
            }

            //Giá tiền sản phẩm
            BigDecimal priceProduct = productDetailOptional.get().getPrice();

            //Tìm kiếm sản phẩm có đang  sale hay không
            Optional<ProductPromotionResponse> productPromotionResponse = productDetailRepository.findProductPromotionByIdProductDetail(request.getIdProductDetail());

            //Kiểm tra xem nếu sản phẩm đang sale
            if (productPromotionResponse.isPresent()) {
                //Tìm kiếm đợt giảm giá chi tiết
                Optional<PromotionDetail> promotionDetail = promotionDetailRepository.findByIdAndAndStatus(productPromotionResponse.get().getIdPromotionDetail(), Status.ONGOING.toString());
                //Số lượng sản phẩm sale
                int quantityProductPromotion = productPromotionResponse.get().getQuantityPromotionDetail();
                //Giá sản phẩm sau khi sale
                BigDecimal promotionPrice = productPromotionResponse.get().getProductDetailPrice()
                        .multiply(BigDecimal.valueOf(1 - productPromotionResponse.get().getValue() / 100))
                        .setScale(0, RoundingMode.DOWN);
                //Nếu số lượng sản phẩm khách hàng mua nhỏ hơn số lượng đang sale
                if (request.getQuantityCartDetail() <= quantityProductPromotion) {
                    // Tạo hóa đơn chi tiết mới
                    BillDetail billDetail = new BillDetail();
                    billDetail.setProductDetail(productDetailOptional.get());
                    billDetail.setBill(saveBill);
                    billDetail.setQuantity(request.getQuantityCartDetail());
                    billDetail.setStatus(Status.PENDING.toString());
                    //Áp dụng giá sale
                    billDetail.setPriceDiscount(promotionPrice);
                    billDetailRepository.save(billDetail);

                    //Số lượng còn lại của sản phẩm sale
                    Integer newPromotionQuantity = quantityProductPromotion - request.getQuantityCartDetail();

                    //Cập nhật số lượng cho sản phẩm sale
                    promotionDetail.get().setQuantity(newPromotionQuantity);

                    //Cập nhận lại số lượng giảm giá
                    if (newPromotionQuantity <= 0) {
                        promotionDetail.get().setStatus(Status.FINISHED.toString());
                    }
                    //Cập nhật lại sản phản phẩm sale
                    promotionDetailRepository.save(promotionDetail.get());
                } else {
                    // Số lượng bán lẻ khách hàng sẽ mua ngoài số lượng sale
                    int retailQuantity = request.getQuantityCartDetail() - quantityProductPromotion;

                    BillDetail billDetailSale = new BillDetail();
                    billDetailSale.setProductDetail(productDetailOptional.get());
                    billDetailSale.setBill(saveBill);
                    billDetailSale.setQuantity(quantityProductPromotion);
                    billDetailSale.setStatus(Status.PENDING.toString());
                    billDetailSale.setPriceDiscount(promotionPrice);
                    billDetailRepository.save(billDetailSale);
                    if (retailQuantity > 0) {
                        BillDetail billDetail = new BillDetail();
                        billDetail.setProductDetail(productDetailOptional.get());
                        billDetail.setBill(saveBill);
                        billDetail.setQuantity(retailQuantity);
                        billDetail.setStatus(Status.PENDING.toString());
                        billDetail.setPriceDiscount(priceProduct);
                        billDetailRepository.save(billDetail);
                    }

                    // Cập nhật lại thông tin sản phẩm sale trong PromotionDetail
                    promotionDetail.get().setQuantity(0);
                    promotionDetail.get().setStatus(Status.FINISHED.toString());
                    promotionDetailRepository.save(promotionDetail.get());
                }
            }
            //Trường hợp sản phẩm không sale
            else {
                // Tạo hóa đơn chi tiết mới
                BillDetail billDetail = new BillDetail();
                billDetail.setProductDetail(productDetailOptional.get());
                billDetail.setBill(saveBill);
                billDetail.setQuantity(request.getQuantityCartDetail());
                billDetail.setStatus(Status.PENDING.toString());
                //Áp dụng giá gốc của sản phẩm
                billDetail.setPriceDiscount(productDetailOptional.get().getPrice());
                billDetailRepository.save(billDetail);
            }
            cartDetailRepository.deleteById(request.getIdCartDetail());
        }

        PayBillRequest payBillRequest = PayBillRequest.builder()
                .amount(totalAmount)
                .codeBill(saveBill.getCodeBill())
                .type(2)
                .build();
        PayBillService.createPayBill(payBillRequest, 2, Status.WAITING_FOR_PAYMENT.toString());
        if (checkVoucher) {
            voucherRepository.save(voucher);
        }
        if (checkAccountVoucher) {
            accountVoucherRepository.save(accountVoucher);
        }
        notificationController.sendNotification();
    }
    @Transactional
    public void payBillOnlinev2(
            List<ProductDetailPromoRequest> productDetailPromoRequests,
            String codeVoucher,
            Long idAccount,
            String name,
            String phoneNumber,
            String address,
            String note
    ) {
        // Ngưỡng tối đa
        BigDecimal limit = new BigDecimal("100000000");
        //Tạo mã hóa đơn
        UUID uuid = UUID.randomUUID();
        String generatedCode = this.generateRandomCode() + "-" + uuid;
        boolean checkVoucher = false;
        boolean checkAccountVoucher = false;
        Voucher voucher = null;
        AccountVoucher accountVoucher = null;
        Bill bill = Bill.builder()
                .codeBill(generatedCode)
                .nameCustomer(name)
                .phoneNumber(phoneNumber)
                .address(address)
                .note(note)
                .type(1)
                .build();
        if(idAccount!=null){
            //Kiểm tra xem tài khoản có tồn tại hay không
            Account account = accountRepository.findByIdAndStatus(idAccount, Status.ACTIVE.toString())
                    .orElseThrow(() -> new RuntimeException("Lỗi khi tìm kiếm tài khoản!"));
            bill.setCustomer(account);
            bill.setCreatedBy(account.getName());
            bill.setUpdatedBy(account.getName());
        }
        bill.setStatus(Status.PENDING.toString());
        // Lưu Bill trước
        Bill saveBill = billRepository.save(bill);
        BigDecimal totalMerchandise = BigDecimal.ZERO;
        for (ProductDetailPromoRequest request : productDetailPromoRequests) {
            // Tìm kiếm sản phẩm chi tiết theo id và trạng thái
            ProductDetail productDetail = productDetailRepository.findById(request.getIdProductDetail())
                    .orElseThrow(() -> new RuntimeException("Lỗi tài nguyên sản phẩm."));
            if(productDetail.getStatus().equals(Status.INACTIVE.toString())){
                throw new RuntimeException("Sản phẩm "+productDetail.getProduct().getName()+" hiện đã ngừng bán");
            }
            //Số lượng sản phẩm của sản phẩm chi tiết
            int quantityProductDetail = productDetail.getQuantity();

            // Kiểm tra số lượng sản phẩm số lượng mua so với sản phẩm còn lại trong kho
            if (request.getQuantity() > quantityProductDetail || quantityProductDetail <= 0) {
                throw new RuntimeException("Sản phẩm " + productDetail.getProduct().getName() + " đang hết hàng.");
            }

            //Giá tiền sản phẩm
            BigDecimal priceProduct = productDetail.getPrice();

            //Tìm kiếm sản phẩm có đang  sale hay không
            Optional<ProductPromotionResponse> productPromotionResponse = productDetailRepository.findProductPromotionByIdProductDetail(request.getIdProductDetail());

            //Kiểm tra xem nếu sản phẩm đang sale
            if (productPromotionResponse.isPresent()) {
                //Tìm kiếm đợt giảm giá chi tiết
                Optional<PromotionDetail> promotionDetail = promotionDetailRepository.findByIdAndAndStatus(productPromotionResponse.get().getIdPromotionDetail(), Status.ONGOING.toString());
                //Số lượng sản phẩm sale
                int quantityProductPromotion = productPromotionResponse.get().getQuantityPromotionDetail();
                //Giá sản phẩm sau khi sale
                BigDecimal promotionPrice = productPromotionResponse.get().getProductDetailPrice()
                        .multiply(BigDecimal.valueOf(1 - productPromotionResponse.get().getValue() / 100))
                        .setScale(0, RoundingMode.DOWN);
                //Nếu số lượng sản phẩm khách hàng mua nhỏ hơn số lượng đang sale
                if (request.getQuantity() <= quantityProductPromotion) {
                    // Tạo hóa đơn chi tiết mới
                    BillDetail billDetail = new BillDetail();
                    billDetail.setProductDetail(productDetail);
                    billDetail.setBill(saveBill);
                    billDetail.setQuantity(request.getQuantity());
                    billDetail.setStatus(Status.PENDING.toString());
                    //Áp dụng giá sale
                    billDetail.setPriceDiscount(promotionPrice);
                    billDetailRepository.save(billDetail);

                    //Số lượng còn lại của sản phẩm sale
                    Integer newPromotionQuantity = quantityProductPromotion - request.getQuantity();

                    //Cập nhật số lượng cho sản phẩm sale
                    promotionDetail.get().setQuantity(newPromotionQuantity);

                    //Cập nhận lại số lượng giảm giá
                    if (newPromotionQuantity <= 0) {
                        promotionDetail.get().setStatus(Status.FINISHED.toString());
                    }
                    //Cập nhật lại sản phản phẩm sale
                    totalMerchandise = totalMerchandise.add(promotionPrice.multiply(BigDecimal.valueOf(request.getQuantity())));
                    promotionDetailRepository.save(promotionDetail.get());
                } else {
                    // Số lượng bán lẻ khách hàng sẽ mua ngoài số lượng sale
                    int retailQuantity = request.getQuantity() - quantityProductPromotion;

                    BillDetail billDetailSale = new BillDetail();
                    billDetailSale.setProductDetail(productDetail);
                    billDetailSale.setBill(saveBill);
                    billDetailSale.setQuantity(quantityProductPromotion);
                    billDetailSale.setStatus(Status.PENDING.toString());
                    billDetailSale.setPriceDiscount(promotionPrice);
                    billDetailRepository.save(billDetailSale);
                    if (retailQuantity > 0) {
                        BillDetail billDetail = new BillDetail();
                        billDetail.setProductDetail(productDetail);
                        billDetail.setBill(saveBill);
                        billDetail.setQuantity(retailQuantity);
                        billDetail.setStatus(Status.PENDING.toString());
                        billDetail.setPriceDiscount(priceProduct);
                        billDetailRepository.save(billDetail);
                    }

                    // Cập nhật lại thông tin sản phẩm sale trong PromotionDetail
                    promotionDetail.get().setQuantity(0);
                    promotionDetail.get().setStatus(Status.FINISHED.toString());
                    promotionDetailRepository.save(promotionDetail.get());
                    totalMerchandise = totalMerchandise.add(
                            promotionPrice.multiply(BigDecimal.valueOf(quantityProductPromotion))
                                    .add(priceProduct.multiply(BigDecimal.valueOf(retailQuantity)))
                    );
                }
            }
            //Trường hợp sản phẩm không sale
            else {
                // Tạo hóa đơn chi tiết mới
                BillDetail billDetail = new BillDetail();
                billDetail.setProductDetail(productDetail);
                billDetail.setBill(saveBill);
                billDetail.setQuantity(request.getQuantity());
                billDetail.setStatus(Status.PENDING.toString());
                //Áp dụng giá gốc của sản phẩm
                billDetail.setPriceDiscount(productDetail.getPrice());
                billDetailRepository.save(billDetail);
                totalMerchandise = totalMerchandise.add(productDetail.getPrice().multiply(BigDecimal.valueOf(request.getQuantity())));
            }

        }

        BigDecimal priceDiscount = BigDecimal.ZERO;
        BigDecimal totalAmount = totalMerchandise.add(SHIPPING_PRICE);

        if (codeVoucher != null && !codeVoucher.isBlank()) {
            Optional<Voucher> voucherOptional = voucherRepository.findByCodeVoucher(codeVoucher);
            if (voucherOptional.isPresent()) {
                voucher = voucherOptional.get();

                if (voucher.getQuantity() <= 0) {
                    throw new RuntimeException("Đã hết phiếu giảm giá");
                }
                if (totalMerchandise.compareTo(voucher.getMinBillValue()) < 0) {
                    throw new RuntimeException("Hóa đơn không đủ điều kiện sử dụng phiếu giảm giá");
                }
                BigDecimal priceSale = totalMerchandise.multiply(BigDecimal.valueOf(voucher.getValue() / 100.0))
                        .setScale(0, RoundingMode.DOWN);
                BigDecimal maximumDiscount = voucher.getMaximumDiscount().max(BigDecimal.ZERO);

                priceDiscount = priceSale.compareTo(maximumDiscount) <= 0 ? priceSale : maximumDiscount;
                totalAmount = totalMerchandise.subtract(priceDiscount).setScale(0, RoundingMode.DOWN);
                if (voucher.getIsPrivate()) {
                    if (idAccount == null) {
                        throw new RuntimeException("Bạn không đủ điều kiện sử dụng voucher: " + voucher.getCodeVoucher());
                    }
                    Optional<AccountVoucher> accountVoucherOptional = accountVoucherRepository
                            .findAccountVoucherByIdAccountAndidVoucher(idAccount, voucher.getId());
                    if (accountVoucherOptional.isEmpty()) {
                        throw new RuntimeException("Bạn không đủ điều kiện sử dụng voucher: " + voucher.getCodeVoucher());
                    }
                    accountVoucher = accountVoucherOptional.get();
                    accountVoucher.setStatus(Status.INACTIVE.toString());
                    checkAccountVoucher = true;
                }
                int newQuantityVoucher = voucher.getQuantity() - 1;
                voucher.setQuantity(newQuantityVoucher);
                if (newQuantityVoucher <= 0) {
                    voucher.setStatus(Status.EXPIRED.toString());
                }
                checkVoucher = true;
                bill.setVoucher(voucher);
            }
        }
        bill.setTotalMerchandise(totalMerchandise.setScale(0, RoundingMode.DOWN));
        bill.setPriceDiscount(priceDiscount);
        bill.setTotalAmount(totalAmount.setScale(0, RoundingMode.DOWN));
        if (totalAmount.compareTo(limit) > 0) {
            throw new RuntimeException("Tổng giá trị hàng hóa vượt quá giới hạn cho phép (100,000,000).");
        }
        //Cập nhật lại Bill
        Bill updateBill = billRepository.save(saveBill);
        // Thực hiện thêm hóa đơn chi tiết

        PayBillRequest payBillRequest = PayBillRequest.builder()
                .amount(totalAmount)
                .codeBill(saveBill.getCodeBill())
                .type(2)
                .build();
        PayBillService.createPayBill(payBillRequest, 2, Status.WAITING_FOR_PAYMENT.toString());
        if (checkVoucher) {
            voucherRepository.save(voucher);
        }
        if (checkAccountVoucher) {
            accountVoucherRepository.save(accountVoucher);
        }
        notificationController.sendNotification();
    }
    public BigDecimal calculateTotalCartPriceForSelected(List<CartDetailProductDetailResponse> cartDetails) {
        BigDecimal total = BigDecimal.ZERO;

        for (CartDetailProductDetailResponse detail : cartDetails) {
            total = total.add(detail.calculatePricePerProductDetail());
        }

        return total;
    }

}
