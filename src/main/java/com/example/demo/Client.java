package com.example.demo;

import java.awt.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import javafx.animation.AnimationTimer;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.effect.Light;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;
import javafx.util.Duration;

public class Client extends Application {
    private static final int WIDTH = Config.getWindowWidth();
    private static final int HEIGHT = Config.getWindowHeight();
    private static final int CANVAS_HEIGHT = Config.getCanvasHeight();
    private static final int CANVAS_WIDTH = Config.getCanvasWidth();

    // UI Components
    private final StackPane root = new StackPane();
    private final VBox connectionPane = new VBox(10);

    private final VBox lobbyPane  = new VBox(10);
    private final Label timerLabel = new Label();

    private VBox gamePane = new VBox(10);
    private final Button btnShoot = new Button("Shoot");
    private final Button btnPause = new Button("Pause");

    // Game components
    private Archer playerArcher;
    private final List<Projectile> activeProjectiles = new ArrayList<>();
    private boolean isUpPressed = false;
    private boolean isDownPressed = false;
    private Canvas gameCanvas;
    private final Map<String, Archer> otherPlayers = new HashMap<>();
    private String nickName;
    private final List<Target> targets = new ArrayList<>();
    private final IntegerProperty score = new SimpleIntegerProperty(0);
    private final IntegerProperty arrows = new SimpleIntegerProperty(Config.ARCHER_INIT_ARROWS);
    private boolean canShoot = true;
    private final Timeline shootCooldownTimer = new Timeline( new KeyFrame(Duration.millis(Config.ARCHER_COOLDOWN_MILLIS), e -> canShoot = true));
    private boolean isPaused = false;
    AnimationTimer gameLoop;

    // Network
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;

    // Player update (delta-time + delta-transition)
    private long lastUpdateTime = 0;
    private double lastSentY = -1;
    private static final long UPDATE_INTERVAL_MS = 50;
    private static final double MIN_DELTA = 1.0;

    private static final String SERVER_IP = "localhost";
    private static final int SERVER_PORT = 8080;
    private static final Gson gson = new Gson();

    private static final int READY_TIMER_TIME = 1; // milliseconds
    private Timeline timeline;

    private double currentDeltaTime = 0.0;

    private final ObservableList<String> lobbyPlayers = FXCollections.observableArrayList();
    private final ListView<String> playerListView = new ListView<>();

