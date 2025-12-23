package com.hotelbooking.controller;

import com.hotelbooking.entity.Offer;
import com.hotelbooking.entity.User;
import com.hotelbooking.repository.UserRepository;
import com.hotelbooking.service.OfferService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/offers")
@CrossOrigin(origins = "*")
public class OfferController {
    
    private final OfferService offerService;
    private final UserRepository userRepository;
    
    public OfferController(OfferService offerService, UserRepository userRepository) {
        this.offerService = offerService;
        this.userRepository = userRepository;
    }
    
    @PostMapping
    public ResponseEntity<Offer> createOffer(@RequestParam Long adminId, @RequestBody Offer offer) {
        Optional<User> userOpt = userRepository.findById(adminId);
        if (userOpt.isEmpty() || !"ADMIN".equals(userOpt.get().getRole())) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(offerService.saveOffer(offer));
    }
    
    @GetMapping("/active")
    public List<Offer> getActiveOffers() {
        return offerService.getActiveOffers();
    }
    
    @GetMapping("/hotel/{hotelId}")
    public List<Offer> getHotelOffers(@PathVariable Long hotelId) {
        return offerService.getActiveOffersForHotel(hotelId);
    }
    
    @DeleteMapping("/{offerId}")
    public ResponseEntity<Void> deleteOffer(@RequestParam Long adminId, @PathVariable Long offerId) {
        Optional<User> userOpt = userRepository.findById(adminId);
        if (userOpt.isEmpty() || !"ADMIN".equals(userOpt.get().getRole())) {
            return ResponseEntity.status(403).build();
        }
        offerService.deleteOffer(offerId);
        return ResponseEntity.ok().build();
    }
}