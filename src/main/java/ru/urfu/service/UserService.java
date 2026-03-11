package ru.urfu.service;


import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import ru.urfu.dto.UserDto;
import ru.urfu.entity.User;

public interface UserService {

    void saveUser(UserDto userDto);

    void deleteUser(Long userId);



    User findUserByEmail(String email);

    void deleteUserById(Long id);

    UserDetails loadUserByUsername(String email) throws UsernameNotFoundException;

//
//    List<SellerDto> findAllUsers();
//    SellerDto userToUserDto(Seller user);
}