    @Override
    public void start(Stage stage) throws Exception {
        initializeUI(stage);
        stage.setOnCloseRequest(event -> {
            try {
                if (socket != null && !socket.isClosed()) {
                    Message existMsg = new Message(MessageType.EXIT, (String) null);
                    out.println(gson.toJson(existMsg));
                    socket.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    private void initializeUI(Stage stage) {
        // UI Panels
        createConnectionPane();

        // Assembly
        root.getChildren().addAll(connectionPane);

        // Scene Setup
        Scene scene = new Scene(root, WIDTH, HEIGHT);
        stage.setScene(scene);
        stage.setWidth(WIDTH);
        stage.setMinWidth(WIDTH);
        stage.setMaxWidth(WIDTH);
        stage.setHeight(HEIGHT);
        stage.setMinHeight(HEIGHT);
        stage.setMaxHeight(HEIGHT);

        stage.setTitle("Archery Battle");
        stage.show();
    }

    private void createConnectionPane() {
        connectionPane.setAlignment(Pos.CENTER);

        TextField nameField = new TextField();
        nameField.setPromptText("Enter your nickname");

        Button connectButton = new Button("Connect");
        connectButton.disableProperty().bind(
                nameField.textProperty().isEmpty().or(
                        nameField.textProperty().length().greaterThan(20)
                )
        );
        connectButton.setOnAction(e -> {
            connectToServer(nameField.getText());
            nickName = nameField.getText();
        });

        Label enterNameLabel = new Label("Enter your nickname");

        connectionPane.getChildren().addAll(enterNameLabel, nameField,connectButton);
    }

    private void createLobbyPane() {
        lobbyPane.setAlignment(Pos.CENTER);

        playerListView.setPrefSize(300, 200);
        playerListView.setCellFactory(lv -> new ListCell<String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    Label playerLabel = new Label(item);
                    if (item.startsWith("✓")) {
                        playerLabel.setTextFill(Color.GREEN);
                    } else {
                        playerLabel.setTextFill(Color.RED);
                    }
                    setGraphic(playerLabel);
                }
            }
        });

        Button readyButton = new Button("Ready");
        readyButton.setOnAction(e -> {
            Message readyMsg = new Message();
            readyMsg.setType(MessageType.READY);
            out.println(gson.toJson(readyMsg));
        });

        String defTimerVal = Integer.toString(READY_TIMER_TIME / 1000);
        timerLabel.setText(defTimerVal);
        timerLabel.setVisible(false);

        VBox lobbyContent = new VBox(10,
                new Label("Connected players:"),
                playerListView,
                readyButton,
                timerLabel
        );
        lobbyContent.setAlignment(Pos.CENTER);

        lobbyPane.getChildren().setAll(lobbyContent);
    }

    public void createGamePane() {
        // Инициализация лучника
        playerArcher = new Archer(nickName, (double) HEIGHT /2, CANVAS_WIDTH, false);
        arrows.bind(playerArcher.arrowsCountProperty());

        BorderPane gameContainer = new BorderPane();

        // Создаем игровое поле
        gameCanvas = new Canvas(CANVAS_WIDTH, CANVAS_HEIGHT);
        gameCanvas.setFocusTraversable(true);
        gameContainer.requestFocus();

        HBox controlPanel = createControlPanel();
        VBox statsPanel = createStatsPanel();

        controlPanel.setPrefHeight(Config.getControlPanelHeight());
        statsPanel.setPrefWidth(Config.getStatsPanelWidth());

        gameContainer.setCenter(gameCanvas);
        gameContainer.setBottom(controlPanel);
        gameContainer.setRight(statsPanel);

        gamePane = new VBox(gameContainer);
        gamePane.setAlignment(Pos.CENTER_LEFT);
    }

    private HBox createControlPanel() {
        HBox controlPanel = new HBox(10);
        controlPanel.setPadding(new Insets(10));
        controlPanel.setStyle("-fx-background-color: #EEEEEE;");

        btnShoot.setStyle("-fx-base: #4CAF50;");
        btnShoot.setOnAction(e -> handleShoot());

        btnPause.setOnAction(e -> togglePause());
        btnPause.setStyle("-fx-base: #FFC107;");

        controlPanel.getChildren().addAll(btnShoot, btnPause);
        return controlPanel;
    }

    private VBox createStatsPanel() {
        VBox statsPanel = new VBox(10);
        statsPanel.setPadding(new Insets(10));
        statsPanel.setStyle("-fx-background-color: #AAAAAA;");
        statsPanel.setStyle("-fx-background-color: rgba(0,0,0,0.7); -fx-background-radius: 10;");
        statsPanel.setMinWidth(Config.getStatsPanelWidth());

        Label title = new Label("Player Statistics");
        title.setStyle("-fx-font-weight: bold; -fx-font-size: 14;");
        title.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");

        // Контейнер для записей игроков
        VBox playersStats = new VBox(5);
        playersStats.setId("playersStats");

        Button btnLeaderboard = new Button("Leaderboard");
        btnLeaderboard.setOnAction(e -> showLeaderboard());

//        Label lblScore = new Label();
//        lblScore.textProperty().bind(score.asString("Score: %d"));
//        Label lblArrows = new Label();
//        lblArrows.textProperty().bind(arrows.asString("Arrows: %d"));

        statsPanel.getChildren().addAll(title, playersStats, btnLeaderboard);
        return statsPanel;
    }

    private void showLeaderboard() {
        Stage leaderboardStage = new Stage();
        leaderboardStage.setTitle("Top Players");
        TableView<PlayerStats> table = new TableView<>();
        table.setSortPolicy(null);

        // Колонка с именем
        TableColumn<PlayerStats, String> nameColumn = new TableColumn<>("Player");
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));

        // Колонка с победами
        TableColumn<PlayerStats, Integer> winsColumn = new TableColumn<>("Wins");
        winsColumn.setCellValueFactory(new PropertyValueFactory<>("wins"));

        // Колонка с общим числом очков
        TableColumn<PlayerStats, Integer> PointsColumn = new TableColumn<>("Total points");
        PointsColumn.setCellValueFactory(new PropertyValueFactory<>("totalPoints"));

        // Колонка с общим числом очков
        TableColumn<PlayerStats, Integer> DateColumn = new TableColumn<>("Last activity");
        DateColumn.setCellValueFactory(new PropertyValueFactory<>("lastSeen"));

        table.getColumns().addAll(nameColumn, winsColumn, PointsColumn, DateColumn);

        // Загрузка данных в фоновом потоке
        new Thread(() -> {
            List<PlayerStats> leaders = DataBase.getLeaderboard();
            Platform.runLater(() -> table.getItems().setAll(leaders));
        }).start();

        VBox vbox = new VBox(table);
        Scene scene = new Scene(vbox, 450, 400);
        leaderboardStage.setScene(scene);
        leaderboardStage.show();
    }

