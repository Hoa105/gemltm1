package client.GUI;

import client.Client;
import common.Message;
import java.io.IOException;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.animation.KeyFrame;
import javafx.animation.ParallelTransition;
import javafx.animation.PathTransition;
import javafx.animation.PauseTransition;
import javafx.animation.SequentialTransition;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Group;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.media.AudioClip;
import javafx.scene.paint.Color;
import javafx.scene.shape.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.shape.Rectangle;
import javafx.scene.paint.Color;
import javafx.util.Duration;
import java.util.ArrayList;
import java.util.List;

public class GameRoomController {

    @FXML private TextArea chatArea;
    @FXML private TextField chatInput;
    @FXML private Button shootButton;
    @FXML private Button goalkeeperButton;
    @FXML private Button quitButton;
    @FXML private Pane gamePane;
    @FXML private Label scoreLabel;
    @FXML private Label timerLabel;

    private Client client;
    private final ChoiceDialog<String> dialog = new ChoiceDialog<>("Middle", "Left", "Middle", "Right");

    // -------------------------
    //   Tham số đồ họa cố định
    // -------------------------
    private static final double PLAYER_W = 70 * 3;
    private static final double PLAYER_H = 110;
    private static final double KEEPER_H = 110;
    // GOAL_WIDTH bây giờ là fallback; thực tế sẽ lấy từ ảnh goal.png đã scale
    private static final double GOAL_WIDTH_FALLBACK = 200;

    // -------------------------
    //  Hằng số chuyển động sút
    // -------------------------
    private static final double KICK_STEP_Y = 40;
    private static final Duration KICK_IN_TIME  = Duration.millis(300);
    private static final Duration KICK_BACK_TIME = Duration.millis(300);

    // -------------------------
    //        Node đồ họa
    // -------------------------
    private Group ball;
    private Circle ballCircle;
    private Group goalkeeper;
    private Group player;
    private Group imageWinGroup;
    private Group imageLoseGroup;

    // Ảnh nền & khung thành
    private ImageView fieldBgView;   // sân.png
    private ImageView goalView;      // goal.png

    // Kích thước/biên khung thành hiện tại (đọc từ ảnh goal.png sau khi scale)
    private double currentGoalWidth = GOAL_WIDTH_FALLBACK;
    private double currentGoalLeftX = 0;
    private double currentGoalBottomY = 0;

    // bề rộng hiển thị thực tế của thủ môn sau khi scale
    private double keeperDisplayWidth = 0;

    // =========================
    //         Âm thanh
    // =========================
    private AudioClip siuuuuuu;
    private AudioClip mu;

    // =========================
    //       Thời gian, lượt
    // =========================
    private Timeline countdownTimeline;
    private int timeRemaining;
    private static final int TURN_TIMEOUT = 15;
    private int lastTurnDuration = 15;
    private String yourRole = "";
    private boolean isMyTurn = false;
    private String waitingForOpponentAction = "";
    private List<Rectangle> goalZones = new ArrayList<>();

    // =========================================================
    //                 CẬP NHẬT ĐIỂM LÊN UI
    // =========================================================
    public void updateScore(int[] scores) {
        // Debug: print received scores and which user this controller belongs to (if set)
        String userName = (client != null && client.getUser() != null) ? client.getUser().getUsername() : "(unknown)";
        System.out.println("[GameRoomController] user=" + userName + " updateScore received: " + java.util.Arrays.toString(scores));
        Platform.runLater(() -> {
            if (scoreLabel == null) {
                System.out.println("[GameRoomController] scoreLabel is null for user=" + userName);
                return;
            }
            int yourScore = scores[0];
            int opponentScore = scores[1];
            int currentRound = scores[2];
            scoreLabel.setText("Round: " + currentRound + "         Bạn: " + yourScore
                    + "   -   Đối thủ: " + opponentScore);
        });
    }

    public void setClient(Client client) { this.client = client; }

    // =========================================================
    //                   VÒNG ĐỜI MÀN GAME
    // =========================================================
    @FXML
    private void initialize() {
        shootButton.setDisable(false);
        goalkeeperButton.setDisable(false);
        if (timerLabel != null) timerLabel.setText("Thời gian còn lại: 0 giây");
        Platform.runLater(this::drawField);
        if (scoreLabel != null) scoreLabel.setText("Round: 1         Bạn: 0   -   Đối thủ: 0");
     // Sau khi mọi thứ khung thành, bóng, thủ môn được thêm
        createGoalZones();
        Platform.runLater(() -> createGoalZones());
    }

