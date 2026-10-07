
package com.umc.study.service;

import com.umc.study.repository.RentalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service // 비즈니스 로직을 수행하는 메인 셰프 계층
@RequiredArgsConstructor
public class RentalService {

// 창고지기(Repository)를 생성자 주입으로 데려옵니다.
    private final RentalRepository rentalRepository;

    public void createRental(Map<String, Object> body){
        rentalRepository.save(body);
    }
}