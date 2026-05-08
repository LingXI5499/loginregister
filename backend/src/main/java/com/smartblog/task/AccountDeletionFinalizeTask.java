package com.smartblog.task;

import com.smartblog.service.AccountService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class AccountDeletionFinalizeTask {

    private final AccountService accountService;

    public AccountDeletionFinalizeTask(AccountService accountService) {
        this.accountService = accountService;
    }

    @Scheduled(fixedDelayString = "${account.delete.finalize-fixed-delay-ms:600000}")
    public void finalizeDueDeletionRequests() {
        accountService.finalizeDueDeletionRequests();
    }
}
