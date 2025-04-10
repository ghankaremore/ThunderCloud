package com.example.ec2launcher.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.List;

@Service
public class EC2Service {

//    @Value("${aws.accessKeyId}")
    private String accessKeyId;

//    @Value("${aws.secretAccessKey}")
    private String secretAccessKey;

    public String getAccessKeyId() {
        return accessKeyId;
    }

    public void setAccessKeyId(String accessKeyId) {
        this.accessKeyId = accessKeyId;
    }

    public String getSecretAccessKey() {
        return secretAccessKey;
    }

    public void setSecretAccessKey(String secretAccessKey) {
        this.secretAccessKey = secretAccessKey;
    }

    @Value("${aws.region}")
    private String region;

    private Ec2Client ec2;

    private String lastCreatedSecurityGroupId;
    private String lastCreatedInstanceId;

    private String lastCreatedInstancePublicIp;


    private void initializeClient() {
        if (ec2 == null) {
            ec2 = Ec2Client.builder()
                    .region(Region.of(region))
                    .credentialsProvider(StaticCredentialsProvider.create(
                            AwsBasicCredentials.create(accessKeyId, secretAccessKey)
                    ))
                    .build();
        }
    }

    public String launchEC2WithSecurityAndKey() {
        initializeClient();

        String keyName = "springboot-key";
        String sgName = "springboot-sg";

        try {
            // 1. Create Key Pair
            // 1. Check if key pair exists
            try {
                DescribeKeyPairsRequest describeKeyPairsRequest = DescribeKeyPairsRequest.builder()
                        .keyNames(keyName).build();
                ec2.describeKeyPairs(describeKeyPairsRequest);
                System.out.println("Key pair already exists. Skipping creation.");
            } catch (Ec2Exception e) {
                if (e.awsErrorDetails().errorCode().equals("InvalidKeyPair.NotFound")) {
                    System.out.println("Key pair not found. Creating a new one...");
                    CreateKeyPairRequest keyRequest = CreateKeyPairRequest.builder().keyName(keyName).build();
                    CreateKeyPairResponse keyResponse = ec2.createKeyPair(keyRequest);
                    String privateKey = keyResponse.keyMaterial();
                    Files.write(Paths.get(keyName + ".pem"), privateKey.getBytes());
                } else {
                    throw e; // rethrow other unexpected errors
                }
            }


            // 2. Check if Security Group exists, else create it
            String groupId;
            try {
                DescribeSecurityGroupsRequest describeRequest = DescribeSecurityGroupsRequest.builder()
                        .groupNames(sgName).build();
                DescribeSecurityGroupsResponse describeResponse = ec2.describeSecurityGroups(describeRequest);
                groupId = describeResponse.securityGroups().get(0).groupId();
                System.out.println("Security group already exists. Reusing group ID: " + groupId);
            } catch (Ec2Exception e) {
                if (e.awsErrorDetails().errorCode().equals("InvalidGroup.NotFound")) {
                    System.out.println("Security group not found. Creating new security group...");
                    CreateSecurityGroupRequest sgRequest = CreateSecurityGroupRequest.builder()
                            .groupName(sgName)
                            .description("Spring Boot SG")
                            .build();
                    CreateSecurityGroupResponse sgResponse = ec2.createSecurityGroup(sgRequest);
                    groupId = sgResponse.groupId();
                } else {
                    throw e;
                }
            }
            this.lastCreatedSecurityGroupId = groupId;

            // 3. Authorize SSH and 8080 port
            addSecurityGroupRule(groupId, 22);
            // 4. Launch EC2 Instance
//            RunInstancesRequest runRequest = RunInstancesRequest.builder()
//                    .imageId("ami-0c02fb55956c7d316")
//                    .instanceType(InstanceType.T2_MICRO)
//                    .keyName(keyName)
//                    .securityGroupIds(groupId)
//                    .minCount(1)
//                    .maxCount(1)
//                    .build();
//            RunInstancesResponse runResponse = ec2.runInstances(runRequest);
//            String instanceId = runResponse.instances().get(0).instanceId();
//            this.lastCreatedInstanceId = instanceId;
            String userDataScript = Base64.getEncoder().encodeToString((
                    "#!/bin/bash\n" +
                            "amazon-linux-extras install docker -y\n" +
                            "service docker start\n" +
                            "usermod -a -G docker ec2-user\n" +
                            "curl -L \"https://github.com/docker/compose/releases/download/1.29.2/docker-compose-$(uname -s)-$(uname -m)\" " +
                            "-o /usr/local/bin/docker-compose\n" +
                            "chmod +x /usr/local/bin/docker-compose\n"
            ).getBytes());

            RunInstancesRequest runRequest = RunInstancesRequest.builder()
                    .imageId("ami-0c02fb55956c7d316")
                    .instanceType(InstanceType.T2_MICRO)
                    .keyName(keyName)
                    .securityGroupIds(groupId)
                    .minCount(1)
                    .maxCount(1)
                    .userData(userDataScript)
                    .build();

            RunInstancesResponse runResponse = ec2.runInstances(runRequest);
            String instanceId = runResponse.instances().get(0).instanceId();
            this.lastCreatedInstanceId = instanceId;


            // 5. Get public IP
            DescribeInstancesRequest descReq = DescribeInstancesRequest.builder()
                    .instanceIds(instanceId).build();

            String publicIp = null;
            while (publicIp == null) {
                DescribeInstancesResponse descResp = ec2.describeInstances(descReq);
                List<Reservation> reservations = descResp.reservations();
                publicIp = reservations.get(0).instances().get(0).publicIpAddress();
                Thread.sleep(5000);
            }
            this.lastCreatedInstancePublicIp = publicIp;

            return "✅ EC2 Instance Launched!\nInstance ID: " + instanceId + "\nPublic IP: " + publicIp;

        } catch (Ec2Exception | IOException | InterruptedException e) {
            e.printStackTrace();
            return "❌ Error: " + e.getMessage();
        }
    }

