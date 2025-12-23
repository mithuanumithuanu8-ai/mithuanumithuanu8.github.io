package com.hotelbooking.controller;

import com.hotelbooking.entity.Hotel;
import com.hotelbooking.repository.HotelRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.validation.BindingResult;
import jakarta.validation.Valid;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/hotels")
@CrossOrigin(origins = "*")
public class HotelController {

    private final HotelRepository hotelRepository;

    public HotelController(HotelRepository hotelRepository){
        this.hotelRepository = hotelRepository;
    }

    @GetMapping
    public List<Hotel> getAllHotels(){
        return hotelRepository.findAll();
    }

    @GetMapping("/search")
    public List<Hotel> searchByDistrict(@RequestParam String district){
        return hotelRepository.findByDistrictContainingIgnoreCase(district);
    }

    @PostMapping
    public ResponseEntity<?> addHotel(@Valid @RequestBody Hotel hotel, BindingResult result){
        if(result.hasErrors()){
            return ResponseEntity.badRequest().body(result.getAllErrors().stream()
                .map(error -> error.getDefaultMessage()).collect(Collectors.toList()));
        }
        try {
            Hotel savedHotel = hotelRepository.save(hotel);
            return ResponseEntity.ok(savedHotel);
        } catch(Exception e) {
            return ResponseEntity.badRequest().body("Failed to save hotel");
        }
    }

    @PutMapping("/{id}")
    public Hotel updateHotel(@PathVariable Long id, @RequestBody Hotel hotel){
        hotel.setHotelId(id);
        return hotelRepository.save(hotel);
    }

    @DeleteMapping("/{id}")
    public void deleteHotel(@PathVariable Long id){
        hotelRepository.deleteById(id);
    }
}
