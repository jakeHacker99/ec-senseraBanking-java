package se.sensera.banking.impl;

import lombok.SneakyThrows;
import lombok.AllArgsConstructor;

import se.sensera.banking.*;
import se.sensera.banking.exceptions.UseExceptionType;
import se.sensera.banking.exceptions.UseException;

import se.sensera.banking.utils.ListUtils;
import se.sensera.banking.exceptions.Activity;



import java.util.Comparator;
import java.util.stream.Stream;
import java.util.UUID;
import java.util.function.Consumer;


@AllArgsConstructor
public class AccountServiceImpl implements AccountService {
    protected   UsersRepository usersRepository;
    protected   AccountsRepository accountsRepository;

    @Override
    @SneakyThrows
    public Account createAccount(String userId, String accountName)  {
        User user = getUsers(userId, Activity.CREATE_ACCOUNT);
        Account account = new AccountImpl(UUID.randomUUID().toString(), user, accountName, true);

        if (accountsRepository.all()
                .anyMatch(acc1 -> acc1.getName().equals(accountName)))
            throw new UseException(Activity.CREATE_ACCOUNT, UseExceptionType.ACCOUNT_NAME_NOT_UNIQUE);

        return accountsRepository.save(account);
    }

    @Override
    @SneakyThrows
    public Account changeAccount(String userId, String accountId, Consumer<ChangeAccount> changeAccountConsumer)  {
        boolean[] isSaved = {true}; Account account = getAccounts(accountId, Activity.UPDATE_ACCOUNT, UseExceptionType.ACCOUNT_NOT_FOUND);
        changePossible(userId, account, Activity.UPDATE_ACCOUNT);
        isAccountActive(account, Activity.UPDATE_ACCOUNT, UseExceptionType.NOT_ACTIVE);
        notUniqueException(changeAccountConsumer, isSaved, account);

        if (isSaved[0]){accountsRepository.save(account);}return account;
    }

    private void notUniqueException(Consumer<ChangeAccount> changeAccountConsumer, boolean[] isSaved, Account account) {
        changeAccountConsumer.accept(name -> { if (accountsRepository.all().anyMatch(acc1 -> acc1.getName().equals(name))) {isSaved[0] = false;
            throw new UseException(Activity.UPDATE_ACCOUNT, UseExceptionType.ACCOUNT_NAME_NOT_UNIQUE); }
        else if (account.getName().equals(name)){isSaved[0] = false;}
        else{account.setName(name);}});
    }


    @SneakyThrows
    private void changePossible(String userId, Account account, Activity activity)  {
        if (!account.getOwner().getId().equals(userId))
            throw new UseException(activity, UseExceptionType.NOT_OWNER);

    }

    @SneakyThrows
    private void isAccountActive(Account account, Activity activity, UseExceptionType useExceptionType)  {
        if (!account.isActive())
            throw new UseException(activity, useExceptionType);

    }

    @SneakyThrows
    private Account getAccounts(String accountId, Activity activity, UseExceptionType useExceptionType)  {
        return accountsRepository
                .getEntityById(accountId)
                .orElseThrow(() -> new UseException(activity, useExceptionType));
    }

    @SneakyThrows
    private User getUsers(String userId, Activity activity)  {
        return usersRepository
                .getEntityById(userId)
                .orElseThrow(() -> new UseException(activity, UseExceptionType.USER_NOT_FOUND));
    }

    @Override
    @SneakyThrows
    public Account addUserToAccount(String userId, String accountId, String userIdToBeAssigned) {
        User user1 = getUsers(userIdToBeAssigned, Activity.UPDATE_ACCOUNT);
        Account account = getAccounts(accountId, Activity.UPDATE_ACCOUNT, UseExceptionType.NOT_FOUND);

        isAccountActive(account, Activity.UPDATE_ACCOUNT, UseExceptionType.ACCOUNT_NOT_ACTIVE);
        isAssignedUserOwner(userId, user1.getId());

        isUserAssignedToAccount(user1, account);
        changePossible(userId, account, Activity.UPDATE_ACCOUNT);

        account.addUser(user1);
        return accountsRepository.save(account);
    }

