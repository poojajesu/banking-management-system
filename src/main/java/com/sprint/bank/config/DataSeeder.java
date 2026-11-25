package com.sprint.bank.config;

import com.sprint.bank.entity.*;
import com.sprint.bank.repository.AccountRepository;
import com.sprint.bank.repository.TransactionRepository;
import com.sprint.bank.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Component
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(UserRepository userRepository,
                      AccountRepository accountRepository,
                      TransactionRepository transactionRepository,
                      PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (userRepository.count() > 0) {
            return;
        }

        // 1. Seed Admin user
        User admin = new User();
        admin.setEmail("admin@bank.com");
        admin.setPassword(passwordEncoder.encode("admin123"));
        admin.setRole(Role.ADMIN);
        userRepository.save(admin);

        // 2. Seed Customer 1: Rahul Verma
        User rahulUser = new User();
        rahulUser.setEmail("rahul@bank.com");
        rahulUser.setPassword(passwordEncoder.encode("customer123"));
        rahulUser.setRole(Role.CUSTOMER);

        CustomerProfile rahulProfile = new CustomerProfile();
        rahulProfile.setFullName("Rahul Verma");
        rahulProfile.setPhone("9876543210");
        rahulProfile.setAddress("Indiranagar, Bengaluru");
        rahulProfile.setPan("ABCDE1234F");
        rahulProfile.setUser(rahulUser);
        rahulUser.setCustomerProfile(rahulProfile);
        userRepository.save(rahulUser);

        Account rahulSavings = new Account();
        rahulSavings.setAccountNumber("BNK100001");
        rahulSavings.setType(AccountType.SAVINGS);
        rahulSavings.setBalance(new BigDecimal("15000.00"));
        rahulSavings.setStatus(AccountStatus.ACTIVE);
        rahulSavings.setCreatedAt(LocalDateTime.now());
        rahulSavings.setCustomer(rahulProfile);
        accountRepository.save(rahulSavings);

        Transaction t1 = new Transaction();
        t1.setAccount(rahulSavings);
        t1.setType(TransactionType.DEPOSIT);
        t1.setAmount(new BigDecimal("15000.00"));
        t1.setTimestamp(LocalDateTime.now());
        t1.setDescription("Initial account opening deposit");
        transactionRepository.save(t1);

        // 3. Seed Customer 2: Ananya Rao
        User ananyaUser = new User();
        ananyaUser.setEmail("ananya@bank.com");
        ananyaUser.setPassword(passwordEncoder.encode("customer123"));
        ananyaUser.setRole(Role.CUSTOMER);

        CustomerProfile ananyaProfile = new CustomerProfile();
        ananyaProfile.setFullName("Ananya Rao");
        ananyaProfile.setPhone("9123456780");
        ananyaProfile.setAddress("Koramangala, Bengaluru");
        ananyaProfile.setPan("FGHIJ5678K");
        ananyaProfile.setUser(ananyaUser);
        ananyaUser.setCustomerProfile(ananyaProfile);
        userRepository.save(ananyaUser);

        Account ananyaSavings = new Account();
        ananyaSavings.setAccountNumber("BNK100002");
        ananyaSavings.setType(AccountType.SAVINGS);
        ananyaSavings.setBalance(new BigDecimal("8000.00"));
        ananyaSavings.setStatus(AccountStatus.ACTIVE);
        ananyaSavings.setCreatedAt(LocalDateTime.now());
        ananyaSavings.setCustomer(ananyaProfile);
        accountRepository.save(ananyaSavings);

        Transaction t2 = new Transaction();
        t2.setAccount(ananyaSavings);
        t2.setType(TransactionType.DEPOSIT);
        t2.setAmount(new BigDecimal("8000.00"));
        t2.setTimestamp(LocalDateTime.now());
        t2.setDescription("Initial account opening deposit");
        transactionRepository.save(t2);
    }
}
