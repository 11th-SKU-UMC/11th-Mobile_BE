
package com.umc.study.controller;

import com.umc.study.service.RentalService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController // 1. "나는 데이터를 JSON으로 서빙하는 API 카운터야!"
@RequestMapping("/rentals") // 2. 이 컨트롤러로 들어오는 요청의 기본 주소는 /books
@RequiredArgsConstructor
public class RentalController {


    private final RentalService rentalService;


    @PostMapping
    public String createRental(@RequestBody Map<String, Object> body){
        rentalService.createRental(body);
        return "신규 도서 대여 기록 등록이 완료되었습니다!";
    }
}