    private void updatePlayersStats(Map<String, Integer> scores, Map<String, Integer> arrows) {
        VBox statsContainer = (VBox) gamePane.lookup("#playersStats");
        if (statsContainer == null) return;

        statsContainer.getChildren().clear();

        scores.forEach((playerName, score) -> {
            int playerArrows = arrows.getOrDefault(playerName, 0);
            HBox entry = createPlayerEntry(
                    playerName,
                    score,
                    arrows.getOrDefault(playerName, 0),
                    playerName.equals(nickName) // isCurrentPlayer
            );
            statsContainer.getChildren().add(entry);
        });
    }

    private void showPauseScreen() {
        VBox pauseBox = new VBox(20);
        pauseBox.setAlignment(Pos.CENTER);
        Button btnResume = new Button("Resume");
        btnResume.setOnAction(e -> togglePause());

        pauseBox.getChildren().addAll(
                new Label("Game Paused"),
                btnResume
        );
        root.getChildren().setAll(pauseBox);
    }

    private void hidePauseScreen() {
        root.getChildren().setAll(gamePane);
    }

    private void showGameOverScreen(String winner) {
        VBox gameOverBox = new VBox(20);
        gameOverBox.setAlignment(Pos.CENTER);

        Button btnLobby = new Button("Return to Lobby");
        btnLobby.setOnAction(e -> switchToLobby());

        gameOverBox.getChildren().addAll(
                new Label("Winner: " + winner),
                btnLobby
        );


        Platform.runLater(() -> {
            root.getChildren().clear();
            root.getChildren().add(gameOverBox);
        });
    }

    private HBox createPlayerEntry(String name, int score, int arrows, boolean isCurrent) {
        HBox entry = new HBox(10);
        entry.setAlignment(Pos.CENTER_LEFT);
        entry.setPadding(new Insets(5));
        entry.setPrefWidth(Config.STATS_ENTRY_WIDTH);

        // Индикатор игрока
        Circle indicator = new Circle(5);
        indicator.setFill(isCurrent ? Config.PLAYER_COLOR : Config.ENEMY_COLOR);

        // Имя игрока
        Label nameLabel = new Label(name + ":");
        nameLabel.setStyle("-fx-font-weight: bold;");

        nameLabel.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");

        // Счет и стрелы
        Label statsLabel = new Label(String.format("%d pts | %d \u27B6", score, arrows));
        statsLabel.setStyle("-fx-text-fill: white;");

        entry.getChildren().addAll(indicator, nameLabel, statsLabel);
        return entry;
    }

    private void switchToConnection(){
        root.getChildren().setAll(connectionPane);
    }

    private void switchToLobby() {
        createLobbyPane();
        playerListView.setItems(lobbyPlayers);
        Platform.runLater(() -> {
            root.getChildren().setAll(lobbyPane);
            lobbyPane.requestFocus();
        });
    }

    private void returnToLobby() {
        Message lobbyMsg = new Message();
        lobbyMsg.setType(MessageType.RETURN_TO_LOBBY);
        out.println(gson.toJson(lobbyMsg));

        switchToLobby();
    }

    private void switchToGame() {
        createGamePane();
        root.getChildren().setAll(gamePane);
        gamePane.requestFocus();
    }

