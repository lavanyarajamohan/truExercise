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

    
//Combine Companies and officers
    public Object searchOfficerAndCompany(String companyNumber, String companyName) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("x-api-key", key);

            // Search for the company
            ResponseEntity<String> response = restTemplate.exchange(urlcompany + companyName, HttpMethod.GET, new HttpEntity<>(headers), String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                ObjectMapper objectMapper = new ObjectMapper();
                JsonNode rootNode = objectMapper.readTree(response.getBody());
                JsonNode itemsNode = rootNode.get("items");

                if (itemsNode != null && itemsNode.isArray()) {
                    List<ObjectNode> companyResults = new ArrayList<>();
                    for (JsonNode itemNode : itemsNode) {
                        JsonNode companyStatusNode = itemNode.get("company_status");
                        if (companyStatusNode != null && "active".equals(companyStatusNode.asText())) {
                            // If the company is active, fetch its officers
                            String companyNumber1 = itemNode.get("company_number").asText();
                            ObjectNode companyResult = objectMapper.createObjectNode();
                            companyResult.put("company_number", companyNumber1);
                            companyResult.put("company_type", itemNode.get("company_type").asText());
                            companyResult.put("title", itemNode.get("title").asText());
                            companyResult.put("company_status", itemNode.get("company_status").asText());
                            companyResult.put("date_of_creation", itemNode.get("date_of_creation").asText());
                            companyResult.set("address", itemNode.get("address"));

                            // Retrieve officers for the company
                            ResponseEntity<String> officersResponse = restTemplate.exchange(urlofficer + companyNumber, HttpMethod.GET, new HttpEntity<>(headers), String.class);
                            if (officersResponse.getStatusCode().is2xxSuccessful()) {
                                JsonNode officersRootNode = objectMapper.readTree(officersResponse.getBody());
                                JsonNode officersNode = officersRootNode.get("items");
                                if (officersNode != null && officersNode.isArray()) {
                                    List<ObjectNode> officersList = new ArrayList<>();
                                    for (JsonNode officerNode : officersNode) {
                                        if (!officerNode.has("resigned_on")) {
                                            ObjectNode officerResult = objectMapper.createObjectNode();
                                            officerResult.put("name", officerNode.get("name").asText());
                                            officerResult.put("officer_role", officerNode.get("officer_role").asText());
                                            officerResult.put("appointed_on", officerNode.get("appointed_on").asText());
                                            officerResult.set("address", officerNode.get("address"));
                                            officersList.add(officerResult);
                                        }
                                    }
                                    companyResult.set("officers", objectMapper.valueToTree(officersList));
                                }
                            }

                            companyResults.add(companyResult);
                        }
                    }
                    ObjectNode result = objectMapper.createObjectNode();
                    result.put("total_results", companyResults.size());
                    result.set("items", objectMapper.valueToTree(companyResults));
                    return result;
                } else {
                    return "No active companies found with the specified name";
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

