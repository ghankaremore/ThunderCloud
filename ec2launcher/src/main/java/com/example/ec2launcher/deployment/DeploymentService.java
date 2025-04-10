package com.example.ec2launcher.deployment;

import com.example.ec2launcher.Ec2launcherApplication;
import com.example.ec2launcher.deployment.model.ProjectArchive;
import com.example.ec2launcher.deployment.repository.ProjectArchiveRepository;
import com.example.ec2launcher.service.EC2Service;
import com.jcraft.jsch.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.Files;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Service
public class DeploymentService {

    @Autowired
    private ProjectArchiveRepository repository;


    private EC2Service ec2Service;
    private String latestip;

    @Autowired
    public DeploymentService(EC2Service ec2Service) {
        this.ec2Service = ec2Service;
      this.latestip  =   ec2Service.getLastCreatedInstancePublicIp();
    }


    private Long lastUploadedProjectId;


    public String dockerHubUsername = null;
    public String dockerHubPassword = null;

    public void setDockerHubUsername(String dockerHubUsername) {
        this.dockerHubUsername = dockerHubUsername;
    }

    public void setDockerHubPassword(String dockerHubPassword) {
        this.dockerHubPassword = dockerHubPassword;
    }

    public String saveZipsToDatabase(MultipartFile frontend, MultipartFile backend, int port) {
        try {
            ProjectArchive archive = new ProjectArchive();
            archive.setAppPort(port);
            archive.setFrontendZip(frontend.getBytes());
            archive.setBackendZip(backend.getBytes());
            ProjectArchive saved = repository.save(archive);
            lastUploadedProjectId = saved.getId();
            return "✅ Project files uploaded and stored in database with ID: " + saved.getId();
        } catch (Exception e) {
            e.printStackTrace();
            return "❌ Error storing files: " + e.getMessage();
        }
    }

