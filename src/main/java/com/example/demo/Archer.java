package com.example.demo;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class Archer {
    private double posY;
    private final double posX;
    private final double maxWidth;
    private final double speed;
    private final IntegerProperty  arrowsCount = new SimpleIntegerProperty();
    private final List<Projectile> projectiles;
    private final String nickname;

    private final double width;
    private final double height;
    private final Image archerImage;
    private final String projectileImagePath;

    private double targetY;

    private final Point sight;

    public Archer(String nickname, double posX, double posY, double width, double height, double speed, Point sight, int arrowsCount, String archerImagePath, String projectileImagePath, double maxWidth) {
        this.nickname = nickname;
        this.posX = posX;
        this.posY = posY;
        this.maxWidth = maxWidth;
        this.speed = speed;
        this.width = width;
        this.height = height;
        this.sight = sight;
        this.arrowsCount.set(arrowsCount);
        this.projectiles = new ArrayList<Projectile>();
        this.projectileImagePath = projectileImagePath;
        if (archerImagePath != null && !archerImagePath.isEmpty()) {
            this.archerImage = new Image(archerImagePath);
            System.out.println("Archer image path: " + archerImagePath);
        } else {
            this.archerImage = null; // then draw rect
            System.out.println("Archer image path is null");

        }
    }

    public Archer(String nickname, double pos_y, double maxWidth, boolean isEnemy) {
        this.nickname = nickname;
        this.posY = pos_y;
        this.maxWidth = maxWidth;
        this.posX = Config.START_POS_X;
        this.width = Config.ARCHER_WIDTH;
        this.height = Config.ARCHER_HEIGHT;
        this.speed = Config.ARCHER_SPEED;
        this.sight = Config.ARCHER_SIGHT_POINT;
        this.arrowsCount.set(Config.ARCHER_INIT_ARROWS);
        this.projectiles = new ArrayList<Projectile>();
        this.projectileImagePath = Config.ARCHER_ARROW_IMG_PATH;
        if (isEnemy) {
            this.archerImage = new Image(Config.ARCHER_ENEMY_IMG_PATH);
        } else {
            this.archerImage = new Image(Config.ARCHER_PLAYER_IMG_PATH);
        }
    }

    public void moveUp(double deltaTime) {
        double step = speed * deltaTime; // Расчет шага с учетом времени
        if (posY - step >= 0) {
            posY -= step;
        } else {
            posY = 0;
        }
    }

    public void moveDown(double maxHeight, double deltaTime) {
        double step = speed * deltaTime; // Расчет шага с учетом времени
        if (posY + height + step <= maxHeight) {
            posY += step;
        } else {
            posY = maxHeight - height;
        }
    }

    public boolean shoot() {
        if (arrowsCount.intValue() > 0) {
            projectiles.add(new Projectile(posX + 30, posY + 20, Config.ARCHER_ARROW_SPEED, Config.ARCHER_ARROW_IMG_PATH, Config.getWindowWidth())); // Снаряд появляется справа от стрелка
            arrowsCount.set(arrowsCount.get() - 1);
            return true;
        } else {
            System.out.println("No arrows left!");
            return false; // Возвращаем false, если стрел нет
        }
    }

    public synchronized void updateProjectiles(double deltaTime) {
        projectiles.removeIf(projectile -> {
            projectile.update(deltaTime);
            return projectile.isOutOfBounds();
        });
    }

    public synchronized void drawProjectiles(GraphicsContext gc) {
        for (Projectile projectile : projectiles) {
            projectile.draw(gc);
        }
    }

    public void draw(GraphicsContext gc) {
        if (archerImage != null) {
            //System.out.println("Drawing archer image at (" + posX + ", " + posY + ")");
            gc.drawImage(archerImage, posX, posY, width, height);
        } else {
            //System.out.println("Drawing rectangle instead of image");
            gc.setFill(Color.GREEN);
            gc.fillRect(posX, posY, width, height);
        }
    }

    public void setPosY(double posY) {
        this.posY = posY;
    }

    public IntegerProperty arrowsCountProperty() {
        return arrowsCount;
    }

    public synchronized List<Projectile> getProjectiles() {
        return projectiles;
    }

    public double getPosY() {
        return posY;
    }

    public String getNickname() {
        return nickname;
    }

    public void setTargetY(double targetY) {
        this.targetY = targetY;
    }

    public void updatePosition(double deltaTime) {
        double lerpSpeed = 8.0 * deltaTime;
        posY += (targetY - posY) * lerpSpeed;
    }

    public synchronized void setArrowsCount(int count) {
        arrowsCount.set(count);
    }

    public synchronized int getArrowsCount() {
        return arrowsCount.get();
    }
}
