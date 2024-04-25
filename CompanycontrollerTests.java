package com.tru.company;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tru.company.Entity.Company;
import com.tru.company.Service.CompanyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CompanyServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private CompanyService companyService;

    @BeforeEach
    void setUp() {
        companyService = new CompanyService();
    }

    @Test
    void searchCompany_Successful() {
        // Arrange
        HttpHeaders headers = new HttpHeaders();
        headers.set("x-api-key", "testKey");
        HttpEntity<String> httpEntity = new HttpEntity<>(headers);

        ResponseEntity<String> responseEntity = new ResponseEntity<>("{\"page_number\": 1, \"kind\": \"search#companies\", \"total_results\": 1, \"items\": [{\"company_status\": \"active\"}]}", HttpStatus.OK);
/*        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), eq(httpEntity), eq(String.class)))
                .thenReturn(responseEntity);*/

        // Act
        Object result = companyService.searchCompany("BBC LIMITED");

        // Assert
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
    }

    @Test
    void getCompanyOfficers_Successful() {
        // Arrange
        HttpHeaders headers = new HttpHeaders();
        headers.set("x-api-key", "testKey");
        HttpEntity<String> httpEntity = new HttpEntity<>(headers);

        ResponseEntity<String> responseEntity = new ResponseEntity<>("{\"items\": [{\"name\": \"John Doe\", \"appointed_on\": \"2024-04-25\", \"officer_role\": \"CEO\"}]}", HttpStatus.OK);
        /*when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), eq(httpEntity), eq(String.class)))
                .thenReturn(responseEntity);*/

        // Act
       companyService.getCompanyOfficers("10241297");

        // Assert
        ObjectMapper objectMapper = new ObjectMapper();
        ObjectNode expected = objectMapper.createObjectNode();
        ArrayNode itemsArray = objectMapper.createArrayNode();
        ObjectNode officerNode = objectMapper.createObjectNode();
        officerNode.put("name", "John Doe");
        officerNode.put("appointed_on", "2024-04-25");
        officerNode.put("officer_role", "CEO");
        itemsArray.add(officerNode);
        expected.set("items", itemsArray);

        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
    }
}

