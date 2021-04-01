package se.sensera.banking.impl;

import lombok.SneakyThrows;
import lombok.Synchronized;
import se.sensera.banking.*;
import se.sensera.banking.exceptions.Activity;
import se.sensera.banking.exceptions.UseException;
import se.sensera.banking.exceptions.UseExceptionType;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public class TransactionServiceImpl implements TransactionService {
    private  UsersRepository usersRepository;
    private  List<Consumer<Transaction>> monitorListing = new LinkedList<>();
    private  AccountsRepository accountsRepository;
    private  TransactionsRepository transactionsRepository;



    public TransactionServiceImpl(UsersRepository usersRepository, AccountsRepository accountsRepository, TransactionsRepository transactionsRepository) {
        this.usersRepository = usersRepository;
        this.accountsRepository = accountsRepository;
        this.transactionsRepository = transactionsRepository;
    }

    @Override
    @SneakyThrows
    public Transaction createTransaction(String created, String userId, String accountId, double amount){
        User user = getUser(userId);    Account account = getAccount(accountId);
        Date date = stringDateFormatter(created);   changePossible(userId, user, account);
        testAmountForAccount(created, userId, accountId, amount);

        Transaction transaction = new TransactionImpl(UUID.randomUUID().toString(), date, user, account, amount);
        new Thread(() -> monitorListing.forEach(transactionConsumer -> transactionConsumer.accept(transaction))).start();

        return transactionsRepository.save(transaction);
    }


    @SneakyThrows
    private void testAmountForAccount(String created, String userId, String accountId, double amount)  {
        AtomicBoolean foundErrors = new AtomicBoolean(false);
        Thread t1 = new Thread(() -> {
            try {if (sum(created, userId, accountId) + amount < 0){ foundErrors.set(true);  }}
            catch (UseException error) {    error.printStackTrace();    }});

        notFoundException(foundErrors,t1);
    }

    @SneakyThrows
    private void notFoundException(AtomicBoolean foundErrors, Thread t1) {
        try {t1.start();t1.join();} catch (InterruptedException error) {error.printStackTrace();}

        if (foundErrors.get()){
            throw new UseException(Activity.CREATE_TRANSACTION, UseExceptionType.NOT_FUNDED);
        }
    }

    @SneakyThrows
    private void changePossible(String userId, User user, Account account) {
        if (!account.getOwner().equals(user) & account.getUsers().noneMatch(firstUser -> firstUser.getId().equals(userId)))
            throw new UseException(Activity.CREATE_TRANSACTION, UseExceptionType.NOT_ALLOWED);
    }

    private Date stringDateFormatter(String created) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        LocalDateTime localDateTime = LocalDateTime.parse(created, formatter);
        return Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant());
    }

    @SneakyThrows

    private User getUser(String userId)  {
        return usersRepository.getEntityById(userId).orElseThrow(() ->new UseException(Activity.CREATE_TRANSACTION, UseExceptionType.USER_NOT_FOUND));
    }

    @SneakyThrows

    private Account getAccount(String accountId)  {
        return accountsRepository.getEntityById(accountId).orElseThrow(() ->new UseException(Activity.CREATE_TRANSACTION, UseExceptionType.ACCOUNT_NOT_FOUND));
    }

    @Override
    public double sum(String created, String userId, String accountId) throws UseException {
        Account account = getAccount(accountId);
        isUserExisting(userId, account);
        Date date = stringDateFormatter(created);

        return transactionsRepository.all()
                .filter(transaction -> transaction.getAccount().getId().equals(accountId)&& (transaction.getCreated().before(date) || transaction.getCreated().equals(date)))
                .mapToDouble(Transaction::getAmount).sum();
    }

    @SneakyThrows
    private void isUserExisting(String userId, Account account)  {
        if (!account.getOwner().getId().equals(userId)&& account.getUsers().noneMatch(u1 -> u1.getId().equals(userId))) {
            throw new UseException(Activity.SUM_TRANSACTION,
                    UseExceptionType.NOT_ALLOWED);
        }
    }
    @Override
    public void addMonitor(Consumer<Transaction> monitor) {
        monitorListing.add(monitor);
    }
}