    // Vẽ bằng ẢNH: sân (sân.png) + khung thành (goal.png)
    private void drawField() {
        playBackgroundMusic();
        gamePane.getChildren().clear();

        double paneWidth = gamePane.getWidth();
        double paneHeight = gamePane.getHeight();
        if (paneWidth <= 0 || paneHeight <= 0) { paneWidth = 600; paneHeight = 400; }

        // ----- ẢNH NỀN SÂN -----
        Image fieldImg = new Image(getClass().getResource("/assets/sân.png").toExternalForm());
        fieldBgView = new ImageView(fieldImg);
        fieldBgView.setFitWidth(paneWidth);
        fieldBgView.setFitHeight(paneHeight);
        fieldBgView.setPreserveRatio(false);
        fieldBgView.setLayoutX(0);
        fieldBgView.setLayoutY(0);
        gamePane.getChildren().add(fieldBgView);

    // ----- KHUNG 6 Ô -----
    // Tăng kích thước, vẫn nằm cao
    double gridWidth = Math.min(paneWidth * 0.68, 320); // to hơn
    double gridHeight = 170; // cao hơn
    double gridStartX = (paneWidth - gridWidth) / 2.0; // left edge for both grid and goal
    double gridStartY = 30; // vẫn nằm cao

    // ----- ẢNH KHUNG THÀNH -----
    Image goalImg = new Image(getClass().getResource("/assets/goal.png").toExternalForm());
    goalView = new ImageView(goalImg);
    goalView.setFitWidth(gridWidth); // khung thành bằng khung 6 ô
    goalView.setPreserveRatio(true);
    goalView.setLayoutX(gridStartX); // left edge of goal matches grid
    goalView.setLayoutY(gridStartY - 90); // đẩy khung thành lên cao thêm 20px nữa
    gamePane.getChildren().add(goalView);

        // Tính toán biên khung dựa theo ảnh đã đặt
        currentGoalWidth = goalView.getBoundsInParent().getWidth();
        currentGoalLeftX = goalView.getLayoutX();
        currentGoalBottomY = goalView.getLayoutY() + goalView.getBoundsInParent().getHeight();

        // ----- CẦU THỦ SÚT -----
        player = createPlayer(paneWidth / 2, paneHeight - 50, "/assets/đá.png");
        gamePane.getChildren().add(player);

    // ----- THỦ MÔN: căn giữa khung thành mới -----
    double keeperY = currentGoalBottomY - 10; // thủ môn nằm sát mép dưới khung thành
    goalkeeper = createKeeper(currentGoalLeftX, currentGoalWidth, keeperY, "/assets/đỡ.png");
    gamePane.getChildren().add(goalkeeper);

        // ----- BÓNG -----
        ball = createBall(paneWidth / 2, paneHeight - 120, 10);
        gamePane.getChildren().add(ball);

        // ----- BANNER THẮNG/THUA -----
        Image image = new Image(getClass().getResource("/assets/c1cup.png").toExternalForm());
        ImageView imageView = new ImageView(image);
        imageView.setX(0); imageView.setY(20);
        imageView.setFitWidth(image.getWidth() / 4); imageView.setFitHeight(image.getHeight() / 4);
        Text winText = new Text("Bạn đã thắng!");
        winText.setFill(Color.YELLOW); winText.setFont(Font.font("Arial", FontWeight.BOLD, 24));
        winText.setX(imageView.getX() + 25); winText.setY(imageView.getY() + imageView.getFitHeight() + 30);
        Text winText2 = new Text("Glory Man United!");
        winText2.setFill(Color.YELLOW); winText2.setFont(Font.font("Arial", FontWeight.BOLD, 24));
        winText2.setX(imageView.getX() + 5); winText2.setY(imageView.getY() + imageView.getFitHeight() + 60);
        imageWinGroup = new Group(imageView, winText, winText2);
        gamePane.getChildren().add(imageWinGroup);
        enableWinGroup(false);

        Image imageLose = new Image(getClass().getResource("/assets/loa.png").toExternalForm());
        ImageView imageLoseView = new ImageView(imageLose);
        imageLoseView.setX(25); imageLoseView.setY(20);
        imageLoseView.setFitWidth(imageLose.getWidth() / 8); imageLoseView.setFitHeight(imageLose.getHeight() / 8);
        Text loseText = new Text("Bạn đã thua!");
        loseText.setFill(Color.YELLOW); loseText.setFont(Font.font("Arial", FontWeight.BOLD, 24));
        loseText.setX(imageLoseView.getX()); loseText.setY(imageLoseView.getY() + imageLoseView.getFitHeight() + 20);
        Text loseText2 = new Text("Tất cả vào hang!");
        loseText2.setFill(Color.YELLOW); loseText2.setFont(Font.font("Arial", FontWeight.BOLD, 24));
        loseText2.setX(imageLoseView.getX() - 20);
        loseText2.setY(imageLoseView.getY() + imageLoseView.getFitHeight() + 50);
        imageLoseGroup = new Group(imageLoseView, loseText, loseText2);
        gamePane.getChildren().add(imageLoseGroup);
        enableLoseGroup(false);
    }

