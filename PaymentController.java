package com.hotelbooking.controller;

import com.hotelbooking.service.BookingService;
import org.springframework.util.DigestUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import java.util.Map;

@RestController
@RequestMapping("/api/payhere")
public class PaymentController {

    private final BookingService bookingService;
    private final String merchantSecret = "YOUR_MERCHANT_SECRET";

    public PaymentController(BookingService bookingService){
        this.bookingService = bookingService;
    }

    @PostMapping("/notify")
    public ResponseEntity<String> payhereNotify(@RequestParam Map<String,String> params){
        String merchantId = params.get("merchant_id");
        String orderId = params.get("order_id");
        String payhereAmount = params.get("payhere_amount");
        String currency = params.get("payhere_currency");
        String statusCode = params.get("status_code");
        String md5sig = params.get("md5sig");
        String paymentId = params.get("payment_id");

        // MD5 verification
        String localSig = DigestUtils.md5DigestAsHex(
                (merchantId + orderId + payhereAmount + currency + statusCode + DigestUtils.md5DigestAsHex(merchantSecret.getBytes()).toUpperCase())
                        .getBytes()).toUpperCase();

        if(!localSig.equalsIgnoreCase(md5sig)){
            return ResponseEntity.badRequest().body("INVALID SIGNATURE");
        }

        int code = Integer.parseInt(statusCode);
        if(code == 2){
            bookingService.markBookingPaid(orderId, paymentId);
        } else if(code == 0){
            bookingService.markBookingPending(orderId);
        } else {
            bookingService.markBookingFailed(orderId);
        }

        return ResponseEntity.ok("OK");
    }
}
