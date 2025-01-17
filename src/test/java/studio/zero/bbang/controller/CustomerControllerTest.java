package studio.zero.bbang.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.MethodArgumentNotValidException;
import studio.zero.bbang.dto.CustomerDTO;
import studio.zero.bbang.dto.JwtDTO;
import studio.zero.bbang.dto.LoginDTO;
import studio.zero.bbang.factory.CustomerTestDataFactory;
import studio.zero.bbang.model.Customer;
import studio.zero.bbang.service.CustomerService;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WithMockUser
@WebMvcTest(controllers = CustomerController.class)
class CustomerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CustomerService customerService;

    @Test
    void createCustomer() throws Exception {
        // given
        CustomerDTO customerDTO = CustomerTestDataFactory.createCustomerDTO();
        Customer customer = CustomerTestDataFactory.createCustomer();

        // when
        when(customerService.signUpCustomer(any(CustomerDTO.class))).thenReturn(customerDTO);

        // then
        mockMvc.perform(post("/customer/customers")
                        .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(customerDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nickname").value(customer.getNickname()))
                .andExpect(jsonPath("$.phone").value(customer.getPhone()));
    }

    @Test
    void createCustomerWithoutNickname() throws Exception {
        // given
        CustomerDTO invalidCustomerDTO = CustomerTestDataFactory.customerDTOWithoutNickname();

        // when & then
        mockMvc.perform(post("/customer/customers")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidCustomerDTO)))
                .andExpect(status().isBadRequest())
                .andExpect(result -> {
                    assertTrue(result.getResolvedException() instanceof MethodArgumentNotValidException);
                    assertTrue(result.getResolvedException().getMessage().contains("nickname cannot be null"));
                });
    }

    @ParameterizedTest
    @ValueSource(strings = {"short1", "thisaverylongpassword123", "123456789", "alphabets", "!!!!!!!!"})
    void failWhenPasswordOnlyContainsDigits() throws Exception {
        // given
        String password = "123456789";
        CustomerDTO invalidCustomerDTO = CustomerTestDataFactory.customerDTOAboutPassword(password);

        // when & then
        mockMvc.perform(post("/customer/customers")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidCustomerDTO)))
                .andExpect(status().isBadRequest())
                .andExpect(result -> {
                    assertTrue(result.getResolvedException() instanceof MethodArgumentNotValidException);
                    assertTrue(result.getResolvedException().getMessage().contains("Password must contain"));
                });
    }

    @Test
    void successCustomerLogin() throws Exception {
        // given
        String phoneNumber = "01012345678";
        String password = "encryptedPassword";
        String expectedAccessToken = "testAccessToken";
        String expectedRefreshToken = "testRefreshToken";

        LoginDTO loginDTO = new LoginDTO(phoneNumber, password);
        JwtDTO expectedJwtToken = new JwtDTO(expectedAccessToken, expectedRefreshToken);

        // when
        when(customerService.loginCustomer(any(LoginDTO.class))).thenReturn(expectedJwtToken);

        // then
        mockMvc.perform(post("/customer/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value(expectedJwtToken.getAccessToken()))
                .andExpect(jsonPath("$.refreshToken").value(expectedJwtToken.getRefreshToken()));
    }
}