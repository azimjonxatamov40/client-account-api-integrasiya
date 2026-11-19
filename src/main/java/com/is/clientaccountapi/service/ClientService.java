package com.is.clientaccountapi.service;

import com.is.clientaccountapi.component.CheckRequest;
import com.is.clientaccountapi.model.client.ClientInfo;
import com.is.clientaccountapi.model.client.ClientRequest;
import com.is.clientaccountapi.model.result.ApiResponse;
import com.is.clientaccountapi.model.result.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;

import java.sql.Date;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClientService {
    private final JdbcTemplate jdbcTemplate;
    private final CheckRequest checkRequest;

    private static final String CLIENT_SELECT = "select c.id, \n" +
            "       c.pinfl, \n" +
            "       c.first_name, \n" +
            "       c.last_name, \n" +
            "       c.patronymic, \n" +
            "       c.birth_date, \n" +
            "       c.passport, \n" +
            "       c.phone, \n" +
            "       c.state, \n" +
            "       c.created_at \n" +
            "  from cm_client c \n";

    private static final RowMapper<ClientInfo> CLIENT_MAPPER = (rs, rowNum) -> {
        Date birthDate = rs.getDate("birth_date");
        return ClientInfo.builder()
                .id(rs.getLong("id"))
                .pinfl(rs.getString("pinfl"))
                .firstName(rs.getString("first_name"))
                .lastName(rs.getString("last_name"))
                .patronymic(rs.getString("patronymic"))
                .birthDate(birthDate == null ? null : birthDate.toString())
                .passport(rs.getString("passport"))
                .phone(rs.getString("phone"))
                .state(rs.getInt("state"))
                .createdAt(rs.getTimestamp("created_at").toLocalDateTime().format(CheckRequest.DATE_TIME))
                .build();
    };

    public ApiResponse<ClientInfo> createClient(ClientRequest request) {
        List<String> fields = new ArrayList<>();
        if (checkRequest.isEmpty(request.getPinfl())) {
            fields.add("pinfl");
        }
        if (checkRequest.isEmpty(request.getFirstName())) {
            fields.add("firstName");
        }
        if (checkRequest.isEmpty(request.getLastName())) {
            fields.add("lastName");
        }
        if (!fields.isEmpty()) {
            return ApiResponse.error(ErrorCode.EMPTY_PARAM, fields);
        }

        boolean hasBirthDate = !checkRequest.isEmpty(request.getBirthDate());
        if (!request.getPinfl().matches("\\d{14}")) {
            fields.add("pinfl");
        }
        if (hasBirthDate && !checkRequest.isDate(request.getBirthDate())) {
            fields.add("birthDate");
        }
        if (!fields.isEmpty()) {
            return ApiResponse.error(ErrorCode.INVALID_PARAM, fields);
        }

        try {
            Integer count = jdbcTemplate.queryForObject("select count(*) \n" +
                    "  from cm_client c \n" +
                    " where c.pinfl = ? \n" +
                    "   and rownum = 1", Integer.class, request.getPinfl());
            if (count != null && count > 0) {
                return ApiResponse.error(ErrorCode.CLIENT_EXISTS, "pinfl");
            }

            Long id = jdbcTemplate.queryForObject("select cm_client_seq.nextval from dual", Long.class);

            jdbcTemplate.update("insert into cm_client \n" +
                            "  (id, pinfl, first_name, last_name, patronymic, birth_date, passport, phone) \n" +
                            "values (?, ?, ?, ?, ?, ?, ?, ?)",
                    id,
                    request.getPinfl(),
                    request.getFirstName(),
                    request.getLastName(),
                    request.getPatronymic(),
                    hasBirthDate ? Date.valueOf(request.getBirthDate()) : null,
                    request.getPassport(),
                    request.getPhone());

            log.info("Client created: id: {}, pinfl: {}", id, request.getPinfl());

            return ApiResponse.ok(getActiveClient(id), 201);
        } catch (Exception e) {
            return checkRequest.dbError(e);
        }
    }

    public ApiResponse<List<ClientInfo>> getClients(Integer page, Integer size, String search) {
        int p = page == null ? 0 : page;
        int s = size == null ? 20 : size;

        List<String> fields = new ArrayList<>();
        if (p < 0) {
            fields.add("page");
        }
        if (s < 1 || s > 100) {
            fields.add("size");
        }
        if (!fields.isEmpty()) {
            return ApiResponse.error(ErrorCode.INVALID_PARAM, fields);
        }

        String where = " where c.state = 1 \n";
        List<Object> args = new ArrayList<>();

        if (!checkRequest.isEmpty(search)) {
            where += "   and (lower(c.first_name) like ? \n" +
                    "     or lower(c.last_name)  like ? \n" +
                    "     or lower(c.patronymic) like ?) \n";
            String like = "%" + search.toLowerCase() + "%";
            args.add(like);
            args.add(like);
            args.add(like);
        }

        try {
            String sql = "select * \n" +
                    "  from (select t.*, rownum rn \n" +
                    "          from (" + CLIENT_SELECT + where +
                    "                 order by c.id) t \n" +
                    "         where rownum <= ?) \n" +
                    " where rn > ?";

            args.add((p + 1) * s);
            args.add(p * s);

            return ApiResponse.ok(jdbcTemplate.query(sql, CLIENT_MAPPER, args.toArray()), 200);
        } catch (Exception e) {
            return checkRequest.dbError(e);
        }
    }

    public ApiResponse<ClientInfo> getClientById(Long id) {
        if (checkRequest.isInvalidId(id)) {
            return ApiResponse.error(ErrorCode.INVALID_PARAM, "id");
        }

        try {
            ClientInfo client = getActiveClient(id);
            if (client == null) {
                return ApiResponse.error(ErrorCode.CLIENT_NOT_FOUND, "id");
            }

            return ApiResponse.ok(client, 200);
        } catch (Exception e) {
            return checkRequest.dbError(e);
        }
    }

    public ApiResponse<ClientInfo> updateClient(Long id, ClientRequest request) {
        if (checkRequest.isInvalidId(id)) {
            return ApiResponse.error(ErrorCode.INVALID_PARAM, "id");
        }

        List<String> fields = new ArrayList<>();
        if (checkRequest.isEmpty(request.getFirstName())) {
            fields.add("firstName");
        }
        if (checkRequest.isEmpty(request.getLastName())) {
            fields.add("lastName");
        }
        if (!fields.isEmpty()) {
            return ApiResponse.error(ErrorCode.EMPTY_PARAM, fields);
        }

        boolean hasBirthDate = !checkRequest.isEmpty(request.getBirthDate());
        if (hasBirthDate && !checkRequest.isDate(request.getBirthDate())) {
            return ApiResponse.error(ErrorCode.INVALID_PARAM, "birthDate");
        }

        try {
            int updated = jdbcTemplate.update("update cm_client c \n" +
                            "   set c.first_name = ?, \n" +
                            "       c.last_name  = ?, \n" +
                            "       c.patronymic = ?, \n" +
                            "       c.birth_date = ?, \n" +
                            "       c.passport   = ?, \n" +
                            "       c.phone      = ?, \n" +
                            "       c.updated_at = sysdate \n" +
                            " where c.id = ? \n" +
                            "   and c.state <> 0",
                    request.getFirstName(),
                    request.getLastName(),
                    request.getPatronymic(),
                    hasBirthDate ? Date.valueOf(request.getBirthDate()) : null,
                    request.getPassport(),
                    request.getPhone(),
                    id);

            if (updated == 0) {
                return ApiResponse.error(ErrorCode.CLIENT_NOT_FOUND, "id");
            }

            log.info("Client updated: id: {}", id);

            return ApiResponse.ok(getActiveClient(id), 200);
        } catch (Exception e) {
            return checkRequest.dbError(e);
        }
    }

    public ApiResponse<Boolean> deleteClient(Long id) {
        if (checkRequest.isInvalidId(id)) {
            return ApiResponse.error(ErrorCode.INVALID_PARAM, "id");
        }

        try {
            int deleted = jdbcTemplate.update("delete from cm_client c where c.id = ?", id);

            if (deleted == 0) {
                return ApiResponse.error(ErrorCode.CLIENT_NOT_FOUND, "id");
            }

            log.info("Client deleted: id: {}", id);

            return ApiResponse.ok(true, 200);
        } catch (Exception e) {
            return checkRequest.dbError(e);
        }
    }

    public ClientInfo getActiveClient(Long id) {
        List<ClientInfo> list = jdbcTemplate.query(CLIENT_SELECT +
                " where c.id = ? \n" +
                "   and c.state <> 0", CLIENT_MAPPER, id);
        return list.isEmpty() ? null : list.get(0);
    }
}
