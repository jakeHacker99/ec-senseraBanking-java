package se.sensera.banking.impl;

import lombok.SneakyThrows;
import se.sensera.banking.User;
import se.sensera.banking.UserService;
import se.sensera.banking.UsersRepository;
import se.sensera.banking.exceptions.Activity;
import se.sensera.banking.exceptions.UseException;
import se.sensera.banking.exceptions.UseExceptionType;
import se.sensera.banking.utils.ListUtils;

import java.util.Comparator;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.stream.Stream;

public class UserServiceImpl implements UserService {
    private UsersRepository usersRepository;

    public UserServiceImpl(UsersRepository usersRepository) {
        this.usersRepository = usersRepository;
    }

    @Override
    public User createUser(String name, String personalIdentificationNumber) throws UseException {
        if (usersRepository.all().anyMatch(user -> user.getPersonalIdentificationNumber().equals(personalIdentificationNumber)))
            throw new UseException(Activity.CREATE_USER, UseExceptionType.USER_PERSONAL_ID_NOT_UNIQUE);
        UserImpl user = new UserImpl(UUID.randomUUID().toString(), name, personalIdentificationNumber, true);
        return usersRepository.save(user);
    }

    @Override
    public User changeUser(String userId, Consumer<ChangeUser> changeUser) throws UseException {
        User user = usersRepository.getEntityById(userId).orElseThrow(() -> new UseException(Activity.UPDATE_USER, UseExceptionType.NOT_FOUND));
        AtomicBoolean save = new AtomicBoolean(true);

        setPersonInfo(changeUser, user, save);

        if (!save.get()) {return user;}     return usersRepository.save(user);
    }


    private void setPersonInfo(Consumer<ChangeUser> changeUser, User user , AtomicBoolean save) {
        changeUser.accept(new ChangeUser() {
            @Override public void setName(String name) {user.setName(name); }
            @Override @SneakyThrows
            public void setPersonalIdentificationNumber(String personalIdentificationNumber)  {
                if (usersRepository.all().anyMatch(user -> user.getPersonalIdentificationNumber().equals(personalIdentificationNumber))) { save.set(false);
                    throw new UseException(Activity.UPDATE_USER, UseExceptionType.USER_PERSONAL_ID_NOT_UNIQUE);}
                user.setPersonalIdentificationNumber(personalIdentificationNumber); }});
        }

    @Override
    @SneakyThrows
    public User inactivateUser(String userId)   {
        User user = usersRepository.getEntityById(userId).orElseThrow( () -> new UseException(Activity.UPDATE_USER, UseExceptionType.NOT_FOUND) );user.setActive(false);
        return usersRepository.save(user);
    }


    @Override
    public Optional<User> getUser(String userId) {
        Optional<User> user = usersRepository.getEntityById(userId);
        return user;
    }

    @Override
    public Stream<User> find(String searchString, Integer pageNumber, Integer pageSize, SortOrder sortOrder) {

        Stream<User> user = ListUtils.applyPage(usersRepository.all(), pageNumber, pageSize)
                .filter(user1 -> user1.getName().toLowerCase().contains(searchString) && user1.isActive());

        switch (sortOrder){
            case Name :{return user.sorted(Comparator.comparing(User::getName));}
            case PersonalId:{return user.sorted(Comparator.comparing(User::getPersonalIdentificationNumber));}
        }
        return user;
    }
}
