package com.is.clientaccountapi.controller;

import com.is.clientaccountapi.model.client.ClientInfo;
import com.is.clientaccountapi.model.client.ClientRequest;
import com.is.clientaccountapi.model.result.ApiResponse;
import com.is.clientaccountapi.service.ClientService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/clients")
@RequiredArgsConstructor
public class ClientController {
    private final ClientService clientService;

    @PostMapping
    public ResponseEntity<ApiResponse<ClientInfo>> createClient(@RequestBody ClientRequest request) {
        log.info("CreateClient start: pinfl: {}", request.getPinfl());
        return clientService.createClient(request).toResponse();
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ClientInfo>>> getClients(@RequestParam(required = false) Integer page,
                                                              @RequestParam(required = false) Integer size,
                                                              @RequestParam(required = false) String search) {
        log.info("GetClients start: page: {}, size: {}, search: {}", page, size, search);
        return clientService.getClients(page, size, search).toResponse();
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ClientInfo>> getClientById(@PathVariable Long id) {
        log.info("GetClientById start: id: {}", id);
        return clientService.getClientById(id).toResponse();
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ClientInfo>> updateClient(@PathVariable Long id,
                                                                @RequestBody ClientRequest request) {
        log.info("UpdateClient start: id: {}", id);
        return clientService.updateClient(id, request).toResponse();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Boolean>> deleteClient(@PathVariable Long id) {
        log.info("DeleteClient start: id: {}", id);
        return clientService.deleteClient(id).toResponse();
    }
}
