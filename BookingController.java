package com.hotelbooking.controller;

import com.hotelbooking.entity.Booking;
import com.hotelbooking.entity.Hotel;
import com.hotelbooking.entity.User;
import com.hotelbooking.repository.HotelRepository;
import com.hotelbooking.repository.UserRepository;
import com.hotelbooking.service.BookingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/bookings")
@CrossOrigin(origins = "*")
public class BookingController {

    private final BookingService bookingService;
    private final UserRepository userRepository;
    private final HotelRepository hotelRepository;

    public BookingController(BookingService bookingService, UserRepository userRepository, HotelRepository hotelRepository){
        this.bookingService = bookingService;
        this.userRepository = userRepository;
        this.hotelRepository = hotelRepository;
    }

    @GetMapping("/user/{userId}")
    public List<Booking> getUserBookings(@PathVariable Long userId){
        return bookingService.findByUserId(userId);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Booking> updateBooking(@PathVariable Long id, @RequestBody Booking booking){
        try {
            // Get the original booking to compare room counts
            Optional<Booking> originalOpt = bookingService.findById(id);
            if(originalOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            Booking original = originalOpt.get();
            int originalRooms = original.getRoomsBooked();
            int newRooms = booking.getRoomsBooked();
            
            // If room count changed, update hotel availability
            if(originalRooms != newRooms) {
                Hotel hotel = original.getHotel();
                int roomDifference = newRooms - originalRooms;
                
                // Check if hotel has enough rooms for increase
                if(roomDifference > 0 && hotel.getRoomsAvailable() < roomDifference) {
                    return ResponseEntity.badRequest().build();
                }
                
                // Update hotel room availability
                hotel.setRoomsAvailable(hotel.getRoomsAvailable() - roomDifference);
                hotelRepository.save(hotel);
            }
            
            booking.setBookingId(id);
            return ResponseEntity.ok(bookingService.saveBooking(booking));
        } catch (Exception e) {
            System.err.println("Error updating booking: " + e.getMessage());
            return ResponseEntity.status(500).build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancelBooking(@PathVariable Long id, @RequestParam(required = false) Long adminId){
        try {
            System.out.println("Deleting booking ID: " + id + ", Admin ID: " + adminId);
            
            // If adminId is provided, validate admin access
            if(adminId != null) {
                Optional<User> adminOpt = userRepository.findById(adminId);
                if(adminOpt.isEmpty()) {
                    System.out.println("Admin not found with ID: " + adminId);
                    return ResponseEntity.status(403).build();
                }
                String role = adminOpt.get().getRole();
                if(!"ADMIN".equals(role) && !"SUB_ADMIN".equals(role)) {
                    System.out.println("User is not admin, role: " + role);
                    return ResponseEntity.status(403).build();
                }
                System.out.println("Admin validation passed for role: " + role);
            }
            
            // Get booking details before deletion to restore room availability
            Optional<Booking> bookingOpt = bookingService.findById(id);
            if(bookingOpt.isEmpty()) {
                System.out.println("Booking not found with ID: " + id);
                return ResponseEntity.notFound().build();
            }
            
            Booking booking = bookingOpt.get();
            System.out.println("Found booking: " + booking.getBookingId() + ", rooms: " + booking.getRoomsBooked());
            
            Hotel hotel = booking.getHotel();
            if(hotel != null) {
                System.out.println("Restoring " + booking.getRoomsBooked() + " rooms to hotel: " + hotel.getHotelName());
                
                // Get fresh hotel data to avoid validation issues
                Optional<Hotel> freshHotelOpt = hotelRepository.findById(hotel.getHotelId());
                if(freshHotelOpt.isPresent()) {
                    Hotel freshHotel = freshHotelOpt.get();
                    freshHotel.setRoomsAvailable(freshHotel.getRoomsAvailable() + booking.getRoomsBooked());
                    hotelRepository.save(freshHotel);
                    System.out.println("Hotel rooms updated successfully");
                } else {
                    System.out.println("Warning: Could not find fresh hotel data");
                }
            } else {
                System.out.println("Warning: Booking has no associated hotel");
            }
            
            bookingService.deleteBooking(id);
            System.out.println("Booking deleted successfully");
            return ResponseEntity.ok().build();
            
        } catch (Exception e) {
            System.err.println("Error deleting booking: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).build();
        }
    }

    @GetMapping("/admin/all")
    public List<Booking> getAllBookings(@RequestParam Long adminId){
        Optional<User> userOpt = userRepository.findById(adminId);
        if(userOpt.isEmpty()) {
            System.out.println("Admin not found for getAllBookings: " + adminId);
            return List.of();
        }
        String role = userOpt.get().getRole();
        if(!"ADMIN".equals(role) && !"SUB_ADMIN".equals(role)){
            System.out.println("Insufficient permissions for getAllBookings, role: " + role);
            return List.of();
        }
        System.out.println("Loading all bookings for admin: " + adminId + ", role: " + role);
        return bookingService.getAllBookings();
    }

    @PutMapping("/admin/{id}/status")
    public ResponseEntity<Booking> updateBookingStatus(@RequestParam Long adminId, @PathVariable Long id, @RequestParam String status){
        Optional<User> userOpt = userRepository.findById(adminId);
        if(userOpt.isEmpty() || !"ADMIN".equals(userOpt.get().getRole())){
            return ResponseEntity.status(403).build();
        }
        
        Optional<Booking> bookingOpt = bookingService.findById(id);
        if(bookingOpt.isEmpty()) return ResponseEntity.notFound().build();
        
        Booking booking = bookingOpt.get();
        booking.setStatus(status);
        return ResponseEntity.ok(bookingService.saveBooking(booking));
    }

    @PostMapping("/create")
    public ResponseEntity<Booking> createBooking(@RequestParam Long userId,
                                 @RequestParam Long hotelId,
                                 @RequestParam String checkIn,
                                 @RequestParam String checkOut,
                                 @RequestParam int roomsBooked,
                                 @RequestParam(defaultValue = "USD") String currency) {
        Optional<User> userOpt = userRepository.findById(userId);
        Optional<Hotel> hotelOpt = hotelRepository.findById(hotelId);

        if(userOpt.isEmpty() || hotelOpt.isEmpty()){
            return ResponseEntity.badRequest().build();
        }

        User user = userOpt.get();
        Hotel hotel = hotelOpt.get();

        // Check room availability
        if(hotel.getRoomsAvailable() < roomsBooked){
            return ResponseEntity.badRequest().build();
        }

        LocalDate inDate = LocalDate.parse(checkIn);
        LocalDate outDate = LocalDate.parse(checkOut);
        long nights = Math.max(1, java.time.temporal.ChronoUnit.DAYS.between(inDate, outDate));

        BigDecimal baseAmount = BigDecimal.valueOf(hotel.getPricePerNight()).multiply(BigDecimal.valueOf(nights * roomsBooked));
        BigDecimal totalAmount = convertCurrency(baseAmount, "USD", currency);

        // Reduce room availability
        hotel.setRoomsAvailable(hotel.getRoomsAvailable() - roomsBooked);
        hotelRepository.save(hotel);

        Booking booking = new Booking();
        booking.setOrderId(UUID.randomUUID().toString());
        booking.setUser(user);
        booking.setHotel(hotel);
        booking.setCheckIn(inDate);
        booking.setCheckOut(outDate);
        booking.setRoomsBooked(roomsBooked);
        booking.setTotalAmount(totalAmount);
        booking.setCurrency(currency);
        booking.setStatus("pending");

        Booking savedBooking = bookingService.saveBooking(booking);
        return ResponseEntity.ok(savedBooking);
    }
    
    private BigDecimal convertCurrency(BigDecimal amount, String fromCurrency, String toCurrency) {
        if (fromCurrency.equals(toCurrency)) {
            return amount;
        }
        
        // Simple conversion rate: 1 USD = 320 LKR (you can make this dynamic)
        BigDecimal usdToLkrRate = BigDecimal.valueOf(320);
        
        if ("USD".equals(fromCurrency) && "LKR".equals(toCurrency)) {
            return amount.multiply(usdToLkrRate);
        } else if ("LKR".equals(fromCurrency) && "USD".equals(toCurrency)) {
            return amount.divide(usdToLkrRate, 2, BigDecimal.ROUND_HALF_UP);
        }
        
        return amount;
    }
}

