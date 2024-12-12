package studio.zero.bbang.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import studio.zero.bbang.dto.CustomerDTO;
import studio.zero.bbang.factory.CustomerTestDataFactory;
import studio.zero.bbang.mapper.CustomerMapper;
import studio.zero.bbang.model.Customer;
import studio.zero.bbang.repository.CustomerRepository;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

    @InjectMocks
    private CustomerService customerService;
    @Mock
    private CustomerRepository customerRepository;
    @Spy
    private CustomerMapper customerMapper;
    @Spy
    private BCryptPasswordEncoder passwordEncoder;

    @Test
    void saveAndReturnUser() {
        // given
        Customer customer = CustomerTestDataFactory.createCustomer();
        CustomerDTO customerDTO = CustomerTestDataFactory.createCustomerDTO();

        when(customerMapper.dtoToCustomer(any(CustomerDTO.class))).thenReturn(customer);
        when(customerMapper.customerToDto(any(Customer.class))).thenReturn(customerDTO);
        when(customerRepository.save(any(Customer.class))).thenReturn(customer);

        // when
        CustomerDTO savedCustomerDTO = customerService.signUpCustomer(customerDTO);

        // then
        assertNotNull(savedCustomerDTO);
        assertEquals(customer.getNickname(), savedCustomerDTO.getNickname());
        assertEquals(customer.getPhone(), savedCustomerDTO.getPhone());
    }

    @Test
    void failWhenSaveDuplicatedPhone() {
        // given
        Customer customer = CustomerTestDataFactory.createCustomer();
        CustomerDTO customerDTO = CustomerTestDataFactory.createCustomerDTO();

        // when
        when(customerRepository.existsByPhone(customer.getPhone())).thenReturn(true);

        // then
        assertThrows(IllegalArgumentException.class,
                () -> customerService.signUpCustomer(customerDTO),
                        "phone number already exists");
    }

    @Test
    void failWhenSaveWithoutPhoneAuthenticated() {
        // given
        CustomerDTO customerDTO = CustomerTestDataFactory.customerDTOAboutAuthenticated(null);

        // when & then
        assertThrows(IllegalArgumentException.class,
                () -> customerService.signUpCustomer(customerDTO),
                "phone number must be authenticated");
    }

    @Test
    void failWhenSaveWithFalsePhoneAuthenticated() {
        // given
        CustomerDTO customerDTO = CustomerTestDataFactory.customerDTOAboutAuthenticated(false);

        // when & then
        assertThrows(IllegalArgumentException.class,
                () -> customerService.signUpCustomer(customerDTO),
                "phone number must be authenticated");
    }
}