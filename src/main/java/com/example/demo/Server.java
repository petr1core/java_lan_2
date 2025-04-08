package com.example.demo;

import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;

public class Server {
    private static final int PORT = 8080;
    static final Set<ClientHandler> clients = Collections.synchronizedSet(new HashSet<>());
    protected static final List<ProjectileData> activeProjectiles = Collections.synchronizedList(new ArrayList<>());
    protected static final List<TargetData> activeTargets = Collections.synchronizedList(new ArrayList<>());
    static final Gson gson = new Gson();
    protected static boolean paused = false;

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Server started on port " + PORT);
            startGameLoop();
            while (!Thread.interrupted()) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    System.out.println("New connection accepted");
                    new ClientHandler(clientSocket).start();
                } catch (IOException e) {
                    System.err.println("Error accepting connection: " + e.getMessage());
                    // Продолжаем работу сервера после ошибки
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void broadcastLobbyUpdate() {
        List<String> nicknames = new ArrayList<>();
        Set<String> readyPlayers = new HashSet<>();

        synchronized (clients) {
            for (ClientHandler client : clients) {
                if (client.getNickname() != null) {
                    nicknames.add(client.getNickname());
                    if(client.isReady()) readyPlayers.add(client.getNickname());
                }
            }
        }

        Message msg = new Message();
        msg.setType(MessageType.LOBBY_UPDATE);
        msg.setNicknames(nicknames);
        msg.setReadyPlayers(readyPlayers);

        //System.out.println("[SERVER] Sending lobby update: " + nicknames);

        String json = gson.toJson(msg);
        synchronized (clients) {
            for (ClientHandler client : clients) {
                client.sendMessage(json);
            }
        }
    }

    public static void resetAllReadyStatus() {
        synchronized (clients) {
            for (ClientHandler client : clients) {
                client.setIsReady(false);
            }
        }
        broadcastLobbyUpdate();
    }

    public static void startGameLoop() {
        new Timer().scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (!paused){
                    updateProjectilesPositions();
                    updateTargetsPosition();
                    removeOutOfBoundsProjectiles();
                    broadcastProjectiles(activeProjectiles);
                    broadcastTargets();
                    broadcastGameState();
                }
            }
        }, 0, Config.PING_PERIOD); // Рассылать каждые PING_PERIOD (40мс)
    }

    private static void updateTargetsPosition() {
        synchronized (activeTargets) {
            for(TargetData target : activeTargets) {
                // Обновление позиции на сервере
                target.setPosY(target.getPosY() + target.getSpeed() * 0.05);

                // Проверка границ
                if(target.getPosY() < 0 ||
                        target.getPosY() + (50 * target.getSize()) > Config.getCanvasHeight()) {
                    target.setSpeed(-target.getSpeed());
                }
            }
        }
    }

    public static void broadcastGameState() {
        Message msg = new Message();
        msg.setType(MessageType.GAME_STATE_UPDATE);

        Map<String, Double> positions = new HashMap<>();
        Map<String, Integer> scores = new HashMap<>();
        Map<String, Integer> arrows = new HashMap<>();

        synchronized(clients) {
            for (ClientHandler client : clients) {
                String nick = client.getNickname();
                if (nick != null) {
                positions.put(client.getNickname(), client.getPlayerY());
                scores.put(client.getNickname(), client.getScore());
                arrows.put(nick, client.getArrowsCount());
                }
            }
        }

        msg.setPlayerPositions(positions);
        msg.setScores(scores);
        msg.setArrows(arrows);

        String json = gson.toJson(msg);
        synchronized(clients) {
            for(ClientHandler client : clients) {
                client.sendMessage(json);
            }
        }

        // Проверка на победу
        Optional<Map.Entry<String, Integer>> winner = scores.entrySet()
                .stream()
                .filter(e -> e.getValue() >= Config.GAME_WIN_SCORE)
                .findFirst();

        if (winner.isPresent()) {
            Message gameOverMsg = new Message();
            gameOverMsg.setType(MessageType.GAME_OVER);
            gameOverMsg.setContent(winner.get().getKey());

            resetAllReadyStatus();

            String json_ = gson.toJson(gameOverMsg);
            synchronized(clients) {
                for(ClientHandler client : clients) {
                    client.sendMessage(json_);
                    client.resetGameState();
                }
            }
            return;
        }
    }


    public static void broadcastProjectiles(List<ProjectileData> projectiles) {
        Message msg = new Message();
        msg.setType(MessageType.PROJECTILE_UPDATE);
        msg.setProjectiles(projectiles);

        String json = gson.toJson(msg);
        synchronized (clients) {
            for(ClientHandler client : clients) {
                client.sendMessage(json);
            }
        }
    }

    public static void spawnAndBroadcastTargets() {
        double areaStart = Config.getCanvasWidth() - 100; // отступ области спавна
        double areaHeight = Config.getCanvasHeight() - 150; // Высота области спавна
        int cols = 4; // Количество рядов = Количество мишеней

        List<TargetData> newTargets = new ArrayList<>();
        Random rand = new Random();
        for(int col = 0; col < cols; col++) {
            double size = 0.5 + rand.nextDouble() * 0.7; // Размер 0.5-1.2
            double x = areaStart - (col * 60); // Распределение по X
            double y = areaHeight + rand.nextDouble() * 50;

            TargetData target = new TargetData(
                    x,
                    y,
                    size,
                    Config.ARCHER_TARGET_SPEED,
                    (int) (50 / size),
                    col
            );
            newTargets.add(target);
        }

        activeTargets.clear();
        activeTargets.addAll(newTargets);

        Message spawnMsg = new Message();
        spawnMsg.setType(MessageType.TARGETS_SPAWN);
        spawnMsg.setTargets(newTargets);
        String json = gson.toJson(spawnMsg);
        synchronized (clients) {
            for(ClientHandler client : clients) {
                client.sendMessage(json);
            }
        }

        //broadcastTargets();
    }

    protected static void broadcastTargets() {
        List<TargetData> copy;
        synchronized (activeTargets) { // Блок синхронизации
            copy = new ArrayList<>(activeTargets); // Создаем копию
        }

        Message msg = new Message();
        msg.setType(MessageType.TARGETS_UPDATE);
        msg.setTargets(copy); // Используем копию для сериализации

        String json = gson.toJson(msg);
        synchronized (clients) {
            for(ClientHandler client : clients) {
                client.sendMessage(json);
            }
        }
    }

    public static void addProjectile(ProjectileData projectile) {
        activeProjectiles.add(projectile);
    }

    public static void removeOutOfBoundsProjectiles() {
        synchronized (activeProjectiles) {
            activeProjectiles.removeIf(p ->
                    p.getPosX() > Config.getWindowWidth() ||
                            p.getPosX() < 0 ||
                            p.getPosY() < 0 ||
                            p.getPosY() > Config.getWindowHeight()
            );
        }
    }

    public static void updateProjectilesPositions() {
        long currentTime = System.currentTimeMillis();
        synchronized (activeProjectiles) {
            for (ProjectileData p : activeProjectiles) {
                double deltaTime = (currentTime - p.getCreationTime()) / 1000.0;
                p.setPosX(p.getPosX() + p.getVelocity() * deltaTime);
            }
        }
    }

}

