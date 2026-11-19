package com.is.clientaccountapi.model.account;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DepositRequest {
    private Long amount;
    private String purpose;
}
