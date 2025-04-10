package com.example.ec2launcher.controller;

import com.example.ec2launcher.service.EC2Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/ec2")
public class EC2Controller {

    @Autowired
    private EC2Service ec2Service;


    @GetMapping("/set-aws-credentials")
    public String setAwsCredentials(
            @RequestParam String accessKeyId,
            @RequestParam String secretAccessKey
    ) {
        ec2Service.setAccessKeyId(accessKeyId);
        ec2Service.setSecretAccessKey(secretAccessKey);
        return "✅ AWS credentials updated successfully.";
    }

    @GetMapping("/launch")
    public String launchInstance() {
        return ec2Service.launchEC2WithSecurityAndKey();
    }

    @PostMapping("/add-rule")
    public String addSecurityGroupRule(@RequestParam int port) {
        return ec2Service.addSecurityGroupRuleToLastCreated(port);
    }


}