    public String generateDockerAndComposeForLatest() {
        if (lastUploadedProjectId == null) {
            return "❌ No project uploaded yet.";
        }
        return generateDockerAndCompose(lastUploadedProjectId);
    }

//    public String generateDockerAndCompose(Long projectId) {
//        try {
//            ProjectArchive archive = repository.findById(projectId).orElseThrow(() -> new RuntimeException("Project not found"));
//
//            File projectDir = new File("./projects/project-" + projectId);
//            projectDir.mkdirs();
//
//            boolean hasFrontend = archive.getFrontendZip() != null && archive.getFrontendZip().length > 0;
//            boolean hasBackend = archive.getBackendZip() != null && archive.getBackendZip().length > 0;
//
//            if (hasFrontend) {
//                File frontendDir = new File(projectDir, "frontend");
//                frontendDir.mkdirs();
//                unzipBytesToDir(archive.getFrontendZip(), frontendDir);
//
//                File dockerfile = new File(frontendDir, "Dockerfile");
//                Files.writeString(dockerfile.toPath(),
//                        "FROM node:18-alpine\n" +
//                                "WORKDIR /app\n" +
//                                "COPY . .\n" +
//                                "RUN npm install -g @angular/cli && npm install && ng build --configuration=production\n" +
//                                "EXPOSE 80\n" +
//                                "CMD [\"npx\", \"http-server\", \"dist\"]\n");
//            }
//
//            if (hasBackend) {
//                File backendDir = new File(projectDir, "backend");
//                backendDir.mkdirs();
//                unzipBytesToDir(archive.getBackendZip(), backendDir);
//
//                File dockerfile = new File(backendDir, "Dockerfile");
//                Files.writeString(dockerfile.toPath(),
//                        "FROM eclipse-temurin:17-jdk\n" +
//                                "WORKDIR /app\n" +
//                                "COPY target/*.jar app.jar\n" +
//                                "EXPOSE " + archive.getAppPort() + "\n" +
//                                "ENTRYPOINT [\"java\", \"-jar\", \"app.jar\"]\n");
//            }
//
//            File composeFile = new File(projectDir, "docker-compose.yml");
//            StringBuilder compose = new StringBuilder("version: '3'\nservices:\n");
//
//            if (hasFrontend) {
//                compose.append("  frontend:\n")
//                        .append("    build: ./frontend\n")
//                        .append("    ports:\n")
//                        .append("      - \"4200:80\"\n");
//            }
//
//            if (hasBackend) {
//                compose.append("  backend:\n")
//                        .append("    build: ./backend\n")
//                        .append("    ports:\n")
//                        .append("      - \"" + archive.getAppPort() + ":" + archive.getAppPort() + "\"\n");
//            }
//
//            Files.writeString(composeFile.toPath(), compose);
//
//            return "✅ Dockerfile and docker-compose.yml generated successfully.";
//
//        } catch (Exception e) {
//            e.printStackTrace();
//            return "❌ Error generating Docker files: " + e.getMessage();
//        }
//    }

//    public String generateDockerAndCompose(Long projectId) {
//        try {
//            ProjectArchive archive = repository.findById(projectId).orElseThrow(() -> new RuntimeException("Project not found"));
//
//            File projectDir = new File("./projects/project-" + projectId);
//            projectDir.mkdirs();
//
//            boolean hasFrontend = archive.getFrontendZip() != null && archive.getFrontendZip().length > 0;
//            boolean hasBackend = archive.getBackendZip() != null && archive.getBackendZip().length > 0;
//
//            File frontendDir = null;
//            File backendDir = null;
//
//            if (hasFrontend) {
//                frontendDir = new File(projectDir, "frontend");
//                frontendDir.mkdirs();
//                unzipBytesToDir(archive.getFrontendZip(), frontendDir);
//
//                File dockerfile = new File(frontendDir, "Dockerfile");
//                Files.writeString(dockerfile.toPath(),
//                        "FROM node:18-alpine\n" +
//                                "WORKDIR /app\n" +
//                                "COPY . .\n" +
//                                "RUN npm install -g @angular/cli && npm install && ng build --configuration=production\n" +
//                                "EXPOSE 80\n" +
//                                "CMD [\"npx\", \"http-server\", \"dist\"]\n");
//            }
//
//            if (hasBackend) {
//                backendDir = new File(projectDir, "backend");
//                backendDir.mkdirs();
//                unzipBytesToDir(archive.getBackendZip(), backendDir);
//
//                // Find the nested directory with pom.xml to determine jar path
//                File nestedBackendDir = findDirectoryWithFile(backendDir, "pom.xml");
//                String relativeJarPath = "target/*.jar";
//                if (nestedBackendDir != null && !nestedBackendDir.equals(backendDir)) {
//                    String relativePath = backendDir.toPath().relativize(nestedBackendDir.toPath()).toString().replace("\\", "/");
//                    relativeJarPath = relativePath + "/target/*.jar";
//                }
//
//                File dockerfile = new File(backendDir, "Dockerfile");
//                Files.writeString(dockerfile.toPath(),
//                        "FROM eclipse-temurin:17-jdk\n" +
//                                "WORKDIR /app\n" +
//                                "COPY " + relativeJarPath + " app.jar\n" +
//                                "EXPOSE " + archive.getAppPort() + "\n" +
//                                "ENTRYPOINT [\"java\", \"-jar\", \"app.jar\"]\n");
//            }
//
//            File composeFile = new File(projectDir, "docker-compose.yml");
//            StringBuilder compose = new StringBuilder("version: '3.8'\nservices:\n");
//
//            if (hasFrontend && frontendDir != null) {
//                compose.append("  frontend:\n")
//                        .append("    build:\n")
//                        .append("      context: ./frontend\n")
//                        .append("    image: your-dockerhub-user/project-" + projectId + "-frontend\n")
//                        .append("    ports:\n")
//                        .append("      - \"4200:80\"\n")
//                        .append("    networks:\n")
//                        .append("      - project-network\n");
//            }
//
//            if (hasBackend && backendDir != null) {
//                compose.append("  backend:\n")
//                        .append("    build:\n")
//                        .append("      context: ./backend\n")
//                        .append("    image: your-dockerhub-user/project-" + projectId + "-backend\n")
//                        .append("    ports:\n")
//                        .append("      - \"" + archive.getAppPort() + ":" + archive.getAppPort() + "\"\n")
//                        .append("    networks:\n")
//                        .append("      - project-network\n");
//            }
//
//            compose.append("\nnetworks:\n  project-network:\n    driver: bridge\n");
//
//            Files.writeString(composeFile.toPath(), compose);
//
//            return "✅ Dockerfile and docker-compose.yml generated successfully.";
//
//        } catch (Exception e) {
//            e.printStackTrace();
//            return "❌ Error generating Docker files: " + e.getMessage();
//        }
//    }

//    public String generateDockerAndCompose(Long projectId, String dockerHubUsername) {
//        try {
//            ProjectArchive archive = repository.findById(projectId).orElseThrow(() -> new RuntimeException("Project not found"));
//
//            File projectDir = new File("./projects/project-" + projectId);
//            projectDir.mkdirs();
//
//            boolean hasFrontend = archive.getFrontendZip() != null && archive.getFrontendZip().length > 0;
//            boolean hasBackend = archive.getBackendZip() != null && archive.getBackendZip().length > 0;
//
//            File frontendDir = null;
//            File backendDir = null;
//
//            if (hasFrontend) {
//                frontendDir = new File(projectDir, "frontend");
//                frontendDir.mkdirs();
//                unzipBytesToDir(archive.getFrontendZip(), frontendDir);
//
//                File packageJsonDir = findDirectoryWithFile(frontendDir, "package.json");
//                File angularJsonDir = findDirectoryWithFile(frontendDir, "angular.json");
//                System.out.println(packageJsonDir.getName());
//
//                String relPackageJsonPath = frontendDir.toPath().relativize(packageJsonDir.toPath()).toString().replace("\\", "/");
//                String distFolderName = angularJsonDir != null ? angularJsonDir.getName() : "dist";
//
//                File dockerfile = new File(frontendDir, "Dockerfile");
//                Files.writeString(dockerfile.toPath(),
//                        "FROM node:20 as build\n" +
//                                "WORKDIR /app\n" +
//                                "\n" +
//                                "COPY " + relPackageJsonPath + "/package*.json ./\n" +
//                                "RUN npm install\n" +
//                                "COPY . .\n" +
//                                "RUN npm run build\n" +
//                                "\n" +
//                                "FROM nginx:stable-alpine\n" +
//                                "COPY --from=build /app/dist/" + distFolderName + "/browser /usr/share/nginx/html\n" +
//                                "\n" +
//                                "EXPOSE 80\n" +
//                                "CMD [\"nginx\", \"-g\", \"daemon off;\"]\n");
//
//
//            }
//
//            if (hasBackend) {
//                backendDir = new File(projectDir, "backend");
//                backendDir.mkdirs();
//                unzipBytesToDir(archive.getBackendZip(), backendDir);
//
//                File nestedBackendDir = findDirectoryWithFile(backendDir, "pom.xml");
//                String relativeJarPath = "target/*.jar";
//                if (nestedBackendDir != null && !nestedBackendDir.equals(backendDir)) {
//                    String relativePath = backendDir.toPath().relativize(nestedBackendDir.toPath()).toString().replace("\\", "/");
//                    relativeJarPath = relativePath + "/target/*.jar";
//                }
//
//                File dockerfile = new File(backendDir, "Dockerfile");
//                Files.writeString(dockerfile.toPath(),
//                        "FROM openjdk:21-jdk-slim\n" +
//                                "WORKDIR /app\n" +
//                                "COPY " + relativeJarPath + " app.jar\n" +
//                                "EXPOSE " + archive.getAppPort() + "\n" +
//                                "ENTRYPOINT [\"java\", \"-jar\", \"app.jar\"]\n");
//            }
//
//            File composeFile = new File(projectDir, "docker-compose.yml");
//            StringBuilder compose = new StringBuilder("version: '3.8'\nservices:\n");
//
//            if (hasFrontend && frontendDir != null) {
//                String relFrontendPath = projectDir.toPath().relativize(frontendDir.toPath()).toString().replace("\\", "/");
//                compose.append("  frontend:\n")
//                        .append("    build:\n")
//                        .append("      context: ./" + relFrontendPath + "\n")
//                        .append("    image: " + dockerHubUsername + "/project-" + projectId + "-frontend\n")
//                        .append("    ports:\n")
//                        .append("      - \"4200:80\"\n")
//                        .append("    networks:\n")
//                        .append("      - project-network\n");
//            }
//
//            if (hasBackend && backendDir != null) {
//                compose.append("  backend:\n")
//                        .append("    build:\n")
//                        .append("      context: ./backend\n")
//                        .append("    image: " + dockerHubUsername + "/project-" + projectId + "-backend\n")
//                        .append("    ports:\n")
//                        .append("      - \"" + archive.getAppPort() + ":" + archive.getAppPort() + "\"\n")
//                        .append("    networks:\n")
//                        .append("      - project-network\n");
//            }
//
//            compose.append("\nnetworks:\n  project-network:\n    driver: bridge\n");
//
//            Files.writeString(composeFile.toPath(), compose);
//
//            return "✅ Dockerfile and docker-compose.yml generated successfully.";
//
//        } catch (Exception e) {
//            e.printStackTrace();
//            return "❌ Error generating Docker files: " + e.getMessage();
//        }
//    }




