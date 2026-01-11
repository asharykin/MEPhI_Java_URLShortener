package ru.mephi.url.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.mephi.url.model.User;

import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
}
