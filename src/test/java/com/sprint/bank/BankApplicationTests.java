package com.sprint.bank;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class BankApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void fullCustomerAndAdminFlow() throws Exception {
        // 1. Register a new customer
        String registerJson = """
                {
                  "fullName": "Kiran Patel",
                  "email": "kiran@example.com",
                  "password": "password123",
                  "phone": "9988776655",
                  "address": "MG Road, Pune",
                  "pan": "KLMNO1122P"
                }
                """;

        MvcResult regResult = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.role").value("CUSTOMER"))
                .andReturn();

        String customerToken = objectMapper.readTree(regResult.getResponse().getContentAsString())
                .get("token").asText();

        // 2. Customer cannot access Admin endpoints
        mockMvc.perform(get("/api/admin/customers")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isForbidden());

        // 3. Open two accounts (SAVINGS with 1000, CURRENT with 500)
        MvcResult acc1Res = mockMvc.perform(post("/api/accounts")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"SAVINGS\",\"initialDeposit\":1000}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.balance").value(1000))
                .andReturn();

        JsonNode acc1Node = objectMapper.readTree(acc1Res.getResponse().getContentAsString());
        long acc1Id = acc1Node.get("id").asLong();

        MvcResult acc2Res = mockMvc.perform(post("/api/accounts")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"CURRENT\",\"initialDeposit\":500}"))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode acc2Node = objectMapper.readTree(acc2Res.getResponse().getContentAsString());
        long acc2Id = acc2Node.get("id").asLong();
        String acc2Number = acc2Node.get("accountNumber").asText();

        // 4. Deposit 500 into account 1 (balance -> 1500)
        mockMvc.perform(post("/api/accounts/" + acc1Id + "/deposit")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":500,\"description\":\"Bonus\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("DEPOSIT"));

        // 5. Withdraw more than balance -> InsufficientBalanceException (400)
        mockMvc.perform(post("/api/accounts/" + acc1Id + "/withdraw")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":5000}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());

        // 6. Transfer 300 from account 1 to account 2
        String transferJson = String.format(
                "{\"fromAccountId\":%d,\"toAccountNumber\":\"%s\",\"amount\":300,\"description\":\"Rent\"}",
                acc1Id, acc2Number
        );

        mockMvc.perform(post("/api/transfers")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(transferJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("TRANSFER"));

        // 7. Check account 1 statement has transactions
        mockMvc.perform(get("/api/accounts/" + acc1Id + "/transactions")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3));

        // 8. Login as seeded Admin
        MvcResult adminLogin = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"admin@bank.com\",\"password\":\"admin123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ADMIN"))
                .andReturn();

        String adminToken = objectMapper.readTree(adminLogin.getResponse().getContentAsString())
                .get("token").asText();

        // 9. Admin views customers, freezes account 2, and filters TRANSFER transactions
        mockMvc.perform(get("/api/admin/customers")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        mockMvc.perform(put("/api/admin/accounts/" + acc2Id + "/status")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"FROZEN\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FROZEN"));

        mockMvc.perform(get("/api/admin/transactions?type=TRANSFER")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }
}
