package com.is.clientaccountapi.model.account;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TransferRequest {
    private Long fromAccountId;
    private Long toAccountId;
    private Long amount;
    private String purpose;
}
