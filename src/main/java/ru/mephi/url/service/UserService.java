package ru.mephi.url.service;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.mephi.url.model.User;
import ru.mephi.url.repository.UserRepository;

import java.util.UUID;

@Service
public class UserService {
    private final UserRepository userRepository;

    @Autowired
    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User getUserById(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Пользователь с ID " + id + " не найден"));
    }

    @Transactional
    public User createUser() {
        User user = new User();
        user.setId(getUniqueRandomId());
        userRepository.save(user);
        return user;
    }

    private UUID getUniqueRandomId() {
        UUID id;
        do {
            id = UUID.randomUUID();
        } while (userRepository.existsById(id)); // На случай коллизии (очень маловероятно)
        return id;
    }
}
