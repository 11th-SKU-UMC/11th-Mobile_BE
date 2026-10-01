package com.umc.study.controller;

import com.umc.study.service.RentalService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/rentals")
@RequiredArgsConstructor
public class RentalController {

    private final RentalService rentalService;

    // POST /rentals  { "userId": 1, "bookId": 2 }
    @PostMapping
    public ResponseEntity<String> createRental(@RequestBody Map<String, Object> body) {
        rentalService.createRental(body);
        return ResponseEntity.status(HttpStatus.CREATED).body("대여 기록이 생성되었습니다!");
    }

    // PATCH /rentals/{rentalId}/return
    @PatchMapping("/{rentalId}/return")
    public ResponseEntity<String> returnBook(@PathVariable Long rentalId) {
        if (!rentalService.returnBook(rentalId)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("존재하지 않거나 이미 반납된 대여 기록입니다.");
        }
        return ResponseEntity.ok("반납 처리가 완료되었습니다!");
    }
}
