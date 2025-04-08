package com.example.demo;

import java.awt.*;
import javafx.scene.paint.Color;

public class Config {
    public static final int START_POS_X = 80;
    public static final int ARCHER_WIDTH = 40;
    public static final int ARCHER_HEIGHT = 50;
    public static final int ARCHER_SPEED = 200;
    public static final Point ARCHER_SIGHT_POINT = new Point(0,0);
    public static final int ARCHER_INIT_ARROWS = 20;
    public static final String ARCHER_ENEMY_IMG_PATH = "/archer-enemy.png";
    public static final String ARCHER_PLAYER_IMG_PATH = "/archer.png";
    public static final String ARCHER_ARROW_IMG_PATH = "/bow_arrow.png";
    public static final int ARCHER_ARROW_SPEED = 60;
    public static final double ARCHER_TARGET_SPEED = 50;

    public static final int ARCHER_COOLDOWN_MILLIS = 300;

    public static final int GAME_WIN_SCORE = 200;

    public static final String TARGET_IMG_PATH = "/target.png";

    public static final int PING_PERIOD = 16;

    private static int WINDOW_WIDTH = 1000;
    private static int WINDOW_HEIGHT = 1000;

    private static int CANVAS_WIDTH = WINDOW_WIDTH - 300;
    private static int CANVAS_HEIGHT = WINDOW_HEIGHT - 159;

    private static int CONTROL_PANEL_HEIGHT = 50;
    private static int STATS_PANEL_WIDTH = WINDOW_WIDTH - CANVAS_WIDTH;

    public static final double STATS_ENTRY_WIDTH = 200;
    public static final Color PLAYER_COLOR = Color.web("#4CAF50");
    public static final Color ENEMY_COLOR = Color.web("#FF5252");
    public static int getWindowWidth(){
        return WINDOW_WIDTH;
    }
    public static  int getWindowHeight(){
        return WINDOW_HEIGHT;
    }

    public static int getCanvasWidth(){
        return CANVAS_WIDTH;
    }

    public static  int getCanvasHeight(){
        return CANVAS_HEIGHT;
    }

    public static int getControlPanelHeight(){
        return CONTROL_PANEL_HEIGHT;
    }

    public static int getStatsPanelWidth(){
        return STATS_PANEL_WIDTH;
    }
}
