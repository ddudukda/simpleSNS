package com.example.sns.service;

import com.example.sns.exception.ErrorCode;
import com.example.sns.exception.SnsApplicationException;
import com.example.sns.model.Alarm;
import com.example.sns.model.User;
import com.example.sns.model.entity.UserEntity;
import com.example.sns.repository.AlarmEntityRepository;
import com.example.sns.repository.UserEntityRepository;
import com.example.sns.util.JwtTokenUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserEntityRepository userEntityRepository;
    private final AlarmEntityRepository alarmEntityRepository;
    private final BCryptPasswordEncoder encoder;

    @Value("${jwt.secret-key}")
    private String secretKey;

    @Value("${jwt.token.expired-time-ms}")
    private Long expiredTimeMs;

    public User loadUserByUserName(String userName) throws UsernameNotFoundException {
        return userEntityRepository.findByUserName(userName).map(User::fromEntity).orElseThrow(
                () -> new SnsApplicationException(ErrorCode.USER_NOT_FOUND, String.format("%s not found" , userName))
        );
    }

    @Transactional
    public User join(String userName, String password){
        //회원가입하려는 유저네임으로 회원가입된 유저가 있는지
        userEntityRepository.findByUserName(userName).ifPresent(it -> {
            throw new SnsApplicationException(ErrorCode.DUPLICATED_USER_NAME, String.format("%s is duplicated", userName));
        });
        // 회원가입 진행 = user등록
        UserEntity saveUser = userEntityRepository.save(UserEntity.of(userName, encoder.encode(password)));
        return User.fromEntity(saveUser);
    }

    // TODO: implement
    public String login(String userName, String password){
        //회원가입 여부 체크
        UserEntity userEntity = userEntityRepository.findByUserName(userName).orElseThrow(() ->
                new SnsApplicationException(ErrorCode.USER_NOT_FOUND, String.format("%s is not founded", userName)));

        //비밀번호 체크
        if(!encoder.matches(password, userEntity.getPassword())){
        //if(!userEntity.getPassword().equals(password)){
            throw new SnsApplicationException(ErrorCode.INVALID_PASSWORD);
        }
        //토큰생성
        String token = JwtTokenUtils.generateToken(userName, secretKey, expiredTimeMs);
        return token;
    }
    // TODO : alarm return
    public Page<Alarm> alarmList(String userName, Pageable pageable) {
        UserEntity userEntity = userEntityRepository.findByUserName(userName).orElseThrow(()
                -> new SnsApplicationException(ErrorCode.USER_NOT_FOUND, String.format("%s not founded", userName)));

        return alarmEntityRepository.findAllByUser(userEntity, pageable).map(Alarm::fromEntity);
    }
}
