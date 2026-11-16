package com.umc.study.service;

import com.umc.study.dto.BookResponse;
import com.umc.study.dto.CreateBookRequest;
import com.umc.study.entity.Book;
import com.umc.study.entity.Category;
import com.umc.study.repository.BookJpaRepository;
import com.umc.study.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BookService {

    private final BookJpaRepository bookRepository;
    private final CategoryRepository categoryRepository;

    public List<BookResponse> getAllBooks() {
        return bookRepository.findAllByOrderByBookIdDesc().stream()
                .map(BookResponse::from)
                .toList();
    }

    public List<BookResponse> getBooksByCategoryId(Long categoryId) {
        return bookRepository
                .findByCategory_CategoryIdOrderByBookIdDesc(categoryId)
                .stream()
                .map(BookResponse::from)
                .toList();
    }

    @Transactional
    public BookResponse createBook(CreateBookRequest request) {
        Category category = categoryRepository
                .findById(request.categoryId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "존재하지 않는 카테고리입니다."
                ));

        Book book = new Book(
                category,
                request.title(),
                request.description()
        );

        return BookResponse.from(bookRepository.save(book));
    }
}