    @SneakyThrows
    private void isAssignedUserOwner(String userId, String userIdToBeAssigned)  {
        if (userId.equals(userIdToBeAssigned))
            throw new UseException(Activity.UPDATE_ACCOUNT, UseExceptionType.CANNOT_ADD_OWNER_AS_USER);

    }

    @SneakyThrows
    private void isUserAssignedToAccount(User newUser, Account account) {
        if(account.getUsers().anyMatch(firstUser ->firstUser.getId().equals(newUser.getId())))
            throw new UseException(Activity.UPDATE_ACCOUNT, UseExceptionType.USER_ALREADY_ASSIGNED_TO_THIS_ACCOUNT);

    }

    @SneakyThrows
    private void checkIfUserIsNotAssignedToAccount(String userIdToBeAssigned, Account account)  {
        if(account.getUsers().noneMatch(firstUser -> firstUser.getId().equals(userIdToBeAssigned)))
            throw new UseException(Activity.UPDATE_ACCOUNT, UseExceptionType.USER_NOT_ASSIGNED_TO_THIS_ACCOUNT);

    }

    @Override
    @SneakyThrows
    public Account removeUserFromAccount(String userId, String accountId, String userIdToBeAssigned)  {
        Account account = getAccounts(accountId, Activity.UPDATE_ACCOUNT, UseExceptionType.NOT_FOUND);
        User user = getUsers(userIdToBeAssigned, Activity.UPDATE_ACCOUNT);

        changePossible(userId, account, Activity.UPDATE_ACCOUNT);
        checkIfUserIsNotAssignedToAccount(userIdToBeAssigned, account);

        account.removeUser(user);
        return accountsRepository.save(account);
    }

    @Override
    @SneakyThrows
    public Account inactivateAccount(String userId, String accountId) {
        User user = getUsers(userId, Activity.INACTIVATE_ACCOUNT);
        Account account = getAccounts(accountId, Activity.INACTIVATE_ACCOUNT, UseExceptionType.NOT_FOUND);

        isAccountActive(account, Activity.INACTIVATE_ACCOUNT, UseExceptionType.NOT_ACTIVE);
        changePossible(user.getId(), account, Activity.INACTIVATE_ACCOUNT);

        account.setActive(false);
        return accountsRepository.save(account);
    }

    @Override
    @SneakyThrows
    public Stream<Account> findAccounts(String searchValue, String userId, Integer pageNumber, Integer pageSize, SortOrder sortOrder) {
        Stream<Account> account = accountsRepository.all();
        switch (sortOrder) {
            case AccountName : {return nameSort(pageNumber, pageSize, account);}
            case None :{return argumenSort(searchValue, userId, pageNumber, pageSize, account);}
            default : throw new UseException(Activity.FIND_ACCOUNT, UseExceptionType.NOT_FOUND);
        }
    }

    private Stream<Account> nameSort(Integer pageNumber, Integer pageSize, Stream<Account> account) {
        if ( pageSize != null |pageNumber != null) {
            account = account.sorted(Comparator.comparing(Account::getName));
            return ListUtils.applyPage(account, pageNumber, pageSize);
        }
        return account.sorted(Comparator.comparing(Account::getName));
    }

    private Stream<Account> argumenSort(String searchValue, String userId, Integer pageNumber, Integer pageSize, Stream<Account> account) {
        if (  userId == null & searchValue.equals("")) {
            return ListUtils.applyPage(account, pageNumber, pageSize);
        }
        else if (userId != null) {
            return account.filter(acc1 -> acc1.getOwner().getId().equals(userId) | acc1.getUsers().anyMatch(user -> user.getId().equals(userId)));
        }
        return account.filter(account1 -> account1.getName().toLowerCase().contains(searchValue));
    }
}
