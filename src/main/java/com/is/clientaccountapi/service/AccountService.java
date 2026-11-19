package com.is.clientaccountapi.service;

import com.is.clientaccountapi.component.CheckRequest;
import com.is.clientaccountapi.model.account.AccountInfo;
import com.is.clientaccountapi.model.account.AccountRequest;
import com.is.clientaccountapi.model.account.DepositRequest;
import com.is.clientaccountapi.model.account.OperationInfo;
import com.is.clientaccountapi.model.account.TransferRequest;
import com.is.clientaccountapi.model.account.TransferResult;
import com.is.clientaccountapi.model.result.ApiResponse;
import com.is.clientaccountapi.model.result.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

import java.sql.Date;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountService {
    private final JdbcTemplate jdbcTemplate;
    private final CheckRequest checkRequest;
    private final ClientService clientService;

    private static final List<String> CURRENCIES = Arrays.asList("860", "840", "978", "643");

    private static final int STATE_OPEN = 2;
    private static final int STATE_CLOSED = 3;

    private static final String ACCOUNT_SELECT = "select a.id, \n" +
            "       a.client_id, \n" +
            "       a.account_number, \n" +
            "       a.balance_type, \n" +
            "       a.currency, \n" +
            "       a.balance, \n" +
            "       a.state, \n" +
            "       a.date_open, \n" +
            "       a.date_close \n" +
            "  from cm_account a \n";

    private static final String OPERATION_INSERT = "insert into cm_operation \n" +
            "  (id, account_id, amount, purpose) \n" +
            "values (cm_operation_seq.nextval, ?, ?, ?)";

    private static final RowMapper<AccountInfo> ACCOUNT_MAPPER = (rs, rowNum) -> {
        Date dateOpen = rs.getDate("date_open");
        Date dateClose = rs.getDate("date_close");
        return AccountInfo.builder()
                .id(rs.getLong("id"))
                .clientId(rs.getLong("client_id"))
                .accountNumber(rs.getString("account_number"))
                .balanceType(rs.getString("balance_type"))
                .currency(rs.getString("currency"))
                .balance(rs.getLong("balance"))
                .state(rs.getInt("state"))
                .dateOpen(dateOpen == null ? null : dateOpen.toString())
                .dateClose(dateClose == null ? null : dateClose.toString())
                .build();
    };

    private static final RowMapper<OperationInfo> OPERATION_MAPPER = (rs, rowNum) -> OperationInfo.builder()
            .id(rs.getLong("id"))
            .accountId(rs.getLong("account_id"))
            .amount(rs.getLong("amount"))
            .purpose(rs.getString("purpose"))
            .createdAt(rs.getTimestamp("created_at").toLocalDateTime().format(CheckRequest.DATE_TIME))
            .build();

    public ApiResponse<AccountInfo> openAccount(Long clientId, AccountRequest request) {
        List<String> fields = new ArrayList<>();
        if (checkRequest.isEmpty(request.getBalanceType())) {
            fields.add("balanceType");
        }
        if (checkRequest.isEmpty(request.getCurrency())) {
            fields.add("currency");
        }
        if (!fields.isEmpty()) {
            return ApiResponse.error(ErrorCode.EMPTY_PARAM, fields);
        }

        if (!request.getBalanceType().matches("\\d{5}")) {
            fields.add("balanceType");
        }
        if (!CURRENCIES.contains(request.getCurrency())) {
            fields.add("currency");
        }
        if (!fields.isEmpty()) {
            return ApiResponse.error(ErrorCode.INVALID_PARAM, fields);
        }

        try {
            if (clientService.getActiveClient(clientId) == null) {
                return ApiResponse.error(ErrorCode.CLIENT_NOT_FOUND, "clientId");
            }

            String accountNumber = buildAccountNumber(clientId, request.getBalanceType(), request.getCurrency());

            Long id = jdbcTemplate.queryForObject("select cm_account_seq.nextval from dual", Long.class);

            jdbcTemplate.update("insert into cm_account \n" +
                            "  (id, client_id, account_number, balance_type, currency) \n" +
                            "values (?, ?, ?, ?, ?)",
                    id, clientId, accountNumber, request.getBalanceType(), request.getCurrency());

            log.info("Account opened: id: {}, number: {}", id, accountNumber);

            return ApiResponse.ok(getAccount(id), 201);
        } catch (Exception e) {
            return checkRequest.dbError(e);
        }
    }

    public ApiResponse<List<AccountInfo>> getClientAccounts(Long clientId) {
        if (checkRequest.isInvalidId(clientId)) {
            return ApiResponse.error(ErrorCode.INVALID_PARAM, "clientId");
        }

        try {
            if (clientService.getActiveClient(clientId) == null) {
                return ApiResponse.error(ErrorCode.CLIENT_NOT_FOUND, "clientId");
            }

            List<AccountInfo> accounts = jdbcTemplate.query(ACCOUNT_SELECT +
                    " where a.client_id = ? \n" +
                    " order by a.id", ACCOUNT_MAPPER, clientId);

            return ApiResponse.ok(accounts, 200);
        } catch (Exception e) {
            return checkRequest.dbError(e);
        }
    }

    @Transactional
    public ApiResponse<AccountInfo> deposit(Long id, DepositRequest request) {
        if (checkRequest.isInvalidId(id)) {
            return ApiResponse.error(ErrorCode.INVALID_PARAM, "id");
        }
        if (request.getAmount() == null) {
            return ApiResponse.error(ErrorCode.EMPTY_PARAM, "amount");
        }
        if (request.getAmount() <= 0) {
            return ApiResponse.error(ErrorCode.INVALID_PARAM, "amount");
        }

        try {
            int updated = jdbcTemplate.update("update cm_account a \n" +
                    "   set a.balance = a.balance + ? \n" +
                    " where a.id = ? \n" +
                    "   and a.state = " + STATE_OPEN, request.getAmount(), id);

            if (updated == 0) {
                ErrorCode code = getAccount(id) == null ? ErrorCode.ACC_NOT_FOUND : ErrorCode.ACC_CLOSED;
                return ApiResponse.error(code, "id");
            }

            jdbcTemplate.update(OPERATION_INSERT, id, request.getAmount(), request.getPurpose());

            log.info("Deposit: accountId: {}, amount: {}", id, request.getAmount());

            return ApiResponse.ok(getAccount(id), 200);
        } catch (Exception e) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return checkRequest.dbError(e);
        }
    }

    public ApiResponse<List<OperationInfo>> getOperations(Long id, String from, String to) {
        boolean hasFrom = !checkRequest.isEmpty(from);
        boolean hasTo = !checkRequest.isEmpty(to);

        List<String> fields = new ArrayList<>();
        if (checkRequest.isInvalidId(id)) {
            fields.add("id");
        }
        if (hasFrom && !checkRequest.isDate(from)) {
            fields.add("from");
        }
        if (hasTo && !checkRequest.isDate(to)) {
            fields.add("to");
        }
        if (!fields.isEmpty()) {
            return ApiResponse.error(ErrorCode.INVALID_PARAM, fields);
        }

        try {
            Integer count = jdbcTemplate.queryForObject(
                    "select count(*) from cm_account a where a.id = ?", Integer.class, id);
            if (count == null || count == 0) {
                return ApiResponse.error(ErrorCode.ACC_NOT_FOUND, "id");
            }

            StringBuilder sql = new StringBuilder("select o.id, \n" +
                    "       o.account_id, \n" +
                    "       o.amount, \n" +
                    "       o.purpose, \n" +
                    "       o.created_at \n" +
                    "  from cm_operation o \n" +
                    " where o.account_id = ? \n");
            List<Object> args = new ArrayList<>();
            args.add(id);

            if (hasFrom) {
                sql.append("   and o.created_at >= ? \n");
                args.add(Date.valueOf(from));
            }
            if (hasTo) {
                sql.append("   and o.created_at < ? + 1 \n");
                args.add(Date.valueOf(to));
            }
            sql.append(" order by o.id desc");

            List<OperationInfo> operations = jdbcTemplate.query(sql.toString(), OPERATION_MAPPER, args.toArray());

            return ApiResponse.ok(operations, 200);
        } catch (Exception e) {
            return checkRequest.dbError(e);
        }
    }

    @Transactional
    public ApiResponse<TransferResult> transfer(TransferRequest request) {
        Long fromId = request.getFromAccountId();
        Long toId = request.getToAccountId();
        Long amount = request.getAmount();

        List<String> fields = new ArrayList<>();
        if (fromId == null) {
            fields.add("fromAccountId");
        }
        if (toId == null) {
            fields.add("toAccountId");
        }
        if (amount == null) {
            fields.add("amount");
        }
        if (!fields.isEmpty()) {
            return ApiResponse.error(ErrorCode.EMPTY_PARAM, fields);
        }

        if (amount <= 0) {
            fields.add("amount");
        }
        if (fromId.equals(toId)) {
            fields.add("toAccountId");
        }
        if (!fields.isEmpty()) {
            return ApiResponse.error(ErrorCode.INVALID_PARAM, fields);
        }

        try {
            Map<Long, AccountInfo> accounts = getAccounts(fromId, toId);
            AccountInfo from = accounts.get(fromId);
            AccountInfo to = accounts.get(toId);

            if (from == null) {
                return ApiResponse.error(ErrorCode.ACC_NOT_FOUND, "fromAccountId");
            }
            if (to == null) {
                return ApiResponse.error(ErrorCode.ACC_NOT_FOUND, "toAccountId");
            }

            if (from.getState() != STATE_OPEN) {
                return ApiResponse.error(ErrorCode.ACC_CLOSED, "fromAccountId");
            }
            if (to.getState() != STATE_OPEN) {
                return ApiResponse.error(ErrorCode.ACC_CLOSED, "toAccountId");
            }

            if (!from.getCurrency().equals(to.getCurrency())) {
                return ApiResponse.error(ErrorCode.CURRENCY_MISMATCH, Arrays.asList("fromAccountId", "toAccountId"));
            }

            int updated = jdbcTemplate.update("update cm_account a \n" +
                    "   set a.balance = a.balance - ? \n" +
                    " where a.id = ? \n" +
                    "   and a.balance >= ?", amount, fromId, amount);
            if (updated == 0) {
                return ApiResponse.error(ErrorCode.INSUFFICIENT_FUNDS, "amount");
            }

            jdbcTemplate.update("update cm_account a \n" +
                    "   set a.balance = a.balance + ? \n" +
                    " where a.id = ?", amount, toId);

            jdbcTemplate.batchUpdate(OPERATION_INSERT, Arrays.asList(
                    new Object[]{fromId, -amount, request.getPurpose()},
                    new Object[]{toId, amount, request.getPurpose()}));

            log.info("Transfer: from: {}, to: {}, amount: {}", fromId, toId, amount);

            accounts = getAccounts(fromId, toId);
            TransferResult result = TransferResult.builder()
                    .amount(amount)
                    .fromAccount(accounts.get(fromId))
                    .toAccount(accounts.get(toId))
                    .build();

            return ApiResponse.ok(result, 200);
        } catch (Exception e) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return checkRequest.dbError(e);
        }
    }

    public ApiResponse<AccountInfo> closeAccount(Long id) {
        if (checkRequest.isInvalidId(id)) {
            return ApiResponse.error(ErrorCode.INVALID_PARAM, "id");
        }

        try {
            int updated = jdbcTemplate.update("update cm_account a \n" +
                    "   set a.state = " + STATE_CLOSED + ", \n" +
                    "       a.date_close = sysdate \n" +
                    " where a.id = ? \n" +
                    "   and a.state <> " + STATE_CLOSED + " \n" +
                    "   and a.balance = 0", id);

            if (updated == 0) {
                AccountInfo account = getAccount(id);
                if (account == null) {
                    return ApiResponse.error(ErrorCode.ACC_NOT_FOUND, "id");
                }
                if (account.getState() == STATE_CLOSED) {
                    return ApiResponse.error(ErrorCode.ACC_ALREADY_CLOSED, "id");
                }
                return ApiResponse.error(ErrorCode.ACC_HAS_BALANCE, "balance");
            }

            log.info("Account closed: id: {}", id);

            return ApiResponse.ok(getAccount(id), 200);
        } catch (Exception e) {
            return checkRequest.dbError(e);
        }
    }

    private String buildAccountNumber(Long clientId, String balanceType, String currency) {
        String currencyCode = "860".equals(currency) ? "000" : currency;

        Integer count = jdbcTemplate.queryForObject("select count(*) \n" +
                "  from cm_account a \n" +
                " where a.client_id = ? \n" +
                "   and a.balance_type = ? \n" +
                "   and a.currency = ?", Integer.class, clientId, balanceType, currency);

        String order = String.format("%03d", (count == null ? 0 : count) + 1);
        String client = String.format("%08d", clientId);

        return balanceType + currencyCode + "0" + client + order;
    }

    private AccountInfo getAccount(Long id) {
        List<AccountInfo> list = jdbcTemplate.query(ACCOUNT_SELECT + " where a.id = ?", ACCOUNT_MAPPER, id);
        return list.isEmpty() ? null : list.get(0);
    }

    private Map<Long, AccountInfo> getAccounts(Long id1, Long id2) {
        Map<Long, AccountInfo> map = new HashMap<>();
        for (AccountInfo account : jdbcTemplate.query(ACCOUNT_SELECT + " where a.id in (?, ?)", ACCOUNT_MAPPER, id1, id2)) {
            map.put(account.getId(), account);
        }
        return map;
    }
}
