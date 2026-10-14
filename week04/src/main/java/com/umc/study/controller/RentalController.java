// src/main/java/.../controller/RentalController.java
package com.umc.study.controller;

import com.umc.study.service.RentalService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController // 1. "나는 데이터를 JSON으로 서빙하는 API 카운터야!"
@RequestMapping("/rentals") // 2. 이 컨트롤러로 들어오는 요청의 기본 주소는 /rentals
@RequiredArgsConstructor
public class RentalController {

    // 주방장(Service)을 주입받아 카운터 옆에 대기시킵니다.
    private final RentalService rentalService;

    // 3. HTTP GET 방식으로 /rentals 요청이 들어왔을 때 이 메서드가 실행됩니다.

    @PostMapping
    public void getNewRental(@RequestBody Map<String, Object> body) {
        rentalService.getNewRental(body);
    }

}