    public String generateDockerAndCompose(Long projectId) {
        try {
            ProjectArchive archive = repository.findById(projectId).orElseThrow(() -> new RuntimeException("Project not found"));

            File projectDir = new File("./projects/project-" + projectId);
            projectDir.mkdirs();

            boolean hasFrontend = archive.getFrontendZip() != null && archive.getFrontendZip().length > 0;
            boolean hasBackend = archive.getBackendZip() != null && archive.getBackendZip().length > 0;

            File frontendDir = null;
            File backendDir = null;

            if (hasFrontend) {
                frontendDir = new File(projectDir, "frontend");
                frontendDir.mkdirs();
                unzipBytesToDir(archive.getFrontendZip(), frontendDir);

                File angularJsonDir = findDirectoryWithFile(frontendDir, "angular.json");
                File demoprojectDir = findDirectoryWithFileSkippingNodeModules(frontendDir, "package.json");

                String distFolderName = angularJsonDir != null ? angularJsonDir.getName() : "dist";

                String buildCommand = "npm run build";
                if (angularJsonDir != null) {
                    buildCommand = "npm run build -- --configuration=production";
                }

                String relPath = frontendDir.toPath().relativize(demoprojectDir.toPath()).toString().replace("\\", "/");

                File dockerfile = new File(frontendDir, "Dockerfile");
                Files.writeString(dockerfile.toPath(),
                        "FROM node:20 as build\n" +
                                "WORKDIR /app\n" +
                                "COPY " + relPath + "/package*.json ./\n" +
                                "RUN npm install\n" +
                                "COPY " + distFolderName + "/ .\n" +
                                "RUN " + buildCommand + "\n" +

                                "\n" +
                                "FROM nginx:stable-alpine\n" +
                                "COPY --from=build /app/dist/" + distFolderName + "/browser /usr/share/nginx/html\n" +
                                "EXPOSE 80\n" +
                                "CMD [\"nginx\", \"-g\", \"daemon off;\"]\n");

                frontendDir = demoprojectDir;
            }

            if (hasBackend) {
                backendDir = new File(projectDir, "backend");
                backendDir.mkdirs();
                unzipBytesToDir(archive.getBackendZip(), backendDir);

                File nestedBackendDir = findDirectoryWithFile(backendDir, "pom.xml");
                String relativeJarPath = "target/*.jar";
                if (nestedBackendDir != null && !nestedBackendDir.equals(backendDir)) {
                    String relativePath = backendDir.toPath().relativize(nestedBackendDir.toPath()).toString().replace("\\", "/");
                    relativeJarPath = relativePath + "/target/*.jar";
                }

                File dockerfile = new File(backendDir, "Dockerfile");
                Files.writeString(dockerfile.toPath(),
                        "FROM openjdk:21-jdk-slim\n" +
                                "WORKDIR /app\n" +
                                "COPY " + relativeJarPath + " app.jar\n" +
                                "EXPOSE " + archive.getAppPort() + "\n" +
                                "ENTRYPOINT [\"java\", \"-jar\", \"app.jar\"]\n");
            }

            File composeFile = new File(projectDir, "docker-compose.yml");
            StringBuilder compose = new StringBuilder("services:\n");

            if (hasFrontend && frontendDir != null) {
                String relFrontendPath = projectDir.toPath().relativize(frontendDir.toPath()).toString().replace("\\", "/");
                compose.append("  frontend:\n")
                        .append("    build:\n")
                        .append("      context: ./frontend\n")
                        .append("    image: " + dockerHubUsername + "/project-" + projectId + "-frontend\n")
                        .append("    ports:\n")
                        .append("      - \"4200:80\"\n")
                        .append("    networks:\n")
                        .append("      - project-network\n");
            }

            if (hasBackend && backendDir != null) {
                compose.append("  backend:\n")
                        .append("    build:\n")
                        .append("      context: ./backend\n")
                        .append("    image: " + dockerHubUsername + "/project-" + projectId + "-backend\n")
                        .append("    ports:\n")
                        .append("      - \"" + archive.getAppPort() + ":" + archive.getAppPort() + "\"\n")
                        .append("    networks:\n")
                        .append("      - project-network\n");
            }

            compose.append("\nnetworks:\n  project-network:\n    driver: bridge\n");

            Files.writeString(composeFile.toPath(), compose);

            return "✅ Dockerfile and docker-compose.yml generated successfully.";

        } catch (Exception e) {
            e.printStackTrace();
            return "❌ Error generating Docker files: " + e.getMessage();
        }
    }

