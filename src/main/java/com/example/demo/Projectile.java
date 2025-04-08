package com.example.demo;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;

import java.util.UUID;

public class Projectile {
    private double posX;
    private double posY;
    private double maxWidth;
    private final double width, height;
    private double velocity;
    private final Image image;
    private final long creationTime; // время создания
    private String id;

    private String shooter = "none";

    public Projectile( double posX, double posY, double velocity, String imagePath, double maxWidth) {
        this.posX = posX;
        this.posY = posY;
        this.maxWidth = maxWidth;
        this.velocity = velocity;
        this.creationTime = System.currentTimeMillis();
        this.id = UUID.randomUUID().toString();
        if (imagePath != null && !imagePath.isEmpty()) {
            this.image = new Image(imagePath);
            this.width = image.getWidth()/10;
            this.height = image.getHeight()/10;
            System.out.println("Projectile image: " + imagePath);
        } else {
            this.image = null; // then draw circle
            this.width = 20;
            this.height = 5;
            System.out.println("Projectile image path is null");
        }
    }

    public Projectile(ProjectileData data) {
        this.id = data.getId();
        this.posX = data.getPosX();
        this.posY = data.getPosY();
        this.maxWidth = Config.getWindowWidth();
        this.velocity = data.getVelocity();
        this.creationTime = data.getCreationTime();;
        this.image = new Image(Config.ARCHER_ARROW_IMG_PATH);
        this.width = image.getWidth()/10;
        this.height = image.getHeight()/10;
        //this.id= UUID.randomUUID().toString();
    }

//    public boolean matches(ProjectileData data) {
//        return this.id.equals(data.getId());
//    }

    public void update(double deltaTime) {
        posX += velocity * deltaTime;
    }

    public boolean isOutOfBounds(){
        return posX > maxWidth;
    }

    public double getPosX() {
        return posX;
    }

    public void setPosX(double posX) { this.posX = posX; }

    public double getPosY() {
        return posY;
    }

    public double getWidth() {
        return width;
    }

    public double getHeight() {
        return height;
    }

    public void draw(GraphicsContext gc) {
        if (image != null) {
            gc.drawImage(image, posX, posY, width, height);
        } else {
            gc.setFill(Color.RED);
            gc.fillOval(posX, posY, 20, 20);
        }

    }

    public ProjectileData toData() {
        ProjectileData data = new ProjectileData(
                this.shooter,
                this.posX,
                this.posY,
                this.creationTime,
                this.velocity
        );
        data.setId(this.id);
        return data;
    }

    public void setVelocity(double velocity) {
        this.velocity = velocity;
    }

    public String getId() {
        return id;
    }

    public void setPosY(double posY) {
        this.posY = posY;
    }
}
