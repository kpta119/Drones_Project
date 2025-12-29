package com.example.drones.admin;

import com.example.drones.admin.dto.UserDto;
import com.example.drones.user.UserRole;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
class AdminController {
    private final AdminService adminService;

    @GetMapping("/getUsers")
    public ResponseEntity<Page<UserDto>> getUsers(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) UserRole role,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        Page<UserDto> response = adminService.getUsers(query, role, pageable);
        return ResponseEntity.ok().body(response);
    }

    @PatchMapping("/banUser/{userId}")
    public ResponseEntity<UserDto> banUser(@PathVariable UUID userId) {
        UserDto bannedUser = adminService.banUser(userId);
        return ResponseEntity.ok().body(bannedUser);
    }
}
