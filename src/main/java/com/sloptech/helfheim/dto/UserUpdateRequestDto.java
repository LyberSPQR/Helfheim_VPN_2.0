package com.sloptech.helfheim.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UserUpdateRequestDto {

    @NotBlank(message = "Email не может быть пустым")
    @Email(message = "Некорректный формат email")
    private String email;

    @NotNull(message = "Срок подписки не указан")
//    @Min(value = 1, message = "Срок подписки должен быть не менее 1 дня")
    @Max(value = 365, message = "Срок подписки не может превышать 365 дней")
    private Integer subscriptionTimeInDays;
}
