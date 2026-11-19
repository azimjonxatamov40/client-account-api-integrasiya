package com.is.clientaccountapi.model.account;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AccountInfo {
    private Long id;
    private Long clientId;
    private String accountNumber;
    private String balanceType;
    private String currency;
    private Long balance;
    private Integer state;
    private String dateOpen;
    private String dateClose;
}