    private void connectToServer(String nickname) {
        Executors.newSingleThreadExecutor().submit(() -> {
            try {
                socket = new Socket(SERVER_IP, SERVER_PORT);
                out = new PrintWriter(socket.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                // Отправка JOIN-сообщения
                Message joinMsg = new Message();
                joinMsg.setType(MessageType.JOIN);
                joinMsg.setContent(nickname);
                String json = gson.toJson(joinMsg);
                out.println(json);
                System.out.println("Sent JOIN: " + json);

                // Поток чтения сообщений
                new Thread(() -> {
                    try {
                        String serverResponse;
                        while ((serverResponse = in.readLine()) != null) {
                            final String message = serverResponse;
                            Platform.runLater(() -> handleServerMessage(message));
                        }
                    } catch (IOException e) {
                        Platform.runLater(() -> showError("Disconnected from server"));
                    }
                }).start();

                Platform.runLater(() -> {
                    switchToLobby();
                    System.out.println("Switched to LobbyPane");
                });

            } catch (IOException e) {
                Platform.runLater(() -> showError("Connection failed!"+ e.getMessage()));
            }
        });
    }

    private boolean needSendUpdate() {
        long currentTime = System.currentTimeMillis();
        double currentY = playerArcher.getPosY();

        if (currentTime - lastUpdateTime >= UPDATE_INTERVAL_MS
                && Math.abs(currentY - lastSentY) >= MIN_DELTA) {

            lastUpdateTime = currentTime;
            lastSentY = currentY;
            return true;
        }
        return false;
    }

    private void updateGame(double deltaTime) {
        // archer section
        if (isUpPressed && !isPaused) {
            playerArcher.moveUp(deltaTime);
        }
        if (isDownPressed && !isPaused) {
            playerArcher.moveDown(CANVAS_HEIGHT, deltaTime);
        }

        if (needSendUpdate()) {
            Message moveMsg = new Message();
            moveMsg.setType(MessageType.PLAYER_MOVE);
            moveMsg.setPlayerY(playerArcher.getPosY());
            out.println(gson.toJson(moveMsg));
        }

        // projectiles section
        playerArcher.updateProjectiles(deltaTime);

        checkCollisions();
    }

    public void checkCollisions() {
        Iterator<Projectile> iter = activeProjectiles.iterator();
        while(iter.hasNext()) {
            Projectile p = iter.next();
            Iterator<Target> targetIter = targets.iterator();
            while(targetIter.hasNext()) {
                Target t = targetIter.next();
                if(t.isActive() && t.isHit(p)) {
                    System.out.println("Collision detected: " + p.getId() + " with " + t.getId());

                    Message hitMsg = new Message();
                    hitMsg.setType(MessageType.HIT);
                    hitMsg.setHitTargetId(t.getId());
                    hitMsg.setHitArrowId(p.getId());
                    hitMsg.setPoints(t.getPoints());
                    hitMsg.setShooter(nickName);
                    out.println(gson.toJson(hitMsg));

                    iter.remove();
                    targetIter.remove();
                    break;
                }
            }
        }
    }

    private void startGameLoop() {
        final long[] lastFrameTime = {System.nanoTime()};
        gameLoop = new AnimationTimer() {
            @Override
            public void handle(long now) {
                currentDeltaTime = (now - lastFrameTime[0]) / 1_000_000_000.0;
                lastFrameTime[0] = now;

                // Ограничение максимального deltaTime
                if (currentDeltaTime > 0.05) currentDeltaTime = 0.05;

                Platform.runLater(() -> {
                    updateGame(currentDeltaTime);
                    renderGame();
                });
            }
        };
        gameLoop.start();
    }

    private void setupInputHandlers() {
        Scene scene = root.getScene();
        scene.setOnKeyPressed(e -> {
            switch (e.getCode()) {
                case W -> isUpPressed = true;
                case S -> isDownPressed = true;
                case SPACE -> handleShoot();
            }
        });

        scene.setOnKeyReleased(e -> {
            switch (e.getCode()) {
                case W -> isUpPressed = false;
                case S -> isDownPressed = false;
            }
        });

        shootCooldownTimer.setCycleCount(1);
    }

    private void renderGame() {
        GraphicsContext gc = gameCanvas.getGraphicsContext2D();
        gc.clearRect(0, 0, CANVAS_WIDTH, CANVAS_HEIGHT);

        gc.setFill(new Color(0.1, 0.5, 0.5, 1));
        gc.fillRect(764,50,10,10);

        playerArcher.draw(gc);
        otherPlayers.values().forEach(archer -> archer.draw(gc));

        otherPlayers.values().forEach(archer -> {
            archer.updatePosition(currentDeltaTime);
            archer.draw(gc);
        });

        System.out.println("Rendering: " + activeProjectiles.size() + " projectiles, " +
                targets.size() + " targets");

        synchronized (activeProjectiles) {
            activeProjectiles.forEach(p -> p.draw(gc));
        }

        synchronized (targets) {
            targets.forEach(target -> {
                target.updatePosition(currentDeltaTime);
                target.draw(gc);
            });
        }
    }

    private void togglePause() {
        isPaused = !isPaused;
        Message pauseMsg = new Message();
        pauseMsg.setType(isPaused ? MessageType.PAUSE : MessageType.RESUME);
        out.println(gson.toJson(pauseMsg));

        if (isPaused) {
            gameLoop.stop();
            btnPause.setText("Resume");
        } else {
            gameLoop.start();
            btnPause.setText("Pause");
        }
        System.out.println("Game paused/resumed");

    }

    private void handleShoot() {
        if (canShoot && playerArcher.shoot()) {
            canShoot = false;
            shootCooldownTimer.playFromStart();
            Projectile newProjectile = playerArcher.getProjectiles().getLast();

            Message shootMsg = new Message();
            shootMsg.setType(MessageType.PLAYER_SHOOT);
            shootMsg.setProjectiles(Collections.singletonList(newProjectile.toData()));
            out.println(gson.toJson(shootMsg));
        }
    }

    private void handleServerMessage(String message) {
        //System.out.println("[CLIENT] Received: " + message);
        try {
            Message msg = gson.fromJson(message, Message.class);
            switch (msg.getType()) {
                case LOBBY_UPDATE:
                    Platform.runLater(() -> {
                        lobbyPlayers.clear();
                        msg.getNicknames().forEach(nick -> {
                            String status = msg.getReadyPlayers().contains(nick)
                                    ? "✓ " + nick
                                    : "✗ " + nick;
                            lobbyPlayers.add(status);
                        });
                    });
                    break;
                case ERROR:
                    Platform.runLater(() -> {
                        showError(msg.getContent());
                        switchToConnection();
                    });
                    break;
                case GAME_START:
                    Platform.runLater(() -> {
                        System.out.println("[CLIENT] Game countdown running!");
                        startGameCountDown();
                        System.out.println("Switched to gamePane");
                        switchToGame();
                        startGameLoop();
                        setupInputHandlers();
                        renderGame();
                    });
                    break;

                case GAME_CANCEL:
                    Platform.runLater(() -> {
                        cancelGameCountDown();
                        System.out.println("[CLIENT] Game cancelled by some players");
                    });
                    break;

                case GAME_OVER:
                    Platform.runLater(() -> {
                        gameLoop.stop();
                        showGameOverScreen(msg.getContent());
                    });
                    break;

                case PAUSE:
                    Platform.runLater(() -> {
                        isPaused = true;
                        gameLoop.stop();
                        showPauseScreen();
                    });
                    break;
                case RESUME:
                    Platform.runLater(() -> {
                        isPaused = false;
                        gameLoop.start();
                        hidePauseScreen();
                    });
                    break;
                case GAME_STATE_UPDATE:
                    Platform.runLater(() -> {
                        //enemy players section
                        if (playerArcher == null) return;

                        Set<String> activeNicks = new HashSet<>();

                        msg.getPlayerPositions().forEach((nick, y) -> {
                            activeNicks.add(nick);

                            if(nick.equals(playerArcher.getNickname())) return;

                            synchronized (otherPlayers) {
                                otherPlayers.compute(nick, (k,v) -> {
                             if (v == null) {
                                 return new Archer(nick, y, CANVAS_WIDTH, true);
                             } else {
                                 v.setTargetY(y);
                                 return v;
                             }
                            });
                            }
                        });
                        otherPlayers.keySet().removeIf(nick -> !activeNicks.contains(nick) && !nick.equals(playerArcher.getNickname()));

                        // score section
                        if (msg.getScores() != null && msg.getArrows() != null) {
                            score.set(msg.getScores().getOrDefault(nickName, 0));
                            playerArcher.setArrowsCount(msg.getArrows().getOrDefault(nickName, 0));
                            updatePlayersStats(msg.getScores(), msg.getArrows());
                        }
                    });
                    break;
                case PROJECTILE_UPDATE:
                    Platform.runLater(() -> {
                        List<ProjectileData> serverProjectiles = msg.getProjectiles();
                        syncProjectiles(serverProjectiles);
                    });
                    break;
                case TARGETS_SPAWN:
                    Platform.runLater(() -> {
                        targets.clear();
                        for(TargetData tData : msg.getTargets()) {
                            targets.add(new Target(
                                    tData,
                                    Config.TARGET_IMG_PATH
                            ));
                            System.out.println("Spawned target at: " + tData.getPosX() + "," + tData.getPosY() +
                                    " size: " + tData.getSize());
                        }
                    });
                    break;

                case TARGETS_UPDATE:
                    Platform.runLater(() -> {
                        synchronized (targets) {
                            // 1. Собираем ID новых мишеней
                            Set<String> newIds = msg.getTargets().stream()
                                    .map(TargetData::getId)
                                    .collect(Collectors.toSet());

                            // 2. Удаляем старые мишени
                            targets.removeIf(t -> !newIds.contains(t.getId()));

                            // 3. Добавляем/обновляем мишени
                            for (TargetData tData : msg.getTargets()) {
                                Optional<Target> existing = targets.stream()
                                        .filter(t -> t.getId().equals(tData.getId()))
                                        .findFirst();

                                if (existing.isPresent()) {
                                    existing.get().updateFromData(tData);
                                } else {
                                    targets.add(new Target(tData, Config.TARGET_IMG_PATH));
                                }
                            }
                        }
                    });
                    break;
            }
        } catch (JsonSyntaxException e) {
            System.err.println("[CLIENT] JSON error: " + e.getMessage());
        }
    }

    private void syncProjectiles(List<ProjectileData> serverProjectiles) {
        System.out.println("[CLIENT] Syncing projectiles. Server count: " + serverProjectiles.size());
        //delete outdated
        synchronized (activeProjectiles) {
            activeProjectiles.removeIf(p ->
                    serverProjectiles.stream().noneMatch(sp -> sp.getId().equals(p.getId()))
            );

            // Добавляем новые снаряды с сервера
            for(ProjectileData data : serverProjectiles) {
                Optional<Projectile> existing = activeProjectiles.stream()
                        .filter(p -> p.getId().equals(data.getId()))
                        .findFirst();

                if (existing.isPresent()) {
                    existing.get().setPosX(data.getPosX());
//                    Projectile p = existing.get();
//                    p.setPosX(data.getPosX());
//                    p.setPosY(data.getPosY());
                } else {
                    activeProjectiles.add(new Projectile(data));
                }
            }
        }

    }

    private void startGameCountDown() {
        timerLabel.setVisible(true);
        String defTimerVal = Integer.toString(READY_TIMER_TIME / 1000);
        timerLabel.setText(defTimerVal);

        timeline = new Timeline(
                new KeyFrame(Duration.seconds(1), e -> {
                    int timeLeft = Integer.parseInt(timerLabel.getText()) - 1;
                    if (timeLeft >= 0) {
                        timerLabel.setText(String.valueOf(timeLeft));
                        System.out.println("[CLIENT] Countdown: " + timeLeft);
                    } else {
                        timerLabel.setVisible(false);
                        if (lobbyPlayers.size() >= 2) {
                            Platform.runLater(() -> {
                                switchToGame();
                                startGameLoop();
                                setupInputHandlers();
                                System.out.println("[CLIENT] Game started");
                            });
                        }
                    }
                })
        );
        timeline.setCycleCount(1);
        timeline.play();
    }

    private void cancelGameCountDown() {
        if (timeline != null) {
            timeline.stop();
        }
        timerLabel.setVisible(false);
        String defTimerVal = Integer.toString(READY_TIMER_TIME / 1000);
        timerLabel.setText(defTimerVal);

        showError("Start cancelled!");
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setContentText(message);
        alert.show();
    }
}