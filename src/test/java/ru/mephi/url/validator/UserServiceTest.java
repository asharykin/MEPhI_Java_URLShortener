package ru.mephi.url.validator;

import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.mephi.url.model.User;
import ru.mephi.url.repository.UserRepository;
import ru.mephi.url.service.UserService;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    @Test
    @DisplayName("Получение пользователя: Успешный возврат при наличии в БД")
    void getUserById_Success() {
        UUID userId = UUID.randomUUID();
        User expectedUser = new User();
        expectedUser.setId(userId);

        when(userRepository.findById(userId)).thenReturn(Optional.of(expectedUser));
        User result = userService.getUserById(userId);

        assertNotNull(result);
        assertEquals(userId, result.getId());
        verify(userRepository).findById(userId);
    }

    @Test
    @DisplayName("Получение пользователя: Исключение, если пользователь не найден")
    void getUserById_NotFound_ThrowsException() {
        UUID userId = UUID.randomUUID();

        when(userRepository.findById(userId)).thenReturn(Optional.empty());
        EntityNotFoundException ex = assertThrows(EntityNotFoundException.class, () -> userService.getUserById(userId));

        assertTrue(ex.getMessage().contains("не найден"));
        verify(userRepository).findById(userId);
    }

    @Test
    @DisplayName("Создание пользователя: Успешная генерация и сохранение")
    void createUser_Success() {
        when(userRepository.existsById(any(UUID.class))).thenReturn(false);
        User result = userService.createUser();

        assertNotNull(result);
        assertNotNull(result.getId());
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Создание пользователя: Обработка коллизии UUID")
    void createUser_WithCollision_ShouldRetry() {
        when(userRepository.existsById(any(UUID.class))).thenReturn(true).thenReturn(false);
        User result = userService.createUser();

        assertNotNull(result);
        verify(userRepository, times(2)).existsById(any(UUID.class));
        verify(userRepository, times(1)).save(any(User.class));
    }
}
