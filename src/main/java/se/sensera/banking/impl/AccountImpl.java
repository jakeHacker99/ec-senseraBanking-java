package se.sensera.banking.impl;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.SneakyThrows;
import se.sensera.banking.Account;
import se.sensera.banking.User;

import se.sensera.banking.exceptions.Activity;

import java.util.LinkedList;
import java.util.List;
import java.util.stream.Stream;

@Data
@AllArgsConstructor
public class AccountImpl implements Account {
    private String id;
    private User owner;
    private String name;
    private boolean active;
    private List<User> users = new LinkedList<>();


    public AccountImpl(String id, User owner, String name, boolean active) {
        this.id = id;
        this.owner = owner;
        this.name = name;
        this.active = active;
    }


    public Stream<User> getUsers() {
        return users.stream();
    }

    @Override
    public void addUser(User user) {
        users.add(user);
    }

    @Override
    public void removeUser(User user) {
        users.remove(user);
    }



}
