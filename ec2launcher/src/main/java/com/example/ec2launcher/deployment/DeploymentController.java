package com.example.ec2launcher.deployment;

import com.example.ec2launcher.service.EC2Service;
import com.jcraft.jsch.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/deploy")
public class DeploymentController {

    @Autowired
    private DeploymentService deploymentService;

    @Autowired
    private EC2Service ec2Service;


    @PostMapping("/upload")
    public String uploadZips(@RequestParam("frontend") MultipartFile frontendZip,
                             @RequestParam("backend") MultipartFile backendZip,
                             @RequestParam("port") int appPort) {
        return deploymentService.saveZipsToDatabase(frontendZip, backendZip, appPort);
    }


    @PostMapping("/generate-docker")
    public String generateDockerFiles() {
        return deploymentService.generateDockerAndComposeForLatest();
    }


    @PostMapping("/build-projects")
    public String buildFrontendAndBackend() {
        return deploymentService.buildLatestFrontendAndBackend();
    }

    @PostMapping("/push-to-docker")
    public String pushToDocker() {
        return deploymentService.buildAndPushDockerImage();
    }

    @PostMapping("/Dockercredentials")
    public void  getDockerCredentials(@RequestParam String dockerUsername,
                               @RequestParam String dockerPassword) {
        this.deploymentService.setDockerHubUsername(dockerUsername);
        this.deploymentService.setDockerHubPassword(dockerPassword);
    }


    @PostMapping("/prepare-bundle")
    public String prepareDeploymentBundle() {
        File bundle = deploymentService.prepareDeploymentBundle();
        if (bundle != null && bundle.exists()) {
            return "✅ Deployment bundle prepared at: " + bundle.getAbsolutePath();
        } else {
            return "❌ Failed to prepare deployment bundle.";
        }
    }



    @PostMapping("/transfer-bundle")
    public String transferDeploymentBundle() {
        String ec2Ip = ec2Service.getLastCreatedInstancePublicIp();
        return deploymentService.transferDeploymentBundleToEC2(ec2Ip);
    }

//    private static final String PRIVATE_KEY_PATH = "D:/ec2launcher/springboot-key.pem";
//    private static final String USER = "ec2-user"; // use "ubuntu" for Ubuntu AMIs
//    private static final int PORT = 22;
//    private static final String LOCAL_FOLDER_PATH = "D:/ec2launcher/deployment-bundles/project-70";
//    private static final String REMOTE_BASE_PATH = "/home/ec2-user/project-70-upload"; // change if needed

    @GetMapping("/upload-project")
    public String uploadProjectFolder() {
        String output = deploymentService.uploadProjectFolder();
        return output;
    }


    @GetMapping("/deploy-latest")
    public String deployToEC2() {
        String output = deploymentService.deployToEC2();

        return output;
    }





}