class ClientHandler extends Thread {
    private Socket socket;
    private PrintWriter out;
    private String nickname;
    private Boolean isReady = false;
    private double playerY;

    private int arrowsCount = Config.ARCHER_INIT_ARROWS;
    private static final int READY_TIMER_TIME = 1; // milliseconds
    private static Timer gameStartTimer;
    private static boolean isCountdownActive = false;

    private int score = 0;

    public ClientHandler(Socket clientSocket) {
        this.socket = clientSocket;
    }

    public void sendMessage(String json) {
        out.println(json);
    }

    public String getNickname() {
        return nickname;
    }

    public void setIsReady(Boolean isReady) {
        this.isReady = isReady;
    }

    public boolean isReady() {
        return isReady;
    }

    public double getPlayerY() {
        return this.playerY;
    }

    public int getScore() {
        return score;
    }

    public int getArrowsCount() {
        return arrowsCount;
    }

    @Override
    public void run() {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {

            this.out = out;

            String joinMessage = in.readLine();
            if (joinMessage == null) return;

            Message joinMsg = Server.gson.fromJson(joinMessage, Message.class);
            if (joinMsg.getType() == MessageType.JOIN) {
                // Обработка nickname
                nickname = joinMsg.getContent().trim();

                // Проверка уникальности
                synchronized (Server.clients) {
                    if (Server.clients.stream().anyMatch(c -> c.nickname.equals(nickname))) {
                        sendMessage(Server.gson.toJson(new Message(MessageType.ERROR, "Name taken")));
                        return;
                    }
                }

                Server.clients.add(this);
                this.isReady = false;
                System.out.println("Registered: " + nickname);
            }

            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                //System.out.println("[SERVER] Received: " + inputLine);
                Message msg = Server.gson.fromJson(inputLine, Message.class);
                switch (msg.getType()) {
                    case READY:
                        this.isReady = !this.isReady; // Переключаем статус
                        System.out.println(nickname + " ready status: " + isReady);
                        Server.broadcastLobbyUpdate();
                        checkAllReady();
                        break;
                    case PAUSE:
                        Server.paused = true;
                        break;
                    case RESUME:
                        Server.paused = false;
                        break;
                    case PLAYER_MOVE:
                        this.playerY = msg.getPlayerY();
                        break;
                    case SCORE_UPDATE:
                        this.score++;
                        break;
                    case PLAYER_SHOOT:
                        this.arrowsCount--;
                        //projectile data
                        ProjectileData newProjectile = msg.getProjectiles().get(0);
                        newProjectile.setVelocity(Config.ARCHER_ARROW_SPEED);
                        newProjectile.setShooter(this.nickname);
                        // server time stamp
                        newProjectile.setServerTime(System.currentTimeMillis());

                        Server.addProjectile(newProjectile); // add arrow on server
                        break;
                    case HIT:
                        // Проверяем, принадлежит ли снаряд текущему игроку
                        ProjectileData hitProjectile = null;
                        synchronized (Server.activeProjectiles) {
                            for (ProjectileData p : Server.activeProjectiles) {
                                if (p.getId().equals(msg.getHitArrowId())) {
                                    hitProjectile = p;
                                    break;
                                }
                            }
                        }

                        if (hitProjectile != null && hitProjectile.getShooter().equals(this.nickname)) {
                            this.score += msg.getPoints();
                            this.arrowsCount++; // Возвращаем стрелу за попадание
                        }

                        String hitTargetId = msg.getHitTargetId();
                        TargetData destroyedTarget = null;

                        System.out.println("HIT " + msg.toString());

                        synchronized(Server.activeTargets) {
                            Iterator<TargetData> iterator = Server.activeTargets.iterator();
                            while(iterator.hasNext()) {
                                TargetData t = iterator.next();
                                if(t.getId().equals(hitTargetId)) {
                                    destroyedTarget = t;
                                    //destroyedTarget.setId(UUID.randomUUID().toString());
                                    System.out.println("Destroyed: " + hitTargetId);
                                    iterator.remove();
                                    break;
                                }
                            }

                        }
                        // Создаем новую мишень в той же колонке

                        if(destroyedTarget != null) {
                            System.out.println("Started to respawn target: " + destroyedTarget.getId());
                            spawnNewTargetInColumn(destroyedTarget.getColumn());
                        }

                        synchronized (Server.activeProjectiles) {
                            Server.activeProjectiles.removeIf(p -> p.getId().equals(msg.getHitArrowId()));
                        }
                        Server.broadcastGameState(); //
                        Server.broadcastTargets();
                        break;

                    case RETURN_TO_LOBBY:
                        setIsReady(false);
                        Server.broadcastLobbyUpdate();
                        break;

                    case EXIT:
                        throw new IOException("Client exited");
                }
            }
        } catch (IOException e) {
            System.out.println("Client disconnected: " + nickname + " cause: " + e.getMessage());
        } finally {
            Server.clients.remove(this);
            Server.broadcastLobbyUpdate();
        }
    }


    public void resetGameState() {
        this.score = 0;
        this.arrowsCount = Config.ARCHER_INIT_ARROWS;
    }

    private void checkAllReady() {
        synchronized (Server.clients) {
            boolean allReady = Server.clients.stream().allMatch(ClientHandler::isReady);
            int playersCount = Server.clients.size();
            if (allReady && playersCount >= 2 && !isCountdownActive) {
                startGameCountdown();
            } else if ((!allReady || Server.clients.size() < 2) && isCountdownActive) {
                cancelGameCountdown();
            }
        }
    }

    private void startGameCountdown() {
        isCountdownActive = true;
        gameStartTimer = new Timer();
        gameStartTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (checkReadyStatus()){
                    Server.spawnAndBroadcastTargets();
                    Message startMsg = new Message();
                    startMsg.setType(MessageType.GAME_START);
                    String json = Server.gson.toJson(startMsg);
                    for (ClientHandler client : Server.clients) {
                        client.sendMessage(json);
                    }
                }
                isCountdownActive = false;
            }
        }, READY_TIMER_TIME);
    }

    private void cancelGameCountdown() {
        if (gameStartTimer != null) {
            gameStartTimer.cancel();
            gameStartTimer = null;
        }
        isCountdownActive = false;

        Message cancelMsg = new Message();
        cancelMsg.setType(MessageType.GAME_CANCEL);
        String json = Server.gson.toJson(cancelMsg);
        for (ClientHandler client : Server.clients) {
            client.sendMessage(json);
        }
    }

    private static void spawnNewTargetInColumn(int column) {
        double areaStart = Config.getCanvasWidth() - 100;
        Random rand = new Random();

        // Генерация позиции Y с проверкой коллизий
        double newY;
        boolean collision;
        int attempts = 0;

        double newSize;
        do {
            collision = false;
            newY = rand.nextDouble() * (Config.getCanvasHeight() - 150);
            newSize = 0.5 + rand.nextDouble() * 0.7;
            double newHeight = 50 * newSize;

            // Проверка пересечения с существующими мишенями
            synchronized(Server.activeTargets) {
                for(TargetData t : Server.activeTargets) {
                    if(t.getColumn() == column &&
                            Math.abs(t.getPosY() - newY) < (newHeight + 50 * t.getSize())) {
                        collision = true;
                        break;
                    }
                    //System.out.println("collision = false;" + (t.getColumn() == column) + " " + (Math.abs(t.getPosY() - newY) < (newHeight + 50 * t.getSize())));
                }
            }
            attempts++;
        } while(collision && attempts < 10); // Максимум 10 попыток

        // Создаем новую мишень
        TargetData newTarget = new TargetData(
                areaStart - (column * 60),
                newY,
                0.5 + rand.nextDouble() * 0.7,
                Config.ARCHER_TARGET_SPEED,
                (int) (50 / newSize),
                column
        );
        newTarget.setId(UUID.randomUUID().toString()); // Генерируем новый ID

        synchronized(Server.activeTargets) {
            Server.activeTargets.add(newTarget);
            //System.out.println(Server.activeTargets.toArray().length);
        }

        // Рассылаем обновление
        Server.broadcastTargets();
    }

    private boolean checkReadyStatus() {
        synchronized (Server.clients) {
            return Server.clients.stream().allMatch(ClientHandler::isReady) && Server.clients.size() >= 2;
        }
    }
}