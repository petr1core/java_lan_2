package com.example.demo;

import jakarta.persistence.criteria.Root;
import org.hibernate.Session;
import org.hibernate.Transaction;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;

public class DataBase {
    public static final String dbPath = "jdbc:sqlite:"+System.getProperty("user.dir")+"\\src\\main\\resources\\DataBase.db";

    // Метод для получения подключения к базе данных
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(dbPath);
    }

    // Инициализация базы данных и создание таблицы, если она ещё не существует
    public static void initialize() {
//        try (Connection conn = DriverManager.getConnection(dbPath);
//             Statement stmt = conn.createStatement()) {
//            String sql = "CREATE TABLE IF NOT EXISTS PlayerStats (" +
//                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
//                    "name TEXT NOT NULL UNIQUE," +
//                    "wins INTEGER DEFAULT 0," +
//                    "totalPoints INTEGER DEFAULT 0.0," +
//                    "lastSeen TIMESTAMP)";
//            stmt.execute(sql);
//            System.out.println("dbPath: "+dbPath);
//            System.out.println("База данных и таблица успешно созданы или уже существуют.");
//        } catch (SQLException e) {
//            System.err.println("Ошибка инициализации базы данных: " + e.getMessage());
//        }
        System.out.println("Hibernate initialized");
    }

    public static synchronized List<PlayerStats> getLeaderboard() {
//        ArrayList<PlayerStats> leaders = new ArrayList<>();
//        try (Connection conn = DataBase.getConnection();
//             PreparedStatement ps = conn.prepareStatement("SELECT name, wins, totalPoints, lastSeen FROM PlayerStats ORDER BY wins DESC"))
//        {
//            ResultSet rs = ps.executeQuery();
//            while (rs.next()) {
//                leaders.add(new PlayerStats(rs.getString("name"), rs.getInt("wins"), rs.getInt("totalPoints"), rs.getTimestamp("lastSeen")));
//            }
//        } catch (SQLException e) {
//            System.err.println("Ошибка получения таблицы лидеров: " + e.getMessage());
//        }
//        System.out.println("DB SZ "+leaders.size());
//        return leaders;
        // ...
//        try (Session session = HibernateUtil.getSession()) {
//            CriteriaBuilder builder = session.getCriteriaBuilder();
//            CriteriaQuery<PlayerStats> criteria = builder.createQuery(PlayerStats.class);
//            criteria.from(PlayerStats.class);
//
//            return session.createQuery(criteria)
//                    .setMaxResults(50)
//                    .getResultList();
//        } catch (Exception e) {
//            System.err.println("Ошибка получения таблицы лидеров: " + e.getMessage());
//            return List.of();
//        }
        try (Session session = HibernateUtil.getSession()) {
            CriteriaBuilder builder = session.getCriteriaBuilder();
            CriteriaQuery<PlayerStats> criteria = builder.createQuery(PlayerStats.class);
            Root<PlayerStats> root = criteria.from(PlayerStats.class);

            // Упорядочиваем сначала по wins (по убыванию), затем по totalPoints (по убыванию)
            criteria.orderBy(
                    builder.desc(root.get("wins")),
                    builder.desc(root.get("totalPoints"))
            );

            return session.createQuery(criteria)
                    .setMaxResults(50)
                    .getResultList();
        } catch (Exception e) {
            System.err.println("Ошибка получения таблицы лидеров: " + e.getMessage());
            return List.of();
        }
    }

