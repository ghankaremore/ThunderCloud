package com.example.ec2launcher.deployment.model;

import jakarta.persistence.*;

@Entity
public class ProjectArchive {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private int appPort;

    @Lob
    @Column(columnDefinition = "LONGBLOB")
    private byte[] frontendZip;

    @Lob
    @Column(columnDefinition = "LONGBLOB")
    private byte[] backendZip;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public int getAppPort() { return appPort; }
    public void setAppPort(int appPort) { this.appPort = appPort; }

    public byte[] getFrontendZip() { return frontendZip; }
    public void setFrontendZip(byte[] frontendZip) { this.frontendZip = frontendZip; }

    public byte[] getBackendZip() { return backendZip; }
    public void setBackendZip(byte[] backendZip) { this.backendZip = backendZip; }
}
