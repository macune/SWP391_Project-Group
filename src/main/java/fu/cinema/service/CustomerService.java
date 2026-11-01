package fu.cinema.service;

import fu.cinema.dto.request.*;
import fu.cinema.dto.response.CustomerResponse;
import fu.cinema.entity.Customer;
import fu.cinema.enums.AccountStatus;

import java.util.List;

public interface CustomerService {

    Customer register(CustomerCreationRequest request);
    void verifyEmail(String token);

    void forgotPassword(ForgotPasswordRequest request);

    void validateResetPasswordToken(String token);

    void resetPassword(ResetPasswordRequest resetPasswordRequest);

    CustomerRequest getCustomerProfile(String username);

    void updateCustomerProfile(String username, CustomerRequest request);

    void changePassword(String username, ChangePasswordRequest request);
}
