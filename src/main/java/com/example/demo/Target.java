package com.example.demo;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.image.Image;

import java.util.UUID;


public class Target {
    private final String id;
    private final double baseWidth  = 50;
    private final double baseHeight  = 50;
    private final double size;
    private double speed;

    private final int points;
    private final Image image;
    private double posX, posY;
    private double targetPosX, targetPosY;
    private boolean active = true;

//    public Target(double posX, double posY) {
//        this.id = UUID.randomUUID().toString();
//        this.speed = Config.ARCHER_TARGET_SPEED;
//        this.points = 1;
//        this.size = 1.0;
//        this.posX = posX;
//        this.posY = posY;
//        this.image = null;
//    }
//
//    public Target(double posX, double posY, String targetPath) {
//        this.size = 1.0;
//        this.speed = Config.ARCHER_TARGET_SPEED;
//        this.id = UUID.randomUUID().toString();
//        this.points = 1;
//        this.posX = posX;
//        this.posY = posY;
//        if (targetPath != null && !targetPath.isEmpty()) {
//            this.image = new Image(targetPath);
//        } else {
//            this.image = null;
//        }
//    }

    public Target(TargetData data, String imagePath) {
        this.id = data.getId();
        this.posX = data.getPosX();
        this.posY = data.getPosY();
        this.size = data.getSize();
        this.speed = data.getSpeed();
        this.points = data.getPoints();
        this.image = new Image(imagePath);
    }

    public void updateFromData(TargetData data) { //////////////////////////////////
//        this.posX = data.getPosX();
//        this.posY = data.getPosY();
        this.targetPosX = data.getPosX();
        this.targetPosY = data.getPosY();
        this.speed = data.getSpeed();
    }

    public void updatePosition(double deltaTime) {
        double lerpSpeed = 8.0 * deltaTime;
        posX += (targetPosX - posX) * lerpSpeed;
        posY += (targetPosY - posY) * lerpSpeed;
    }

    public boolean isHit(Projectile projectile) {
        double targetWidth = baseWidth * size;
        double targetHeight = baseHeight * size;
        return projectile.getPosX() + projectile.getWidth() > posX &&
                projectile.getPosX() < posX + targetWidth &&
                projectile.getPosY() + projectile.getHeight() > posY &&
                projectile.getPosY() < posY + targetHeight;
    }

    public void draw(GraphicsContext gc) {
        if (image != null) {
            gc.drawImage(image, posX, posY, baseWidth * size, baseHeight * size);
        } else {
            gc.setFill(Color.PINK);
            gc.fillRect(posX, posY, baseWidth * size, baseHeight * size);
        }
    }

    public double getPosX() { return posX; }
    public double getPosY() { return posY; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public String getId() { return id; }
    public int getPoints() { return points; }
}
