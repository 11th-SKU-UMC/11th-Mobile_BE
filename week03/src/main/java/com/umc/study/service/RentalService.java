package com.umc.study.service;

import com.umc.study.repository.RentalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class RentalService {

    private final RentalRepository rentalRepository;

    public void createRental(Map<String, Object> body) {
        rentalRepository.save(body);
    }

    // 갱신된 행이 있으면 true, 없거나 이미 반납된 기록이면 false
    public boolean returnBook(Long rentalId) {
        return rentalRepository.markReturned(rentalId) > 0;
    }
}