    // =========================================================
    //                 TẠO CÁC THÀNH PHẦN ĐỒ HỌA
    // =========================================================
    private Group createPlayer(double x, double y, String imgPath) {
        Image img = new Image(getClass().getResourceAsStream(imgPath));
        ImageView iv = new ImageView(img);
        iv.setFitWidth(PLAYER_W + 10);
        iv.setFitHeight(PLAYER_H + 10);
        iv.setPreserveRatio(true);
        iv.setLayoutX(x - iv.getFitWidth() / 2);
        iv.setLayoutY(y - iv.getFitHeight());
        return new Group(iv);
    }

    // Thủ môn: căn giữa theo ảnh goal.png (goalLeftX + goalWidth)
    private Group createKeeper(double goalLeftX, double goalWidth, double footY, String direction) {
        String imgPath;
        if ("Left".equalsIgnoreCase(direction)) {
            imgPath = "/assets/đỡleft.png";
        } else if ("Right".equalsIgnoreCase(direction)) {
            imgPath = "/assets/đỡright.png";
        } else {
            imgPath = "/assets/đỡ.png";
        }
        Image img = new Image(getClass().getResourceAsStream(imgPath));
        ImageView iv = new ImageView(img);

        double aspect = img.getWidth() / img.getHeight();
        double dispH = KEEPER_H;
        double dispW = dispH * aspect;

        iv.setFitHeight(dispH);
        iv.setFitWidth(dispW);
        iv.setPreserveRatio(false);

        iv.setLayoutX(goalLeftX + (goalWidth - dispW) / 2.0); // đúng giữa khung
        iv.setLayoutY(footY - dispH);                          // chân đúng đáy lưới

        keeperDisplayWidth = dispW;
        return new Group(iv);
    }

    private Group createBall(double x, double y, double radius) {
        Circle circle = new Circle(x, y, radius);
        circle.setFill(Color.WHITE);
        circle.setStroke(Color.BLACK);
        ballCircle = circle;

        Polygon pentagon = new Polygon();
        double angle = -Math.PI / 2;
        double angleIncrement = 2 * Math.PI / 5;
        for (int i = 0; i < 5; i++) {
            pentagon.getPoints().addAll(
                    x + radius * 0.6 * Math.cos(angle),
                    y + radius * 0.6 * Math.sin(angle));
            angle += angleIncrement;
        }
        pentagon.setFill(Color.BLACK);
        return new Group(circle, pentagon);
    }

    private SequentialTransition buildKickerMove() {
        TranslateTransition stepIn = new TranslateTransition(KICK_IN_TIME, player);
        stepIn.setByY(-KICK_STEP_Y);
        TranslateTransition stepBack = new TranslateTransition(KICK_BACK_TIME, player);
        stepBack.setByY(+KICK_STEP_Y);
        return new SequentialTransition(stepIn, stepBack);
    }

    // =========================================================
    //                CHAT/GUI HÀNH ĐỘNG NGƯỜI CHƠI
    // =========================================================
    @FXML
    private void handleSendChat() throws IOException {
        String message = chatInput.getText();
        if (!message.isEmpty()) {
            Message chatMessage = new Message("chat", message);
            client.sendMessage(chatMessage);
            chatInput.clear();
        }
    }

