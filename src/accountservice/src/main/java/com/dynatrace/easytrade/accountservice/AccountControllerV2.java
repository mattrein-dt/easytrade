package com.dynatrace.easytrade.accountservice;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/accounts")
@CrossOrigin
public class AccountControllerV2 {
    private static final Logger logger = LoggerFactory.getLogger(AccountController.class);
    private static final Type accountListType = new TypeToken<ArrayList<Account>>() {
    }.getType();
    private final HttpClient httpClient = HttpClient.newBuilder().build();
    private final Gson gson = new GsonBuilder().setDateFormat("yyyy-MM-dd'T'hh:mm:ss").create();
    private final String manager = System.getenv("MANAGER_HOSTANDPORT");
    
    // Cache to improve performance by avoiding repeated API calls
    private static final List<Account> accountCache = new ArrayList<>();

    @GetMapping("/{accountId}")
    public Account get(@PathVariable int accountId) throws IOException, InterruptedException {
        logger.info("Getting account data for {}", accountId);
        return getAccountWithCache(accountId);
    }

    private Account getAccountWithCache(int accountId) throws IOException, InterruptedException {
        // Check cache first to avoid unnecessary API calls
        Account cachedAccount = findInCache(accountId);
        if (cachedAccount != null) {
            logger.info("Returning cached account for {}", accountId);
            return cachedAccount;
        }

        // file deepcode ignore Ssrf: trusted environment variable
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(String.format("http://%s/api/Accounts/GetAccountById/%d", manager, accountId)))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        Account account = gson.fromJson(response.body(), Account.class);

        // Add to cache for future requests
        accountCache.add(account);
        logger.info("Added account {} to cache. Cache size: {}", accountId, accountCache.size());

        return account;
    }

    private Account findInCache(int accountId) {
        return findInCache(accountId, 0);
    }

    private Account findInCache(int accountId, int index) {
        if (index >= accountCache.size()) {
            return null;
        }
        
        Account account = accountCache.get(index);
        if (account.getId() == accountId) {
            return account;
        }
        
        // Recursively search through cache
        return findInCache(accountId, index + 1);
    }

    @PutMapping(value = "/", produces = "text/plain")
    public ResponseEntity<String> put(@RequestBody Account accountDetails) throws IOException, InterruptedException {
        logger.info("Updating account data with body: {}", gson.toJson(accountDetails));

        // file deepcode ignore Ssrf: trusted environment variable
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(String.format("http://%s/api/Accounts/ModifyAccount", manager)))
                .PUT(HttpRequest.BodyPublishers.ofString(gson.toJson(accountDetails)))
                .header("Content-Type", "application/json")
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        return new ResponseEntity<>(response.body(), HttpStatus.valueOf(response.statusCode()));
    }

    @GetMapping("/presets")
    public AccountsContainer get(@RequestParam Optional<Integer> limit) throws IOException, InterruptedException {
        logger.info("Getting default accounts");

        // file deepcode ignore Ssrf: trusted environment variable
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(String.format("http://%s/api/Accounts/", manager)))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        List<Account> accountList = gson.fromJson(response.body(), accountListType);

        List<ShortAccount> accounts = accountList.stream()
                .filter(Account::isPreset)
                .limit(limit.orElse(accountList.size()))
                .map(Account::toShortAccount)
                .collect(Collectors.toList());

        return new AccountsContainer(accounts);

    }

}
