package com.example.demo;

import jakarta.persistence.*;
import org.hibernate.annotations.NaturalId;

import java.time.Instant;
import java.util.Date;
@Entity
@Table(name = "PlayerStats")
public class PlayerStats {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NaturalId
    @Column(unique = true, nullable = false)
    private String name;

    @Column(nullable = false, columnDefinition = "INTEGER DEFAULT 0")
    private int wins = 0;

    @Column(nullable = false, columnDefinition = "INTEGER DEFAULT 0")
    private int totalPoints = 0;

    @Column
    private Date lastSeen;

    // Конструкторы
    public PlayerStats() {}

    public PlayerStats(String name) {
        this.name = name;
    }

    // Геттеры и сеттеры
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public int getWins() { return wins; }
    public void setWins(int wins) { this.wins = wins; }

    public int getTotalPoints() { return totalPoints; }
    public void setTotalPoints(int totalPoints) { this.totalPoints = totalPoints; }

    public Date getLastSeen() { return lastSeen; }
    public void setLastSeen(Date lastSeen) { this.lastSeen = lastSeen; }

    // Метод для обновления времени последнего посещения
    public void updateLastSeen() {
        this.lastSeen = Date.from(Instant.now());
    }
}