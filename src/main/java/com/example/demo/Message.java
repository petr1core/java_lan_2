package com.example.demo;

import com.google.gson.annotations.SerializedName;

import java.util.*;


public class Message {
    @SerializedName("type")
    private MessageType type;

    @SerializedName("nicknames")
    private List<String> nicknames;

    @SerializedName("readyPlayers")
    private Set<String> readyPlayers = new HashSet<>();

    @SerializedName("content")
    private String content;

    @SerializedName("playerY")
    private double playerY;

    @SerializedName("projectiles")
    private List<ProjectileData> projectiles = new ArrayList<>();

    @SerializedName("playerPositions")
    private Map<String, Double> playerPositions = new HashMap<>();

    @SerializedName("scores")
    private Map<String, Integer> scores = new HashMap<>();

    @SerializedName("targets")
    private List<TargetData> targets;

    @SerializedName("winner")
    private String winner;

    @SerializedName("shooter")
    private String shooter;

    @SerializedName("hit-target")
    private String hitTargetId;

    @SerializedName("hit-arrow")
    private String hitArrowId;

    @SerializedName("points")
    private int points;

    @SerializedName("arrows")
    private Map<String, Integer> arrows;

    public Message() {}

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Message{");
        sb.append(" \ntype=").append(type);
        sb.append(", \nnicknames=").append(nicknames);
        sb.append(", \nwinner=").append(winner);
        sb.append(", \nreadyPlayers=").append(readyPlayers);
        sb.append(", \ncontent='").append(content).append('\'');
        sb.append(", \nplayerY=").append(playerY);
        sb.append(", \nprojectiles=").append(projectiles);
        sb.append(", \nplayerPositions=").append(playerPositions);
        sb.append(", \nscores=").append(scores);
        sb.append(", \ntargets=").append(targets);
        sb.append(", \nhit-shooterId=").append(shooter);
        sb.append(", \nhit-targetId='").append(hitTargetId);
        sb.append(", \nhit-arrowId=").append(hitArrowId);
        sb.append(", \nhit-points=").append(points);
        sb.append(", \narrows=").append(arrows);
        sb.append("\n}");

        return sb.toString();
    }

    public Message(MessageType messageType, String content) {
        this.type = messageType;
        this.content = content;
    }

    public Message(MessageType type, List<String> nicknames) {
        this.type = type;
        this.nicknames = nicknames;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public MessageType getType() {
        return type;
    }

    public void setType(MessageType type) {
        this.type = type;
    }

    public List<String> getNicknames() {
        return nicknames;
    }

    public void setNicknames(List<String> nicknames) {
        this.nicknames = nicknames;
    }

    public Set<String> getReadyPlayers() {
        return readyPlayers;
    }

    public void setReadyPlayers(Set<String> readyPlayers) {
        this.readyPlayers = readyPlayers != null ? readyPlayers : new HashSet<>();
    }

    public double getPlayerY() {
        return playerY;
    }

    public void setPlayerY(double playerY) {
        this.playerY = playerY;
    }

    public List<ProjectileData> getProjectiles() {
        return projectiles;
    }

    public void setProjectiles(List<ProjectileData> projectiles) {
        this.projectiles = projectiles;
    }

    public Map<String, Double> getPlayerPositions() {
        return playerPositions;
    }

    public void setPlayerPositions(Map<String, Double> playerPositions) {
        this.playerPositions = playerPositions != null ? playerPositions : new HashMap<>();
    }

    public Map<String, Integer> getScores() {
        return scores;
    }

    public void setScores(Map<String, Integer> scores) {
        this.scores = scores;
    }

    public List<TargetData> getTargets() {
        return targets;
    }

    public void setTargets(List<TargetData> td) {
        this.targets = td;
    }

    public String getHitTargetId() {
        return hitTargetId;
    }

    public void setHitTargetId(String hitTargetId) {
        this.hitTargetId = hitTargetId;
    }

    public String getHitArrowId() {
        return hitArrowId;
    }

    public void setHitArrowId(String hitArrowId) {
        this.hitArrowId = hitArrowId;
    }

    public int getPoints() {
        return points;
    }

    public void setPoints(int points) {
        this.points = points;
    }

    public Map<String, Integer> getArrows() {
        return arrows;
    }

    public void setArrows(Map<String, Integer> arrows) {
        this.arrows = arrows;
    }

    public String getShooter() {
        return shooter;
    }

    public void setShooter(String shooter) {
        this.shooter = shooter;
    }


}
