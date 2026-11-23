package com.is.clientaccountapi.controller;

import com.is.clientaccountapi.model.account.AccountInfo;
import com.is.clientaccountapi.model.account.AccountRequest;
import com.is.clientaccountapi.model.account.DepositRequest;
import com.is.clientaccountapi.model.account.OperationInfo;
import com.is.clientaccountapi.model.account.TransferRequest;
import com.is.clientaccountapi.model.account.TransferResult;
import com.is.clientaccountapi.model.client.ClientInfo;
import com.is.clientaccountapi.model.client.ClientRequest;
import com.is.clientaccountapi.model.result.ApiResponse;
import com.is.clientaccountapi.service.AccountService;
import com.is.clientaccountapi.service.ClientService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ClientAccountController {
    private final ClientService clientService;
    private final AccountService accountService;

    @PostMapping("/clients")
    public ResponseEntity<ApiResponse<ClientInfo>> createClient(@RequestBody ClientRequest request) {
        log.info("CreateClient start: pinfl: {}", request.getPinfl());
        return clientService.createClient(request).toResponse();
    }

    @GetMapping("/clients")
    public ResponseEntity<ApiResponse<List<ClientInfo>>> getClients(@RequestParam(required = false) Integer page,
                                                                    @RequestParam(required = false) Integer size,
                                                                    @RequestParam(required = false) String search) {
        log.info("GetClients start: page: {}, size: {}, search: {}", page, size, search);
        return clientService.getClients(page, size, search).toResponse();
    }

    @GetMapping("/clients/{id}")
    public ResponseEntity<ApiResponse<ClientInfo>> getClientById(@PathVariable Long id) {
        log.info("GetClientById start: id: {}", id);
        return clientService.getClientById(id).toResponse();
    }

    @PutMapping("/clients/{id}")
    public ResponseEntity<ApiResponse<ClientInfo>> updateClient(@PathVariable Long id,
                                                                @RequestBody ClientRequest request) {
        log.info("UpdateClient start: id: {}", id);
        return clientService.updateClient(id, request).toResponse();
    }

    @DeleteMapping("/clients/{id}")
    public ResponseEntity<ApiResponse<Boolean>> deleteClient(@PathVariable Long id) {
        log.info("DeleteClient start: id: {}", id);
        return clientService.deleteClient(id).toResponse();
    }

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

    @GetMapping("/accounts/{id}")
    public ResponseEntity<ApiResponse<AccountInfo>> getAccountById(@PathVariable Long id) {
        log.info("GetAccountById start: id: {}", id);
        return accountService.getAccountById(id).toResponse();
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
