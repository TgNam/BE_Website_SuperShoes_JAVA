package org.example.datn_website_supershoes.webconfig;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

@RestController
public class NotificationController {

    private final Sinks.Many<String> sink = Sinks.many().multicast().onBackpressureBuffer();

    @GetMapping(value = "/sse/notifications", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> streamNotifications() {
        return sink.asFlux(); // Trả về Flux để phát dữ liệu
    }

    public void sendNotification() {
        sink.tryEmitNext("UPDATE_CART");
        sink.tryEmitNext("UPDATE_PAYMENT");
        sink.tryEmitNext("UPDATE_PROMOTION");
//        sink.tryEmitNext("UPDATE_VOUCHER");
    }
    public void sendEvent(String messages){
        sink.tryEmitNext(messages);
    }
}

