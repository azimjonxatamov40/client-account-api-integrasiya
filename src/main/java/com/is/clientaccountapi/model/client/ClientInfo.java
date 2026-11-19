package com.is.clientaccountapi.model.client;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ClientInfo {
    private Long id;
    private String pinfl;
    private String firstName;
    private String lastName;
    private String patronymic;
    private String birthDate;
    private String passport;
    private String phone;
    private Integer state;
    private String createdAt;
}
