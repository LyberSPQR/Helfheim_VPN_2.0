package com.sloptech.helfheim.controller;

import com.sloptech.helfheim.dto.UserCreateRequestDto;
import com.sloptech.helfheim.dto.UserResponseDto;
import com.sloptech.helfheim.dto.UserUpdateRequestDto;
import com.sloptech.helfheim.service.CoreService;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/v1/core")
@AllArgsConstructor
public class CoreController {
    CoreService coreService;


    @PostMapping("/add")
    public ResponseEntity<UserResponseDto> addUser(@RequestBody UserCreateRequestDto dto) {
        return new ResponseEntity<>(UserResponseDto.from(coreService.saveUser(dto)), HttpStatus.CREATED);
    }
    @PostMapping("/activate")
    public ResponseEntity<Void> activateSubscription(@RequestBody UserUpdateRequestDto updateDto) throws IOException, InterruptedException {
        coreService.activateSubscription(updateDto);
        return new ResponseEntity<Void>(HttpStatus.CREATED);
    }
    @PutMapping
    public ResponseEntity<UserResponseDto> updateUser(@RequestBody UserUpdateRequestDto user) {
        return new ResponseEntity<>(UserResponseDto.from(coreService.updateUser(user)), HttpStatus.OK);
    }
    @GetMapping("/download/{email}")
    public ResponseEntity<byte[]> downloadUserConfig(@PathVariable String email){

        byte[] userConfigInBytes = coreService.generateConfig(email).getBytes(StandardCharsets.UTF_8);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDispositionFormData("attachment", email + ".conf");

        return new ResponseEntity<>(userConfigInBytes,headers,HttpStatus.OK);
    }

}
