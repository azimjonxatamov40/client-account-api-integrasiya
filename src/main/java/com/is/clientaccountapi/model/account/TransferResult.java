package com.is.clientaccountapi.model.account;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TransferResult {
    private Long amount;
    private AccountInfo fromAccount;
    private AccountInfo toAccount;
}