//    public static synchronized boolean isNameUnique(String name) {
//        try (Connection conn = DataBase.getConnection();
//             PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM PlayerStats WHERE name = ?")){
//            ps.setString(1, name);
//            ResultSet rs = ps.executeQuery();
//            return rs.getInt(1) == 0;
//        } catch (SQLException e) {
//            System.err.println("Ошибка проверки имени: " + e.getMessage());
//            return false;
//        }
//    }

    public static synchronized void savePlayerStats(String name) {
//        try (Connection conn = DataBase.getConnection()) {
//            conn.setAutoCommit(false);
//
//            // Сначала пробуем обновить lastSeen для существующего игрока
//            try (PreparedStatement updatePs = conn.prepareStatement(
//                    "UPDATE PlayerStats SET lastSeen = ? WHERE name = ?")) {
//
//                updatePs.setTimestamp(1, Timestamp.from(Instant.now()));
//                updatePs.setString(2, name);
//                int updatedRows = updatePs.executeUpdate();
//
//                // Если игрок не найден, создаем новую запись
//                if (updatedRows == 0) {
//                    try (PreparedStatement insertPs = conn.prepareStatement(
//                            "INSERT INTO PlayerStats (name, lastSeen) VALUES (?, ?)")) {
//
//                        insertPs.setString(1, name);
//                        insertPs.setTimestamp(2, Timestamp.from(Instant.now()));
//                        insertPs.executeUpdate();
//                        System.out.println("db: created new player: " + name);
//                    }
//                } else {
//                    System.out.println("db: updated existing player: " + name);
//                }
//
//                conn.commit();
//            } catch (SQLException e) {
//                conn.rollback();
//                System.err.println("Ошибка сохранения/обновления игрока: " + e.getMessage());
//            }
//        } catch (SQLException e) {
//            System.err.println("Ошибка подключения: " + e.getMessage());
//        }
        try (Session session = HibernateUtil.getSession()) {
            Transaction tx = session.beginTransaction();

            PlayerStats player = session.bySimpleNaturalId(PlayerStats.class)
                    .load(name);

            if (player == null) {
                player = new PlayerStats(name);
                System.out.println("db: created new player: " + name);
            } else {
                System.out.println("db: updated existing player: " + name);
            }

            player.updateLastSeen();
            session.saveOrUpdate(player);
            tx.commit();
        } catch (Exception e) {
            System.err.println("Ошибка сохранения игрока: " + e.getMessage());
        }
    }

    public static synchronized void incrementWins(String name) {
//        try(Connection conn = DataBase.getConnection()) {
//            conn.setAutoCommit(false);
//            try(PreparedStatement ps = conn.prepareStatement("UPDATE PlayerStats SET wins = wins + 1 WHERE name = ?")) {
//                ps.setString(1, name);
//                int affectedRows = ps.executeUpdate();
//                conn.commit();
//                if (affectedRows == 0) {
//                    throw new SQLException("Creating PlayerStats failed, no rows affected");
//                }
//            } catch (SQLException e) {
//                conn.rollback();
//                System.err.println("Ошибка обновления побед: " + e.getMessage());
//            }
//        } catch (SQLException e) {
//            System.err.println("Ошибка подключения: " + e.getMessage());
//        }
        Session session = HibernateUtil.getSession();
        Transaction tx = null;
        try {
            tx = session.beginTransaction();

            PlayerStats player = session.bySimpleNaturalId(PlayerStats.class)
                    .load(name);

            if (player != null) {
                player.setWins(player.getWins() + 1);
                session.merge(player);
                tx.commit();
                System.out.println("Successfully incremented wins for " + name);
            } else {
                System.out.println("Player " + name + " not found in database");
                if (tx != null) tx.rollback();
            }
        } catch (Exception e) {
            System.err.println("Error incrementing wins: " + e.getMessage());
            if (tx != null) tx.rollback();
        } finally {
            session.close();
        }
    }

    public static synchronized void incrementTotalPoints(String name, Integer points) {
//        try(Connection conn = DataBase.getConnection()) {
//            conn.setAutoCommit(false);
//            try(PreparedStatement ps = conn.prepareStatement("UPDATE PlayerStats SET totalPoints = totalPoints + ? WHERE name = ?")) {
//                ps.setDouble(1, points);
//                ps.setString(2, name);
//                int affectedRows = ps.executeUpdate();
//                conn.commit();
//                if (affectedRows == 0) {
//                    throw new SQLException("Creating PlayerStats failed, no rows affected");
//                }
//            } catch (SQLException e) {
//                conn.rollback();
//                System.err.println("Ошибка обновления общих очков: " + e.getMessage());
//            }
//        } catch (SQLException e) {
//            System.err.println("Ошибка подключения: " + e.getMessage());
//        }
        Session session = null;
        Transaction tx = null;
        try {
            session = HibernateUtil.getSession();
            tx = session.beginTransaction();

            PlayerStats player = session.byNaturalId(PlayerStats.class)
                    .using("name", name)
                    .load();

            if (player != null) {
                player.setTotalPoints(player.getTotalPoints() + points);
                session.update(player);
                tx.commit();
                System.out.println("Added " + points + " points to " + name +
                        ", total points: " + player.getTotalPoints());
            } else {
                System.err.println("Player not found: " + name);
                if (tx != null) tx.rollback();
            }
        } catch (Exception e) {
            System.err.println("Error incrementing points: " + e.getMessage());
            if (tx != null) tx.rollback();
        } finally {
            if (session != null) session.close();
        }
    }

    public static synchronized void updateLastSeen(String name) {
//        try(Connection conn = DataBase.getConnection()) {
//            conn.setAutoCommit(false);
//            try(PreparedStatement ps = conn.prepareStatement("UPDATE PlayerStats SET lastSeen = ? WHERE name = ?")) {
//                ps.setTimestamp(1, Timestamp.from(Instant.now()));
//                ps.setString(2, name);
//                int affectedRows = ps.executeUpdate();
//                conn.commit();
//                if (affectedRows == 0) {
//                    throw new SQLException("Creating PlayerStats failed, no rows affected");
//                }
//            } catch (SQLException e) {
//                conn.rollback();
//                System.err.println("Ошибка обновления даты активности: " + e.getMessage());
//            }
//        } catch (SQLException e) {
//            System.err.println("Ошибка подключения: " + e.getMessage());
//        }
        savePlayerStats(name);
    }

    public static void shutdown() {
        HibernateUtil.shutdown();
    }
}
