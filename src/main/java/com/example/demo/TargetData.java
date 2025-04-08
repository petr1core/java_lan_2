package com.example.demo;

import java.io.Serializable;
import java.util.UUID;

public class TargetData implements Serializable {
    private String id;
    private double size;  // 1.0 - small, 2.0 - medium, 3.0 - large
    private double posX;
    private double posY;
    private double speed;
    private int points;

    private int col;

    public TargetData(double posX, double posY, double size, double speed, int points, int col) {
        this.id = UUID.randomUUID().toString();
        this.posX = posX;
        this.posY = posY;
        this.size = size;
        this.speed = speed;
        this.points = points;
        this.col = col;
    }

    public int getColumn(){
        return col;
    }

    public void setColumn(int col){
        this.col = col;
    }

    public double getSpeed() {
        return speed;
    }

    public void setSpeed(double speed) {
        this.speed = speed;
    }

    public int getPoints() {
        return points;
    }

    public void setPoints(int points) {
        this.points = points;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public double getSize() {
        return size;
    }

    public void setSize(double size) {
        this.size = size;
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
}
