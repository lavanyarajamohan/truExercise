package com.tru.company.Controller;

import com.tru.company.Entity.Company;
import com.tru.company.Service.CompanyService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class CompanyController {

    private final CompanyService companyService;

    public CompanyController(CompanyService companyService) {
        this.companyService = companyService;
    }

    @RequestMapping("/getCompanyOfficers")
    public ResponseEntity<?> getOfficer(@RequestBody Company company) {
        if (company.getCompanyNumber() != null) {
            return ResponseEntity.ok(companyService.getCompanyOfficers(company.getCompanyNumber()));
        }else {
            return ResponseEntity.badRequest().body("Either companyName or companyNumber must be provided.");
        }
    }

    @RequestMapping("/searchForCompany")
    public ResponseEntity<?> getCompany(@RequestBody Company company) {
        if (company.getCompanyName()!= null) {
            return ResponseEntity.ok(companyService.searchCompany(company.getCompanyName()));
        }
        else {
            return ResponseEntity.badRequest().body("Either companyName or companyNumber must be provided.");
        }
    }

}

