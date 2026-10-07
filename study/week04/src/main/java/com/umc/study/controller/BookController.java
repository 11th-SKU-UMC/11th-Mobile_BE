// src/main/java/.../controller/BookController.java
package com.umc.study.controller;

import com.umc.study.Entity.Book;
import com.umc.study.Entity.Category;
import com.umc.study.dto.BookResponse;
import com.umc.study.dto.CreateBookRequest;
import com.umc.study.service.BookService;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController // 1. "나는 데이터를 JSON으로 서빙하는 API 카운터야!"
@RequestMapping("/books") // 2. 이 컨트롤러로 들어오는 요청의 기본 주소는 /books
@RequiredArgsConstructor
public class BookController {

    // 주방장(Service)을 주입받아 카운터 옆에 대기시킵니다.
    private final BookService bookService;

    // 3. HTTP GET 방식으로 /books 요청이 들어왔을 때 이 메서드가 실행됩니다.
    @GetMapping
    public List<BookResponse> getBooks() {
        return bookService.getBooks();
    }


@PostMapping
@ResponseStatus(HttpStatus.CREATED)          // 성공 시 201 Created
public BookResponse createBook(@Valid @RequestBody CreateBookRequest request) {
    return bookService.createBook(request);
}

//    @GetMapping("/category/{categoryId}")
//    public List<Map<String, Object>> getBooksByCategory(@PathVariable Long categoryId) {
//        return bookService.getBooksByCategory(categoryId);
//    }
}















