package se.sensera.banking.impl;

import lombok.AllArgsConstructor;
import lombok.Data;
import se.sensera.banking.Account;
import se.sensera.banking.Transaction;
import se.sensera.banking.User;

import java.util.Date;


@Data
@AllArgsConstructor
public class TransactionImpl implements Transaction {
    public String id;
    public Date created;
    public User user;
    public Account account;
    public double amount;
}