    private File findDirectoryWithFileSkippingNodeModules(File baseDir, String fileName) {
        File[] files = baseDir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    if (file.getName().equals("node_modules")) continue;
                    File match = findDirectoryWithFileSkippingNodeModules(file, fileName);
                    if (match != null) return match;
                } else if (file.getName().equals(fileName)) {
                    return baseDir;
                }
            }
        }
        return null;
    }







    public String buildLatestFrontendAndBackend() {
        if (lastUploadedProjectId == null) return "❌ No project to build.";

        StringBuilder log = new StringBuilder();
        File projectDir = new File("./projects/project-" + lastUploadedProjectId);
        File frontendDir = findDirectoryWithFile(new File(projectDir, "frontend"), "angular.json");
        File backendDir = findDirectoryWithFile(new File(projectDir, "backend"), "pom.xml");

        if (frontendDir == null) return "❌ Angular frontend project not found.";
        if (backendDir == null) return "❌ Spring Boot backend project not found.";

        try {
            // Build backend
            log.append("\n🧱 Building backend (Spring Boot JAR)...\n");
            Process mvn = new ProcessBuilder("cmd", "/c", "mvn clean install")
                    .directory(backendDir)
                    .redirectErrorStream(true)
                    .start();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(mvn.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    log.append("[MAVEN] ").append(line).append("\n");
                }
            }
            mvn.waitFor();

            // Build frontend
            log.append("\n📦 Installing frontend dependencies (npm)...\n");
            Process npmInstall = new ProcessBuilder("cmd", "/c", "npm install")
                    .directory(frontendDir)
                    .redirectErrorStream(true)
                    .start();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(npmInstall.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    log.append("[NPM] ").append(line).append("\n");
                }
            }
            npmInstall.waitFor();

            log.append("\n🧱 Building frontend (Angular dist)...\n");
            Process ngBuild = new ProcessBuilder("cmd", "/c", "node --max-old-space-size=8192 ./node_modules/@angular/cli/bin/ng build --configuration=production")
                    .directory(frontendDir)
                    .redirectErrorStream(true)
                    .start();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(ngBuild.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    log.append("[NG] ").append(line).append("\n");
                }
            }
            ngBuild.waitFor();

            log.append("\n✅ Build process completed.\n");

        } catch (Exception e) {
            e.printStackTrace();
            log.append("❌ Build error: ").append(e.getMessage());
        }

        return log.toString();
    }

    private File findDirectoryWithFile(File root, String fileName) {
        if (!root.exists()) return null;
        File[] files = root.listFiles();
        if (files == null) return null;

        for (File file : files) {
            if (file.isDirectory()) {
                File match = findDirectoryWithFile(file, fileName);
                if (match != null) return match;
            } else if (file.getName().equalsIgnoreCase(fileName)) {
                return root;
            }
        }
        return null;
    }
    private void unzipBytesToDir(byte[] data, File dir) throws Exception {
        try (ZipInputStream zipIn = new ZipInputStream(new java.io.ByteArrayInputStream(data))) {
            ZipEntry entry;
            while ((entry = zipIn.getNextEntry()) != null) {
                File file = new File(dir, entry.getName());
                if (entry.isDirectory()) {
                    file.mkdirs();
                } else {
                    file.getParentFile().mkdirs();
                    try (FileOutputStream out = new FileOutputStream(file)) {
                        zipIn.transferTo(out);
                    }
                }
                zipIn.closeEntry();
            }
        }
    }


