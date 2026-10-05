package fu.cinema.service;

import fu.cinema.dto.request.CustomerCreationRequest;
import fu.cinema.entity.Customer;

public interface CustomerService {

    Customer register(CustomerCreationRequest request);
    void verifyEmail(String token);
}
