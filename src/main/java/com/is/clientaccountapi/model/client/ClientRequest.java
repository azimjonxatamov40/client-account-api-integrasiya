package com.is.clientaccountapi.model.client;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ClientRequest {
    private String pinfl;
    private String firstName;
    private String lastName;
    private String patronymic;
    private String birthDate;
    private String passport;
    private String phone;
}