    @FXML
    private void handleShoot() {
        ChoiceDialog<String> d = new ChoiceDialog<>("Middle", "Left", "Middle", "Right");
        d.setTitle("Chọn Hướng Sút");
        d.setHeaderText("Chọn hướng sút:");
        d.setContentText("Hướng:");

        Optional<String> result = d.showAndWait();
        result.ifPresent(direction -> {
            if (timeRemaining < 0) return;
            try {
                // Ensure we send normalized direction tokens
                String dir = normalizeDirection(direction);
                client.sendMessage(new Message("shoot", dir));
                shootButton.setDisable(true);
            } catch (IOException ex) {
                Logger.getLogger(GameRoomController.class.getName()).log(Level.SEVERE, null, ex);
            }
        });
        if (countdownTimeline != null) countdownTimeline.stop();
    }

    @FXML
    private void handleGoalkeeper() {
        ChoiceDialog<String> d = new ChoiceDialog<>("Middle", "Left", "Middle", "Right");
        d.setTitle("Chọn Hướng Chặn");
        d.setHeaderText("Chọn hướng chặn:");
        d.setContentText("Hướng:");

        Optional<String> result = d.showAndWait();
        result.ifPresent(direction -> {
            if (timeRemaining < 0) return;
            try {
                String dir = normalizeDirection(direction);
                client.sendMessage(new Message("goalkeeper", dir));
                goalkeeperButton.setDisable(true);
            } catch (IOException ex) {
                Logger.getLogger(GameRoomController.class.getName()).log(Level.SEVERE, null, ex);
            }
        });
        if (countdownTimeline != null) countdownTimeline.stop();
    }

    // Ensure dialog strings are normalized to Left/Middle/Right
    private String normalizeDirection(String dir) {
        if (dir == null) return "Middle";
        dir = dir.trim().toLowerCase();
        if (dir.contains("left")) return "Left";
        if (dir.contains("right")) return "Right";
        return "Middle";
    }

    public void updateChat(String message) {
        Platform.runLater(() -> chatArea.appendText(message + "\n"));
    }

    // =========================================================
    //                    ANIMATION KẾT QUẢ LƯỢT
    // =========================================================
    public void animateShootVao(String directShoot, String directKeeper) {
        siuuuuuu.play();
        Platform.runLater(() -> {
            // Xóa thủ môn cũ và tạo lại thủ môn mới với ảnh đúng hướng
            gamePane.getChildren().remove(goalkeeper);
            goalkeeper = createKeeper(currentGoalLeftX, currentGoalWidth, currentGoalBottomY-50, directKeeper);
            gamePane.getChildren().add(goalkeeper);

            Path path = new Path();
            path.getElements().add(new MoveTo(ballCircle.getCenterX(), ballCircle.getCenterY()));
            double targetX = ballCircle.getCenterX();
            double targetY = ballCircle.getCenterY() - 210;
            if (directShoot.equalsIgnoreCase("Left"))  targetX -= 90;
            else if (directShoot.equalsIgnoreCase("Right")) targetX += 90;
            path.getElements().add(new LineTo(targetX, targetY));

            PathTransition ballFly = new PathTransition(Duration.seconds(1), path, ball);

            double half = currentGoalWidth / 2.0;
            double dx = half - (keeperDisplayWidth / 2.0) - 5;
            double targetKeeperX = 0;
            if (directKeeper.equalsIgnoreCase("Left"))  targetKeeperX = -dx;
            else if (directKeeper.equalsIgnoreCase("Right")) targetKeeperX =  dx;
            TranslateTransition keeperMove = new TranslateTransition(Duration.seconds(1), goalkeeper);
            keeperMove.setByX(targetKeeperX);

            SequentialTransition kickerMove = buildKickerMove();

            ParallelTransition all = new ParallelTransition(ballFly, keeperMove, kickerMove);
            all.setOnFinished(ev -> {
                ball.setTranslateX(0); ball.setTranslateY(0);
                goalkeeper.setTranslateX(0); goalkeeper.setTranslateY(0);
                resetKeeperToDefault(); // Reset thủ môn về trạng thái mặc định
            });
            all.play();
        });
    }