    public String getLastCreatedInstancePublicIp() {
        return this.lastCreatedInstancePublicIp;
    }

//    public String addSecurityGroupRule(String groupId, int port, String protocol) {
//        initializeClient();
//
//        try {
//            IpPermission permission = IpPermission.builder()
//                    .ipProtocol(protocol)
//                    .fromPort(port)
//                    .toPort(port)
//                    .ipRanges(IpRange.builder().cidrIp("0.0.0.0/0").build())
//                    .build();
//
//            AuthorizeSecurityGroupIngressRequest ingressRequest = AuthorizeSecurityGroupIngressRequest.builder()
//                    .groupId(groupId)
//                    .ipPermissions(permission)
//                    .build();
//
//            ec2.authorizeSecurityGroupIngress(ingressRequest);
//            return "✅ Rule added to security group " + groupId + ": port " + port + ", protocol " + protocol;
//        } catch (Ec2Exception e) {
//            e.printStackTrace();
//            return "❌ Failed to add rule: " + e.awsErrorDetails().errorMessage();
//        }



//    }

    public String addSecurityGroupRule(String groupId, int port) {
        initializeClient();

        try {
            // Automatically using TCP protocol
            String protocol = "tcp";

            IpPermission permission = IpPermission.builder()
                    .ipProtocol(protocol)
                    .fromPort(port)
                    .toPort(port)
                    .ipRanges(IpRange.builder().cidrIp("0.0.0.0/0").build())
                    .build();

            AuthorizeSecurityGroupIngressRequest ingressRequest = AuthorizeSecurityGroupIngressRequest.builder()
                    .groupId(groupId)
                    .ipPermissions(permission)
                    .build();

            ec2.authorizeSecurityGroupIngress(ingressRequest);
            return "✅ Rule added to security group " + groupId + ": port " + port + ", protocol " + protocol;
        } catch (Ec2Exception e) {
            e.printStackTrace();
            return "❌ Failed to add rule: " + e.awsErrorDetails().errorMessage();
        }
    }

    public String addSecurityGroupRuleToLastCreated(int port) {
        if (lastCreatedSecurityGroupId == null) {
            return "❌ No security group has been created yet.";
        }
        return addSecurityGroupRule(lastCreatedSecurityGroupId, port);
    }
}
