package com.example.demo;

import java.io.Serializable;
import java.util.UUID;

public class ProjectileData implements Serializable {
    private String id = UUID.randomUUID().toString();
    private double posX;
    private double posY;
    private long creationTime;
    private long serverTime;
    private double velocity;
    private String shooter;

    public ProjectileData(String shooter,double posX, double posY, long creationTime, double velocity) {
        this.posX = posX;
        this.posY = posY;
        this.creationTime = creationTime;
        this.velocity = velocity;
    }

    public double getVelocity() {
        return velocity;
    }

    public void setVelocity(double velocity) {
        this.velocity = velocity;
    }

    public double getPosX() {
        return posX;
    }

    public void setPosX(double posX) {
        this.posX = posX;
    }

    public double getPosY() {
        return posY;
    }
    public void setPosY(double posY) {
        this.posY = posY;
    }
    public long getCreationTime() { return creationTime; }

    public void setCreationTime(long l) {
        this.creationTime = l;
    }
    public long getServerTime() { return serverTime; }
    public void setServerTime(long t) {
        this.serverTime = t;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setShooter(String shooter) {
        this.shooter = shooter;
    }

    public String getShooter() {
        return this.shooter;
    }
}