    public void animateShootKhongVao(String directShoot, String directKeeper) {
        Platform.runLater(() -> {
            // Xóa thủ môn cũ và tạo lại thủ môn mới với ảnh đúng hướng
            gamePane.getChildren().remove(goalkeeper);
            goalkeeper = createKeeper(currentGoalLeftX, currentGoalWidth, currentGoalBottomY-50, directKeeper);
            gamePane.getChildren().add(goalkeeper);

            Path path = new Path();
            path.getElements().add(new MoveTo(ballCircle.getCenterX(), ballCircle.getCenterY()));
            double targetX = ballCircle.getCenterX();
            double targetY = ballCircle.getCenterY() - 210;
            if (directShoot.equalsIgnoreCase("Left"))  targetX -= 90;
            else if (directShoot.equalsIgnoreCase("Right")) targetX += 90;
            path.getElements().add(new LineTo(targetX, targetY));

            double targetPathOutX = targetX, targetPathOutY = targetY - 25;
            if (directKeeper.equalsIgnoreCase("Left"))      targetPathOutX -= 40;
            else if (directKeeper.equalsIgnoreCase("Right")) targetPathOutX += 40;
            else                                            targetPathOutY -= 40;

            Path pathOut = new Path();
            pathOut.getElements().add(new MoveTo(targetX, targetY));
            pathOut.getElements().add(new LineTo(targetPathOutX, targetPathOutY));

            PathTransition toGoal = new PathTransition(Duration.seconds(0.9), path, ball);
            PathTransition bounceOut = new PathTransition(Duration.seconds(0.3), pathOut, ball);

            double half = currentGoalWidth / 2.0;
            double dx = half - (keeperDisplayWidth / 2.0) - 5;
            double targetKeeperX = 0;
            if (directKeeper.equalsIgnoreCase("Left"))  targetKeeperX = -dx;
            else if (directKeeper.equalsIgnoreCase("Right")) targetKeeperX =  dx;

            TranslateTransition keeperMove = new TranslateTransition(Duration.seconds(1), goalkeeper);
            keeperMove.setByX(targetKeeperX);
            keeperMove.setAutoReverse(false);

            SequentialTransition kickerMove = buildKickerMove();

            PauseTransition pause = new PauseTransition(Duration.seconds(2));
            SequentialTransition ballAnim = directShoot.equalsIgnoreCase(directKeeper)
                    ? new SequentialTransition(toGoal, bounceOut, pause)
                    : new SequentialTransition(toGoal, pause);

            ParallelTransition gameAnimation = new ParallelTransition(ballAnim, keeperMove, kickerMove);
            gameAnimation.setOnFinished(event -> {
                ball.setTranslateX(0); ball.setTranslateY(0);
                goalkeeper.setTranslateX(0); goalkeeper.setTranslateY(0);
                resetKeeperToDefault(); // Reset thủ môn về trạng thái mặc định
            });
            gameAnimation.play();
        });
    }

    // =========================================================
    //                 ĐIỀU KHIỂN LƯỢT/THỜI GIAN
    // =========================================================
    public void promptYourTurn(int durationInSeconds) {
        Platform.runLater(() -> {
            lastTurnDuration = durationInSeconds;
            isMyTurn = true;
            yourRole = "Shooter";
            shootButton.setDisable(false);
            goalkeeperButton.setDisable(true);
            startCountdown(durationInSeconds);
        });
    }

    public void promptGoalkeeperTurn(int durationInSeconds) {
        Platform.runLater(() -> {
            lastTurnDuration = durationInSeconds;
            isMyTurn = true;
            yourRole = "Goalkeeper";
            goalkeeperButton.setDisable(false);
            shootButton.setDisable(true);
            startCountdown(durationInSeconds);
        });
    }

    public void handleOpponentTurn(int durationInSeconds) {
        Platform.runLater(() -> {
            isMyTurn = false;
            shootButton.setDisable(true);
            goalkeeperButton.setDisable(true);
            waitingForOpponentAction = yourRole.equals("Shooter") ? "goalkeeper"
                    : yourRole.equals("Goalkeeper") ? "shoot" : "";
            startCountdown(durationInSeconds);
        });
    }

    // =========================================================
    //                 THÔNG BÁO KẾT THÚC/CUỘC
    // =========================================================
    public void showRoundResult(String roundResult) {
        siuuuuuu.play();
        Platform.runLater(() -> {
            Alert alert = new Alert(AlertType.INFORMATION);
            alert.setTitle("Kết Quả Lượt");
            alert.setHeaderText(null);
            alert.setContentText(roundResult);
            alert.showAndWait();
        });
    }