//    public String buildAndPushDockerImage(String username, String password) {
//        if (lastUploadedProjectId == null) {
//            return "❌ No project to push.";
//        }
//
//        StringBuilder log = new StringBuilder();
//        File projectDir = new File("./projects/project-" + lastUploadedProjectId);
//
//        // Try to find Dockerfile inside frontend or backend subdirectories
//        File frontendDir = findDirectoryWithFile(new File(projectDir, "frontend"), "Dockerfile");
//        File backendDir = findDirectoryWithFile(new File(projectDir, "backend"), "Dockerfile");
//
//        File dockerfileDir = backendDir != null ? backendDir : frontendDir;
//
//        if (dockerfileDir == null) {
//            return "❌ Dockerfile not found.";
//        }
//
//        try {
//            log.append("\n🔐 Logging into Docker Hub...\n");
//            new ProcessBuilder("cmd", "/c",
//                    "docker login -u " + username + " -p " + password)
//                    .redirectErrorStream(true)
//                    .start().waitFor();
//
//            log.append("🐳 Building backend Docker image...\n");
//            Process build = new ProcessBuilder("cmd", "/c",
//                    "docker build -t " + username + "/project-" + lastUploadedProjectId + " .")
//                    .directory(dockerfileDir)
//                    .redirectErrorStream(true)
//                    .start();
//            try (BufferedReader reader = new BufferedReader(new InputStreamReader(build.getInputStream()))) {
//                String line;
//                while ((line = reader.readLine()) != null) {
//                    log.append("[DOCKER BUILD] ").append(line).append("\n");
//                }
//            }
//            build.waitFor();
//
//            log.append("📤 Pushing image to Docker Hub...\n");
//            Process push = new ProcessBuilder("cmd", "/c",
//                    "docker push " + username + "/project-" + lastUploadedProjectId)
//                    .redirectErrorStream(true)
//                    .start();
//            try (BufferedReader reader = new BufferedReader(new InputStreamReader(push.getInputStream()))) {
//                String line;
//                while ((line = reader.readLine()) != null) {
//                    log.append("[DOCKER PUSH] ").append(line).append("\n");
//                }
//            }
//            push.waitFor();
//
//            log.append("\n✅ Docker image pushed successfully.\n");
//
//        } catch (Exception e) {
//            e.printStackTrace();
//            log.append("❌ Error during Docker operations: ").append(e.getMessage());
//        }
//
//        return log.toString();
//    }

    public String buildAndPushDockerImage( ) {
        String username = this.dockerHubUsername;
        String password = this.dockerHubPassword;

        if (lastUploadedProjectId == null) {
            return "❌ No project to push.";
        }

        StringBuilder log = new StringBuilder();
        File projectDir = new File("./projects/project-" + lastUploadedProjectId);

        try {
            log.append("\n🔐 Logging into Docker Hub...\n");
            Process login = new ProcessBuilder("cmd", "/c",
                    "docker login -u " + username + " -p " + password)
                    .directory(projectDir)
                    .redirectErrorStream(true)
                    .start();
            login.waitFor();

            log.append("🐳 Building images using docker-compose...\n");
            Process build = new ProcessBuilder("cmd", "/c", "docker-compose build")
                    .directory(projectDir)
                    .redirectErrorStream(true)
                    .start();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(build.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    log.append("[DOCKER BUILD] ").append(line).append("\n");
                }
            }
            build.waitFor();

            log.append("📤 Pushing images using docker-compose...\n");
            Process push = new ProcessBuilder("cmd", "/c", "docker-compose push")
                    .directory(projectDir)
                    .redirectErrorStream(true)
                    .start();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(push.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    log.append("[DOCKER PUSH] ").append(line).append("\n");
                }
            }
            push.waitFor();

            log.append("\n✅ Docker images built and pushed successfully.\n");

        } catch (Exception e) {
            e.printStackTrace();
            log.append("❌ Error during Docker operations: ").append(e.getMessage());
        }

        return log.toString();
    }


    public File prepareDeploymentBundle() {
        if (lastUploadedProjectId == null) return null;

        try {
            File projectDir = new File("./projects/project-" + lastUploadedProjectId);
            File deploymentDir = new File("./deployment-bundles/project-" + lastUploadedProjectId);
            deploymentDir.mkdirs();

            // Copy docker-compose.yml
            Files.copy(
                    new File(projectDir, "docker-compose.yml").toPath(),
                    new File(deploymentDir, "docker-compose.yml").toPath(),
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING
            );

            // Copy frontend Dockerfile and dist
            File frontendDir = new File(projectDir, "frontend");
            File deploymentFrontend = new File(deploymentDir, "frontend");
            deploymentFrontend.mkdirs();
            Files.copy(
                    new File(frontendDir, "Dockerfile").toPath(),
                    new File(deploymentFrontend, "Dockerfile").toPath(),
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING
            );

            File distDir = findDirectoryWithFile(frontendDir, "angular.json");
            if (distDir != null) {
                File browserDist = new File(distDir, "dist");
                if (browserDist.exists()) {
                    copyDirectory(browserDist, new File(deploymentFrontend, "dist"));
                }
            }

            // Copy backend Dockerfile
            File backendDockerDir = new File(projectDir, "backend");
            File deploymentBackend = new File(deploymentDir, "backend");
            deploymentBackend.mkdirs();

            Files.copy(
                    new File(backendDockerDir, "Dockerfile").toPath(),
                    new File(deploymentBackend, "Dockerfile").toPath(),
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING
            );

// Copy JAR from nested backendDir/target
            File backendDir = findDirectoryWithFile(new File(projectDir, "backend"), "pom.xml");
            File targetDir = new File(backendDir, "target");
            if (targetDir.exists()) {
                for (File file : targetDir.listFiles()) {
                    if (file.getName().endsWith(".jar")) {
                        Files.copy(
                                file.toPath(),
                                new File(deploymentBackend, "app.jar").toPath(),
                                java.nio.file.StandardCopyOption.REPLACE_EXISTING
                        );
                        break;
                    }
                }
            }


            return deploymentDir;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private void copyDirectory(File source, File target) throws IOException {
        if (source.isDirectory()) {
            if (!target.exists()) {
                target.mkdirs();
            }
            for (String child : source.list()) {
                copyDirectory(new File(source, child), new File(target, child));
            }
        } else {
            Files.copy(source.toPath(), target.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        }
    }


    public String transferDeploymentBundleToEC2(String ec2Ip) {
        String sshKey = "springboot-key.pem";
        String localBundlePath = "./deployment-bundles/project-" + lastUploadedProjectId;
        String remotePath = "ec2-user@" + ec2Ip + ":/home/ec2-user/app";
        StringBuilder log = new StringBuilder();

        try {
            log.append("📦 Transferring deployment bundle to EC2 at ").append(ec2Ip).append("...\n");
            System.out.println("📦 Running rsync command...");

            ProcessBuilder rsyncProcess = new ProcessBuilder(
                    "rsync", "-avz", "-e",
                    "ssh -i " + sshKey + " -o StrictHostKeyChecking=no",
                    localBundlePath + "/",
                    remotePath
            );

            Process process = rsyncProcess.start();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println("[RSYNC] " + line);
                    log.append("[RSYNC] ").append(line).append("\n");
                }
            }

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println("[RSYNC-ERR] " + line);
                    log.append("[RSYNC-ERR] ").append(line).append("\n");
                }
            }

            int exitCode = process.waitFor();
            if (exitCode == 0) {
                log.append("✅ Deployment bundle transferred successfully!\n");
            } else {
                log.append("❌ RSYNC exited with code ").append(exitCode).append("\n");
            }

        } catch (Exception e) {
            log.append("❌ Error during RSYNC: ").append(e.getMessage()).append("\n");
            e.printStackTrace();
        }

        return log.toString();
    }



    public String uploadProjectFolder() {
        String ip = ec2Service.getLastCreatedInstancePublicIp();
        StringBuilder output = new StringBuilder();

        final String PRIVATE_KEY_PATH = "D:/ec2launcher/springboot-key.pem";
//        final String LOCAL_FOLDER_PATH = "D:/ec2launcher/deployment-bundles/project-70";
        final String LOCAL_FOLDER_PATH = "D:/ec2launcher/deployment-bundles/project-" + lastUploadedProjectId;
        final String USER = "ec2-user";
        final int PORT = 22;
        final String REMOTE_BASE_PATH = "/home/ec2-user/project-" + lastUploadedProjectId; // You can change this as needed

        try {
            JSch jsch = new JSch();
            jsch.addIdentity(PRIVATE_KEY_PATH);

            Session session = jsch.getSession(USER, ip, PORT);
            session.setConfig("StrictHostKeyChecking", "no");
            session.connect();
            output.append("Connected to EC2: ").append(ip).append("<br>");

            ChannelSftp sftpChannel = (ChannelSftp) session.openChannel("sftp");
            sftpChannel.connect();

            File folder = new File(LOCAL_FOLDER_PATH);
            if (!folder.exists() || !folder.isDirectory()) {
                return "Invalid folder path: " + LOCAL_FOLDER_PATH;
            }

            uploadDirectory(sftpChannel, folder, folder.getAbsolutePath(), REMOTE_BASE_PATH, output);

            sftpChannel.disconnect();
            session.disconnect();

            output.append("<br>Folder upload complete!");
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }

        return output.toString();
    }

    private void uploadDirectory(ChannelSftp sftpChannel, File localDir, String basePath,
                                 String remoteBasePath, StringBuilder output) throws Exception {

        String relativePath = localDir.getAbsolutePath().substring(basePath.length()).replace("\\", "/");
        String remoteDir = remoteBasePath + relativePath;

        try {
            sftpChannel.cd(remoteDir);
        } catch (SftpException e) {
            sftpChannel.mkdir(remoteDir);
            output.append("Created remote directory: ").append(remoteDir).append("<br>");
            sftpChannel.cd(remoteDir);
        }

        for (File file : localDir.listFiles()) {
            if (file.isFile()) {
                String remoteFilePath = remoteDir + "/" + file.getName();
                try (FileInputStream fis = new FileInputStream(file)) {
                    sftpChannel.put(fis, remoteFilePath);
                    output.append("Uploaded: ").append(remoteFilePath).append("<br>");
                } catch (Exception e) {
                    output.append("Failed to upload: ").append(remoteFilePath)
                            .append(" - ").append(e.getMessage()).append("<br>");
                }
            } else if (file.isDirectory()) {
                uploadDirectory(sftpChannel, file, basePath, remoteBasePath, output);
            }
        }


    }


    public String deployToEC2() {
        StringBuilder output = new StringBuilder();
        String user = "ec2-user";
        String privateKeyPath = "D:/ec2launcher/springboot-key.pem";

        try {
            output.append("Connecting to: ").append(ec2Service.getLastCreatedInstancePublicIp()).append("<br>");

            // SSH setup
            JSch jsch = new JSch();
            jsch.addIdentity(privateKeyPath);
            Session session = jsch.getSession(user, ec2Service.getLastCreatedInstancePublicIp(), 22);
            session.setConfig("StrictHostKeyChecking", "no");
            session.connect();

            // Remote deployment command
            String projectDir = "project-" + lastUploadedProjectId;
            String command = "cd " + projectDir + " && docker-compose pull && docker-compose up -d";

            output.append(runCommand(session, command));
            session.disconnect();

        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }

        return output.toString();
    }

    private String runCommand(Session session, String command) throws Exception {
        StringBuilder result = new StringBuilder();
        ChannelExec channel = (ChannelExec) session.openChannel("exec");
        channel.setCommand(command);
        channel.setErrStream(System.err);
        InputStream in = channel.getInputStream();
        channel.connect();

        byte[] tmp = new byte[1024];
        while (true) {
            while (in.available() > 0) {
                int i = in.read(tmp, 0, 1024);
                if (i < 0) break;
                result.append(new String(tmp, 0, i));
            }
            if (channel.isClosed()) break;
            Thread.sleep(100);
        }
        channel.disconnect();
        return result.toString().replaceAll("\n", "<br>");
    }




}