package com.tru.company.Service;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tru.company.Entity.Officer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


@Service
@Slf4j
public class CompanyService {

    @Autowired
    RestTemplate restTemplate;
    @Value("${api.key}")
    private String key;
    @Value("${urlofficer}")
    private String urlofficer;
    @Value("${urlcompany}")
    private String urlcompany;



    //Search for Company
    //only active companies should be returned
    public Object searchCompany(String companyName) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("x-api-key", key);

            ResponseEntity<String> response = restTemplate.exchange(urlcompany + companyName, HttpMethod.GET, new HttpEntity<>(headers), String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                ObjectMapper objectMapper = new ObjectMapper();
                JsonNode rootNode = objectMapper.readTree(response.getBody());
                JsonNode pageNode = rootNode.get("page_number");
                JsonNode kindNode = rootNode.get("kind");
                JsonNode totalResultsNode = rootNode.get("total_results");
                JsonNode itemsNode = rootNode.get("items");
                JsonNode displayNode;
                if (itemsNode != null && itemsNode.isArray()) {
                    List<JsonNode> activeCompanies = new ArrayList<>();
                    for (JsonNode itemNode : itemsNode) {
                        JsonNode companyStatusNode = itemNode.get("company_status");
                        if (companyStatusNode != null && "active".equals(companyStatusNode.asText())) {
                            //return itemNode;
                            activeCompanies.add(itemNode);
                        }
                        ObjectNode result = objectMapper.createObjectNode();
                        result.set("page_number", pageNode);
                        result.set("total_results", totalResultsNode);
                        result.set("kind", kindNode);
                        result.set("items", objectMapper.valueToTree(activeCompanies));
                        return result;
                    }
                    return "No active companies found with the specified name";
                } else {
                    return "Company status not found in the response";
                }
            } else {
                return "Failed to retrieve company information: " + response.getStatusCode().toString();
            }

        } catch (HttpStatusCodeException e) {
            // Handle specific HTTP status code errors
            return "HTTP Error: " + e.getStatusCode() + " - " + e.getStatusText();
        } catch (IOException e) {
            // Handle JSON parsing errors
            log.error("Failed to parse JSON response: " + e.getMessage());
            return "Failed to parse JSON response";
        } catch (Exception e) {
            // Handle other exceptions
            log.error("An error occurred while processing the request: " + e.getMessage());
            return "An error occurred while processing the request";
        }
    }
    //Get Company Officers
    //Only include officers that are active (resigned_on is not present in that case)
    public Object getCompanyOfficers(String companyNumber) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("x-api-key", key);
            ObjectMapper objectMapper = new ObjectMapper();
            ResponseEntity<String> response = restTemplate.exchange(urlofficer + companyNumber, HttpMethod.GET, new HttpEntity<>(headers), String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                JsonNode rootNode = objectMapper.readTree(response.getBody());
                List<JsonNode> activeOfficers = new ArrayList<>();
                List<Officer> officersField = new ArrayList<>();

                JsonNode itemNode = rootNode.get("items");

                if (itemNode != null && itemNode.isArray()) {

                    for (JsonNode itemNodes : itemNode) {
                        if (!itemNodes.has("resigned_on")) {
                            Officer officer = new Officer();
                            officer.setName(itemNodes.get("name").asText());
                            officer.setAppointedOn(itemNodes.get("appointed_on").asText());
                            officer.setOfficerRole(itemNodes.get("officer_role").asText());
                            officersField.add(officer);

                            activeOfficers.add(itemNodes);
                        }

                    }
                }
                ObjectNode result = objectMapper.createObjectNode();
                result.set("items", objectMapper.valueToTree(activeOfficers));
                result.set("officers", objectMapper.valueToTree(officersField));
                return result;
            } else {
                throw new RuntimeException("Failed to retrieve company officers: " + response.getStatusCode().toString());
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to parse JSON response: " + e.getMessage());
        }
    }
}