    public void endMatch(String result) {
        // Dừng countdown ngay lập tức
        if (countdownTimeline != null) {
            countdownTimeline.stop();
        }
        
        // Disable tất cả nút
        shootButton.setDisable(true);
        goalkeeperButton.setDisable(true);
        quitButton.setDisable(true);
        isMyTurn = false;
        
        if (mu != null) mu.stop();
        Platform.runLater(() -> {
            Alert alert = new Alert(AlertType.INFORMATION);
            alert.setTitle("Kết Thúc Trận Đấu");
            alert.setHeaderText(null);
            alert.setContentText(result);
            alert.show();
            PauseTransition delay = new PauseTransition(Duration.seconds(2));
            delay.setOnFinished(event -> {
                try { client.showMainUI(); } catch (Exception e) { e.printStackTrace(); }
            });
            delay.play();
        });
    }

    public void handleRematchDeclined(String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(AlertType.INFORMATION);
            alert.setTitle("Chơi Lại");
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.show();
            PauseTransition delay = new PauseTransition(Duration.seconds(2));
            delay.setOnFinished(event -> {
                try { client.showMainUI(); } catch (Exception e) { e.printStackTrace(); }
            });
            delay.play();
        });
    }

    public void promptPlayAgain() {
        Platform.runLater(() -> {
            Alert alert = new Alert(AlertType.CONFIRMATION);
            alert.setTitle("Chơi Lại");
            alert.setHeaderText(null);
            alert.setContentText("Bạn có muốn chơi lại không?");
            ButtonType yesButton = new ButtonType("Có", ButtonBar.ButtonData.YES);
            ButtonType noButton  = new ButtonType("Không", ButtonBar.ButtonData.NO);
            alert.getButtonTypes().setAll(yesButton, noButton);

            Optional<ButtonType> result = alert.showAndWait();
            if (result.isPresent()) {
                boolean playAgain = result.get() == yesButton;
                try { client.sendMessage(new Message("play_again_response", playAgain)); } catch (IOException e) { e.printStackTrace(); }
                if (!playAgain) {
                    try { client.showMainUI(); } catch (Exception e) { e.printStackTrace(); }
                }
            }
        });
    }

    @FXML
    private void handleQuitGame() {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Thoát Trò Chơi");
            alert.setHeaderText(null);
            alert.setContentText("Bạn có chắc chắn muốn thoát trò chơi không?");
            ButtonType yesButton = new ButtonType("Có", ButtonBar.ButtonData.YES);
            ButtonType noButton  = new ButtonType("Không", ButtonBar.ButtonData.NO);
            alert.getButtonTypes().setAll(yesButton, noButton);

            Optional<ButtonType> result = alert.showAndWait();
            if (result.isPresent() && result.get() == yesButton) {
                // Dừng countdown timer ngay lập tức
                if (countdownTimeline != null) {
                    countdownTimeline.stop();
                }
                
                // Disable tất cả nút
                shootButton.setDisable(true);
                goalkeeperButton.setDisable(true);
                quitButton.setDisable(true);
                
                // Đặt cờ để không nhận message nữa
                isMyTurn = false;
                
                Message quitMessage = new Message("quit_game", null);
                try {
                    // send quit to server and WAIT for server's match_end message
                    client.sendMessage(quitMessage);
                    // Do NOT call client.showMainUI() here. endMatch() will be triggered by server's "match_end" message
                } catch (IOException e) { e.printStackTrace(); }
            }
        });
    }

    @FXML
    private void handleReturnToMain() {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Quay về Màn hình chính");
            alert.setHeaderText(null);
            alert.setContentText("Bạn có chắc chắn muốn quay về màn hình chính? Trận đấu sẽ kết thúc và bạn sẽ thua.");
            ButtonType yesButton = new ButtonType("Có", ButtonBar.ButtonData.YES);
            ButtonType noButton  = new ButtonType("Không", ButtonBar.ButtonData.NO);
            alert.getButtonTypes().setAll(yesButton, noButton);

            Optional<ButtonType> result = alert.showAndWait();
            if (result.isPresent() && result.get() == yesButton) {
                // Dừng countdown timer ngay lập tức
                if (countdownTimeline != null) {
                    countdownTimeline.stop();
                }
                
                // Disable tất cả nút
                shootButton.setDisable(true);
                goalkeeperButton.setDisable(true);
                quitButton.setDisable(true);
                
                // Đặt cờ để không nhận message nữa
                isMyTurn = false;
                
                Message quitMessage = new Message("quit_game", null);
                try {
                    // send quit to server and WAIT for server's match_end message
                    client.sendMessage(quitMessage);
                    // Do NOT call client.showMainUI() here. endMatch() will be triggered by server's "match_end" message
                } catch (IOException e) { e.printStackTrace(); }
            }
        });
    }

    public void showStartMessage(String message) {
        Platform.runLater(() -> {
            // Only show role; real turn prompts come from server via your_turn/goalkeeper_turn
            if (message.contains("người sút")) {
                yourRole = "Shooter";
            } else if (message.contains("người bắt")) {
                yourRole = "Goalkeeper";
            }
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Vai trò");
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.show();
        });
    }

    public void showMatchResult(String result) {
        Platform.runLater(() -> {
            if (result.equals("win")) { enableWinGroup(true); enableLoseGroup(false); }
            else if (result.equals("lose")) { enableLoseGroup(true); enableWinGroup(false); }
            if (countdownTimeline != null) countdownTimeline.stop();
            timerLabel.setText("Kết thúc trận đấu!");
        });
    }

    public void handleTimeout(String message) {
        Platform.runLater(() -> {
            isMyTurn = false;
            Alert alert = new Alert(AlertType.WARNING);
            alert.setTitle("Hết giờ");
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.show();
            // Round sẽ được server kết thúc ngay sau khi timeout, không khởi động countdown mới
            shootButton.setDisable(true);
            goalkeeperButton.setDisable(true);
            timerLabel.setText("Hết giờ - đang xử lý kết quả...");
        });
    }

    public void handleOpponentTimeout(String message) {
        Platform.runLater(() -> {
            if (countdownTimeline != null) countdownTimeline.stop();
            // Đối thủ hết giờ: server sẽ tự kết thúc round và gửi kết quả, không bật nút hay countdown ở client
            isMyTurn = false;
            shootButton.setDisable(true);
            goalkeeperButton.setDisable(true);
            timerLabel.setText("Đối thủ hết giờ - đang xử lý kết quả...");
        });
    }
    
    public void handleRoleChange(String message) {
        Platform.runLater(() -> {
            // Cập nhật vai trò mới dựa vào thông báo từ server
            if (message.contains("người sút")) {
                yourRole = "Shooter";
            } else if (message.contains("người bắt")) {
                yourRole = "Goalkeeper";
            }
            
            // Hiển thị thông báo đổi vai trò
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Đổi vai trò");
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.show();
            
            // Reset trạng thái nút
            shootButton.setDisable(true);
            goalkeeperButton.setDisable(true);
            isMyTurn = false;
        });
    }

    // =========================================================
    //               ÂM THANH + ĐỒNG HỒ ĐẾM NGƯỢC
    // =========================================================
    private void playBackgroundMusic() {
        siuuuuuu = new AudioClip(getClass().getResource("/sound/siuuu.wav").toExternalForm());
        mu = new AudioClip(getClass().getResource("/sound/mu.wav").toExternalForm());
        mu.setCycleCount(AudioClip.INDEFINITE);
        mu.setVolume(0.15f);
        mu.play();
    }

    private void startCountdown(int durationInSeconds) {
        timeRemaining = durationInSeconds;
        if (countdownTimeline != null) countdownTimeline.stop();

        countdownTimeline = new Timeline(new KeyFrame(Duration.seconds(1), event -> {
            final String action = isMyTurn ? "Thời gian còn lại: " : "Đang chờ đối thủ: ";
            timerLabel.setText(action + timeRemaining + " giây");
            timeRemaining--;

            if (timeRemaining < 0) {
                countdownTimeline.stop();
                dialog.close();
                timerLabel.setText(action + "0 giây");
                shootButton.setDisable(true);
                goalkeeperButton.setDisable(true);
                try {
                    if (yourRole.equals("Shooter")) client.sendMessage(new Message("timeout", "shooter"));
                    else if (yourRole.equals("Goalkeeper")) client.sendMessage(new Message("timeout", "goalkeeper"));
                } catch (IOException ex) {
                    Logger.getLogger(GameRoomController.class.getName()).log(Level.SEVERE, null, ex);
                }
                isMyTurn = false;
            }
        }));
        countdownTimeline.setCycleCount(durationInSeconds + 1);
        countdownTimeline.play();

        final String action = isMyTurn ? "Thời gian còn lại: " : "Đang chờ đối thủ: ";
        timerLabel.setText(action + timeRemaining + " giây");
    }

    // =========================================================
    //                BANNER THẮNG/THUA HELPERS
    // =========================================================
    private void enableWinGroup(boolean enable) { imageWinGroup.setVisible(enable); }
    private void enableLoseGroup(boolean enable) { imageLoseGroup.setVisible(enable); }

    // Reset thủ môn về trạng thái mặc định (không nghiêng)
    private void resetKeeperToDefault() {
        gamePane.getChildren().remove(goalkeeper);
        goalkeeper = createKeeper(currentGoalLeftX, currentGoalWidth, currentGoalBottomY-50, "Middle");
        gamePane.getChildren().add(goalkeeper);
    }

    // Public API to stop any playing sounds from outside (e.g., when an invite fails)
    public void stopAllSounds() {
        try {
            if (mu != null) {
                mu.stop();
            }
            if (siuuuuuu != null) {
                siuuuuuu.stop();
            }
        } catch (Exception ex) {
            // Swallow exceptions to avoid crashing caller; log to console
            System.err.println("Error stopping sounds: " + ex.getMessage());
        }
    }
    
    private void createGoalZones() {
        // Xóa các ô cũ nếu có
        gamePane.getChildren().removeIf(node -> {
            return node instanceof Rectangle && node.getId() != null && node.getId().startsWith("goalZone");
        });

        double paneW = gamePane.getWidth();
        double paneH = gamePane.getHeight();
        // Khung 6 ô trùng khít ảnh khung thành nếu có; fallback nếu goalView chưa sẵn sàng
        double gridWidth;
        double gridHeight;
        double gridStartX;
        double gridStartY;

        if (goalView != null) {
            gridWidth = goalView.getBoundsInParent().getWidth();
            gridHeight = goalView.getBoundsInParent().getHeight();
            gridStartX = goalView.getLayoutX();
            gridStartY = goalView.getLayoutY();
        } else {
            gridWidth = Math.min(paneW * 0.68, 320);
            gridHeight = 170;
            gridStartX = (paneW - gridWidth) / 2.0;
            gridStartY = 30;
        }

        // Thu nhỏ chiều cao khung 6 ô và canh giữa theo chiều dọc trong ảnh khung thành
        double heightScale = 0.65; // 65% chiều cao gốc
        double scaledGridHeight = gridHeight * heightScale;
        double verticalOffset = (gridHeight - scaledGridHeight) / 2.0;
        gridStartY += verticalOffset; // đẩy xuống để giữ tâm dọc như cũ
        gridHeight = scaledGridHeight;

        int rows = 2;
        int cols = 3;
        double cellW = gridWidth / cols;
        double cellH = gridHeight / rows;

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                Rectangle rect = new Rectangle(gridStartX + c * cellW, gridStartY + r * cellH, cellW, cellH); // left edge of grid
                rect.setId("goalZone_" + r + "_" + c);
                rect.setFill(Color.rgb(255, 255, 255, 0.1));
                rect.setStroke(Color.WHITE);
                rect.setStrokeWidth(1);

                // Hover effect
                rect.setOnMouseEntered(e -> rect.setFill(Color.rgb(0, 255, 0, 0.3)));
                rect.setOnMouseExited(e -> rect.setFill(Color.rgb(255, 255, 255, 0.1)));

                final int rr = r;
                final int cc = c;
                rect.setOnMouseClicked(e -> handleZoneClick(rr, cc));

                goalZones.add(rect);
                gamePane.getChildren().add(rect);
            }
        }
    }

    private void handleZoneClick(int row, int col) {
        if (!isMyTurn) {
            return;
        }
        // Map clicked zone to server-expected directions: Left / Middle / Right
        String area = convertZoneToDirection(row, col);
        try {
            if (yourRole.equals("Shooter")) {
                client.sendMessage(new Message("shoot", area));
            } else if (yourRole.equals("Goalkeeper")) {
                client.sendMessage(new Message("goalkeeper", area));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        isMyTurn = false;
    }

    private String convertZoneToDirection(int row, int col) {
        // Normalize mapping to the server's expected simple directions
         // Use column only: 0 -> Left, 1 -> Middle, 2 -> Right
        if (col == 0) return "Left";
        if (col == 1) return "Middle";
        if (col == 2) return "Right";
        return "Middle";
    }

}
