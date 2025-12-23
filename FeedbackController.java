package com.hotelbooking.controller;

import com.hotelbooking.entity.Feedback;
import com.hotelbooking.repository.FeedbackRepository;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/feedback")
@CrossOrigin(origins = "*")
public class FeedbackController {

    private final FeedbackRepository feedbackRepository;

    public FeedbackController(FeedbackRepository feedbackRepository){
        this.feedbackRepository = feedbackRepository;
    }

    @PostMapping
    public Feedback submitFeedback(@RequestBody Feedback feedback){
        return feedbackRepository.save(feedback);
    }
}
