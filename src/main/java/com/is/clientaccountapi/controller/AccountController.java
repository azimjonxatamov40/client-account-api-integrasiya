package com.is.clientaccountapi.controller;

import com.is.clientaccountapi.model.account.AccountInfo;
import com.is.clientaccountapi.model.account.AccountRequest;
import com.is.clientaccountapi.model.account.DepositRequest;
import com.is.clientaccountapi.model.account.OperationInfo;
import com.is.clientaccountapi.model.account.TransferRequest;
import com.is.clientaccountapi.model.account.TransferResult;
import com.is.clientaccountapi.model.result.ApiResponse;
import com.is.clientaccountapi.service.AccountService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class AccountController {
    private final AccountService accountService;

    @PostMapping("/clients/{clientId}/accounts")
    public ResponseEntity<ApiResponse<AccountInfo>> openAccount(@PathVariable Long clientId,
                                                                @RequestBody AccountRequest request) {
        log.info("OpenAccount start: clientId: {}", clientId);
        return accountService.openAccount(clientId, request).toResponse();
    }

    @GetMapping("/clients/{clientId}/accounts")
    public ResponseEntity<ApiResponse<List<AccountInfo>>> getClientAccounts(@PathVariable Long clientId) {
        log.info("GetClientAccounts start: clientId: {}", clientId);
        return accountService.getClientAccounts(clientId).toResponse();
    }

    @PostMapping("/accounts/{id}/deposit")
    public ResponseEntity<ApiResponse<AccountInfo>> deposit(@PathVariable Long id,
                                                            @RequestBody DepositRequest request) {
        log.info("Deposit start: id: {}, amount: {}", id, request.getAmount());
        return accountService.deposit(id, request).toResponse();
    }

    @GetMapping("/accounts/{id}/operations")
    public ResponseEntity<ApiResponse<List<OperationInfo>>> getOperations(@PathVariable Long id,
                                                                          @RequestParam(required = false) String from,
                                                                          @RequestParam(required = false) String to) {
        log.info("GetOperations start: id: {}, from: {}, to: {}", id, from, to);
        return accountService.getOperations(id, from, to).toResponse();
    }

    @PostMapping("/accounts/transfer")
    public ResponseEntity<ApiResponse<TransferResult>> transfer(@RequestBody TransferRequest request) {
        log.info("Transfer start: from: {}, to: {}, amount: {}",
                request.getFromAccountId(), request.getToAccountId(), request.getAmount());
        return accountService.transfer(request).toResponse();
    }

    @PutMapping("/accounts/{id}/close")
    public ResponseEntity<ApiResponse<AccountInfo>> closeAccount(@PathVariable Long id) {
        log.info("CloseAccount start: id: {}", id);
        return accountService.closeAccount(id).toResponse();
    }
}
