package com.is.clientaccountapi.model.account;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AccountRequest {
    private String balanceType;
    private String currency;
}
