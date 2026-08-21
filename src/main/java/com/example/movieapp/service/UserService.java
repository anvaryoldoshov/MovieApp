package com.example.movieapp.service;

import com.example.movieapp.dto.UserDto;
import com.example.movieapp.entities.User;
import com.example.movieapp.enums.Role;
import com.example.movieapp.exception.InvalidCredentialsException;
import com.example.movieapp.exception.UserNotFoundException;
import com.example.movieapp.mapper.UserMapper;
import com.example.movieapp.repository.MovieAccessRepository;
import com.example.movieapp.repository.PaymentRepository;
import com.example.movieapp.repository.RefreshTokenRepository;
import com.example.movieapp.repository.UserDeviceRepository;
import com.example.movieapp.repository.UserRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepo userRepo;
    private final UserMapper userMapper;
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserDeviceRepository userDeviceRepository;
    private final MovieAccessRepository movieAccessRepository;
    private final PaymentRepository paymentRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    public User getUserByEmail(String email) {
        Optional<User> byEmail = userRepo.findByEmail(email);
        return byEmail.orElse(null);
    }

    public ResponseEntity<UserDto> updateUser(Long id, UserDto userDto) {
        Optional<User> optionalUser = userRepo.findById(id);
        if (!optionalUser.isPresent()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }

        User user = optionalUser.get();
        user.setUsername(userDto.getUsername());
        user.setEmail(userDto.getEmail());

        User updatedUser = userRepo.save(user);

        return ResponseEntity.ok(userMapper.toDto(updatedUser));
    }

    public ResponseEntity<List<UserDto>> getAllUsers() {
        List<User> users = userRepo.findAll();
        List<UserDto> userDtos = users.stream()
                .map(userMapper::toDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(userDtos);
    }

    public ResponseEntity<UserDto> getUserById(long id) {
        User user = userRepo.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        UserDto dto = userMapper.toDto(user);
        return ResponseEntity.ok().body(dto);
    }

    public ResponseEntity<?> makeAdmin(Long userId) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User topilmadi"));
        user.setRole(Role.ADMIN);
        userRepo.save(user);
        return ResponseEntity.ok("Foydalanuvchi admin qilindi");
    }

    @Transactional
    public void deleteAccountByEmail(String email) {
        User user = userRepo.findByEmail(email).orElseThrow(UserNotFoundException::new);
        deleteAccountData(user);
    }

    @Transactional
    public void deleteAccountByEmailAndPassword(String email, String password) {
        User user = userRepo.findByEmail(email).orElseThrow(UserNotFoundException::new);
        if (user.getPassword() == null || !passwordEncoder.matches(password, user.getPassword())) {
            throw new InvalidCredentialsException();
        }
        deleteAccountData(user);
    }

    private void deleteAccountData(User user) {
        refreshTokenRepository.deleteByUser(user);
        userDeviceRepository.deleteByUser(user);
        movieAccessRepository.deleteByUserId(user.getId());
        paymentRepository.deleteByUserId(user.getId());
        userRepo.delete(user);
    }
}
