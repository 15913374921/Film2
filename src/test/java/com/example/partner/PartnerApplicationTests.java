package com.example.partner;

import com.example.partner.service.IUserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestExecutionListeners;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

@SpringBootTest
class PartnerApplicationTests {

    @Autowired
    IUserService userService;

    @Test
    void contextLoads(){
        userService.removeById(5);
        System.out.println(userService.getById(5));
    }

}
