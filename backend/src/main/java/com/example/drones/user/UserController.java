package com.example.drones.user;

import com.example.drones.common.config.auth.JwtService;
import com.example.drones.user.dto.UserResponse;
import com.example.drones.user.dto.UserUpdateRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final JwtService jwtService;

    @GetMapping("/getUserData")
    public ResponseEntity<UserResponse> getUserData(
            @RequestParam(name = "user_id", required = false) UUID userId
            ){
        UUID targetId = (userId != null) ? userId : jwtService.extractUserId();
        UserResponse response = userService.getUserData(targetId);
        return  ResponseEntity.ok(response);
    }

    @PatchMapping("/editUserData")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public UserResponse editUserData(@RequestBody UserUpdateRequest request){
        return userService.editUserData(request);
    }
}
