package com.devops.api.pqr.reviewer;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/reviewer") // This defines the base path
public class ReviewerController {
    private final ReviewerService reviewerService;

    public ReviewerController(ReviewerService reviewerService) {
        this.reviewerService = reviewerService;
    }
    @PostMapping
    public Reviewer save(@RequestBody Reviewer data){
        System.out.println(data);
        return data;
    }

    @GetMapping
    public ResponseEntity<String> get(){
        return ResponseEntity.ok("hola");
    }
}
