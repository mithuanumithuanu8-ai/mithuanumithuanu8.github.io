package com.hotelbooking.controller;

import com.hotelbooking.dto.UserResponse;
import com.hotelbooking.entity.User;
import com.hotelbooking.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.validation.BindingResult;
import org.springframework.transaction.annotation.Transactional;
import jakarta.validation.Valid;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "*")
public class UserController {

    private final UserRepository userRepository;

    public UserController(UserRepository userRepository){
        this.userRepository = userRepository;
    }

    @PostMapping
    public User createUser(@RequestBody User user){
        return userRepository.save(user);
    }

    @PostMapping("/create-admin")
    public ResponseEntity<UserResponse> createAdmin(){
        // Check if admin already exists
        if(userRepository.findByEmail("admin@hotel.com").isPresent()){
            return ResponseEntity.badRequest().build();
        }
        
        User admin = new User();
        admin.setName("System Admin");
        admin.setEmail("admin@hotel.com");
        admin.setPhone("+94771234567");
        admin.setPassword("admin123");
        admin.setRole("ADMIN");
        admin.setStatus("ACTIVE");
        
        User savedAdmin = userRepository.save(admin);
        UserResponse response = new UserResponse(savedAdmin.getUserId(), savedAdmin.getName(), 
            savedAdmin.getEmail(), savedAdmin.getPhone(), savedAdmin.getRole(), savedAdmin.getStatus());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/fix-admin/{userId}")
    public ResponseEntity<UserResponse> fixAdminRole(@PathVariable Long userId){
        User user = userRepository.findById(userId).orElse(null);
        if(user == null) {
            return ResponseEntity.notFound().build();
        }
        
        System.out.println("Fixing admin role for user: " + user.getName() + ", current role: " + user.getRole());
        user.setRole("ADMIN");
        user.setStatus("ACTIVE");
        
        User savedUser = userRepository.save(user);
        System.out.println("User role updated to: " + savedUser.getRole());
        
        UserResponse response = new UserResponse(savedUser.getUserId(), savedUser.getName(), 
            savedUser.getEmail(), savedUser.getPhone(), savedUser.getRole(), savedUser.getStatus());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody User user, BindingResult result){
        if(result.hasErrors()){
            return ResponseEntity.badRequest().body(result.getAllErrors().stream()
                .map(error -> error.getDefaultMessage()).collect(Collectors.toList()));
        }
        if(userRepository.existsByEmail(user.getEmail())){
            return ResponseEntity.badRequest().body("Email already exists");
        }
        user.setRole("USER");
        user.setStatus("ACTIVE");
        User savedUser = userRepository.save(user);
        UserResponse response = new UserResponse(savedUser.getUserId(), savedUser.getName(), 
            savedUser.getEmail(), savedUser.getPhone(), savedUser.getRole(), savedUser.getStatus());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/login")
    public ResponseEntity<UserResponse> login(@RequestParam String email, @RequestParam String password){
        System.out.println("Login attempt - Email: " + email + ", Password: " + password);
        
        User user = userRepository.findByEmail(email).orElse(null);
        if(user == null) {
            System.out.println("User not found with email: " + email);
            return ResponseEntity.badRequest().build();
        }
        
        System.out.println("User found - Name: " + user.getName() + ", Role: " + user.getRole() + ", Stored Password: " + user.getPassword());
        
        if(!user.getPassword().equals(password)){
            System.out.println("Password mismatch - Provided: " + password + ", Stored: " + user.getPassword());
            return ResponseEntity.badRequest().build();
        }
        
        if("BLOCKED".equals(user.getStatus())){
            System.out.println("User is blocked");
            return ResponseEntity.status(403).build();
        }
        
        System.out.println("Login successful for user: " + user.getName());
        UserResponse response = new UserResponse(user.getUserId(), user.getName(), 
            user.getEmail(), user.getPhone(), user.getRole(), user.getStatus());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/test")
    public String test(){
        return "API is working!";
    }

    @GetMapping
    public List<UserResponse> getAllUsers(){
        return userRepository.findAll().stream()
            .map(user -> new UserResponse(user.getUserId(), user.getName(), 
                user.getEmail(), user.getPhone(), user.getRole(), user.getStatus()))
            .collect(Collectors.toList());
    }

    @PutMapping("/{id}/role")
    @Transactional
    public ResponseEntity<UserResponse> updateUserRole(@PathVariable Long id, @RequestParam String role, @RequestParam Long adminId){
        try {
            System.out.println("Role update request - User ID: " + id + ", New Role: " + role + ", Admin ID: " + adminId);
            
            User admin = userRepository.findById(adminId).orElse(null);
            if(admin == null) {
                System.out.println("Admin not found with ID: " + adminId);
                return ResponseEntity.status(403).build();
            }
            
            System.out.println("Admin found - Name: " + admin.getName() + ", Role: " + admin.getRole());
            
            if(!"ADMIN".equals(admin.getRole()) && !"SUB_ADMIN".equals(admin.getRole())){
                System.out.println("User is not ADMIN or SUB_ADMIN, role is: " + admin.getRole());
                return ResponseEntity.status(403).build();
            }
            
            // Only main ADMIN can assign ADMIN or SUB_ADMIN roles
            if(("ADMIN".equals(role) || "SUB_ADMIN".equals(role)) && !"ADMIN".equals(admin.getRole())) {
                System.out.println("Only main ADMIN can assign ADMIN or SUB_ADMIN roles");
                return ResponseEntity.status(403).build();
            }
            
            // Prevent admin from changing their own role to non-admin
            if(adminId.equals(id) && !"ADMIN".equals(role)) {
                System.out.println("Admin cannot change their own role to non-admin");
                return ResponseEntity.status(400).build();
            }
            
            User user = userRepository.findById(id).orElse(null);
            if(user == null) return ResponseEntity.notFound().build();
            
            user.setRole(role);
            User updatedUser = userRepository.save(user);
            UserResponse response = new UserResponse(updatedUser.getUserId(), updatedUser.getName(), 
                updatedUser.getEmail(), updatedUser.getPhone(), updatedUser.getRole(), updatedUser.getStatus());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.out.println("Error updating user role: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).build();
        }
    }

    @PutMapping("/{id}/status")
    @Transactional
    public ResponseEntity<UserResponse> updateUserStatus(@PathVariable Long id, @RequestParam String status, @RequestParam Long adminId){
        try {
            System.out.println("Status update request - User ID: " + id + ", New Status: " + status + ", Admin ID: " + adminId);
            
            // Validate status parameter
            if(!"ACTIVE".equals(status) && !"BLOCKED".equals(status)) {
                System.out.println("Invalid status: " + status);
                return ResponseEntity.badRequest().build();
            }
            
            User admin = userRepository.findById(adminId).orElse(null);
            if(admin == null) {
                System.out.println("Admin not found with ID: " + adminId);
                return ResponseEntity.status(403).build();
            }
            
            System.out.println("Admin found - Name: " + admin.getName() + ", Role: " + admin.getRole());
            
            if(!"ADMIN".equals(admin.getRole()) && !"SUB_ADMIN".equals(admin.getRole())){
                System.out.println("User is not ADMIN or SUB_ADMIN, role is: " + admin.getRole());
                return ResponseEntity.status(403).build();
            }
            
            User user = userRepository.findById(id).orElse(null);
            if(user == null) return ResponseEntity.notFound().build();
            
            user.setStatus(status);
            User updatedUser = userRepository.save(user);
            UserResponse response = new UserResponse(updatedUser.getUserId(), updatedUser.getName(), 
                updatedUser.getEmail(), updatedUser.getPhone(), updatedUser.getRole(), updatedUser.getStatus());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.out.println("Error updating user status: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).build();
        }
    }
}
