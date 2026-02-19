package com.sloptech.helfheim.controller;

import com.sloptech.helfheim.dto.*;
import com.sloptech.helfheim.service.CoreService;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/v1/core")
@AllArgsConstructor
@Validated
public class CoreController {
    CoreService coreService;

    @PostMapping("/add")
    public ResponseEntity<UserResponseDto> addUser(@Valid @RequestBody UserCreateRequestDto dto) {
        return new ResponseEntity<>(UserResponseDto.from(coreService.saveUser(dto)), HttpStatus.CREATED);
    }
    @PostMapping("/login")
    public ResponseEntity<LoginResponseDto> loginUser(@RequestBody LoginRequestDto dto) throws IOException {
        return new ResponseEntity<>(coreService.generateConfigForFrontend(dto), HttpStatus.OK);
    }
    @PostMapping("/activate")
    public ResponseEntity<Void> activateSubscription(@Valid @RequestBody UserUpdateRequestDto updateDto) throws IOException, InterruptedException {
        coreService.activateSubscription(updateDto);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @PutMapping
    public ResponseEntity<UserResponseDto> updateUser(@Valid @RequestBody UserUpdateRequestDto user) throws IOException, InterruptedException {
        return new ResponseEntity<>(UserResponseDto.from(coreService.updateUser(user)), HttpStatus.OK);
    }

    @GetMapping("/download/{email}")
    public ResponseEntity<byte[]> downloadUserConfig(@PathVariable @NotBlank @Email String email) {

        byte[] userConfigInBytes = coreService.generateConfig(email).getBytes(StandardCharsets.UTF_8);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDispositionFormData("attachment", "helfheim.conf");

        return new ResponseEntity<>(userConfigInBytes,headers,HttpStatus.OK);
    }

}
