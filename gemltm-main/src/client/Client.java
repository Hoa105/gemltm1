package client;

import client.GUI.GameRoomController;
import client.GUI.LoginController;
import client.GUI.MainController;
import common.Match;
import common.MatchDetails;
import common.Message;
import common.User;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.control.Alert;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.URL;
import java.util.List;
import java.util.Arrays;

public class Client {

    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private User user;
    private Stage primaryStage;

    // Controllers
    private LoginController loginController;
    private MainController mainController;
    private GameRoomController gameRoomController;

    private volatile boolean isRunning = true;

    public Client(Stage primaryStage) {
        this.primaryStage = primaryStage;
    }

    public void showErrorAlert(String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Lỗi");
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

    public void startConnection(String address, int port) {
        try {
            socket = new Socket(address, port);
            // Important: create ObjectOutputStream first, then ObjectInputStream.
            // If both sides create ObjectInputStream first, they can deadlock.
            out = new ObjectOutputStream(socket.getOutputStream());
            out.flush();
            in = new ObjectInputStream(socket.getInputStream());
            isRunning = true; // Đặt lại isRunning thành true
            listenForMessages();
        } catch (IOException e) {
            e.printStackTrace();
            showErrorAlert("Không thể kết nối tới server.");
        }
    }

    private void listenForMessages() {
        Thread listener = new Thread(() -> {
            try {
                while (isRunning) {
                    Message message = (Message) in.readObject();
                    if (message == null) {
                        System.out.println("Received null message, ignoring.");
                        continue;
                    }
                    handleMessage(message);
                }
            } catch (IOException | ClassNotFoundException ex) {
                if (isRunning) {
                    ex.printStackTrace();
                    try {
                        closeConnection(); // Đóng kết nối hiện tại
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    Platform.runLater(() -> {
                        showErrorAlert("Kết nối tới server bị mất.");
                        try {
                            showLoginUI(); // Hiển thị giao diện đăng nhập và tái kết nối
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    });
                } else {
                    System.out.println("Đã đóng kết nối, dừng luồng lắng nghe.");
                }
            }
        });
        listener.setDaemon(true); // không chặn JVM thoát
        listener.start();
    }

    // Tiện ích chuyển đổi an toàn sang int[]
    private int[] toIntArray(Object o) {
        if (o == null) return null;
        if (o instanceof int[]) return (int[]) o;
        if (o instanceof Integer[]) {
            Integer[] arr = (Integer[]) o;
            int[] res = new int[arr.length];
            for (int i = 0; i < arr.length; i++) res[i] = arr[i] == null ? 0 : arr[i];
            return res;
        }
        if (o instanceof List) {
            List<?> list = (List<?>) o;
            int[] res = new int[list.size()];
            for (int i = 0; i < list.size(); i++) {
                Object it = list.get(i);
                if (it instanceof Number) res[i] = ((Number) it).intValue();
                else {
                    System.out.println("Warning: non-number in scores list: " + it);
                    res[i] = 0;
                }
            }
            return res;
        }
        System.out.println("Warning: cannot convert to int[]: " + o);
        return null;
    }

    private void handleMessage(Message message) {
        if (message == null) {
            System.out.println("handleMessage called with null message, ignoring.");
            return;
        }
        System.out.println("Received message: " + message.getType() + " - " + message.getContent());
        switch (message.getType()) {
            case "login_success":
                this.user = (User) message.getContent();
                Platform.runLater(() -> showMainUI());
                break;
            case "login_failure":
                Platform.runLater(() -> {
                    if (loginController != null) {
                        loginController.showError((String) message.getContent());
                    }
                });
                break;
            case "user_list":
                List<User> users = (List<User>) message.getContent();
                Platform.runLater(() -> {
                    if (mainController != null) {
                        mainController.updateUsersList(users);
                    }
                });
                break;
            case "status_update":
                Platform.runLater(() -> {
                    if (mainController != null) {
                        mainController.updateStatus((String) message.getContent());
                    }
                });
                break;
            case "match_request":
                final Object matchReqContent = message.getContent();
                Platform.runLater(() -> {
                    if (mainController != null) {
                        if (matchReqContent instanceof Integer) {
                            mainController.showMatchRequest((Integer) matchReqContent);
                        } else {
                            System.out.println("Warning: match_request content not Integer: " + matchReqContent);
                        }
                    }
                });
                break;
            case "match_response":
                Platform.runLater(() -> {
                    if (mainController != null) {
                        mainController.handleMatchResponse((String) message.getContent());
                    }
                });
                break;
            case "chat":
                Platform.runLater(() -> {
                    if (gameRoomController != null) {
                        gameRoomController.updateChat((String) message.getContent());
                    }
                });
                break;
            case "match_start":
                final Object matchStartContent = message.getContent();
                Platform.runLater(() -> {
                    if (matchStartContent instanceof String) showGameRoomUI((String) matchStartContent);
                    else System.out.println("Warning: match_start content not String: " + matchStartContent);
                });
                break;
            case "kick_result":
                Platform.runLater(() -> {
                    if (gameRoomController != null) {
                        final Object payloadObj = message.getContent();
                        if (!(payloadObj instanceof String)) {
                            System.out.println("Warning: kick_result payload is not String: " + payloadObj);
                            return;
                        }
                        String payload = (String) payloadObj;
                        String[] result = payload.split("-");
                        System.out.println("Received kick_result payload: " + payload + " -> parsed: " + Arrays.toString(result));
                        if (result.length >= 3 && result[0].equals("win")) {
                            gameRoomController.animateShootVao(result[1], result[2]);
                        } else if (result.length >= 3) {
                            gameRoomController.animateShootKhongVao(result[1], result[2]);
                        } else {
                            System.out.println("Warning: unexpected kick_result format: " + payload);
                        }
                    }
                });
                break;
            case "round_result":
                Platform.runLater(() -> {
                    if (gameRoomController != null) {
                        gameRoomController.showRoundResult((String) message.getContent());
                    }
                });
                break;
            case "match_end":
                Platform.runLater(() -> {
                    if (gameRoomController != null) {
                        gameRoomController.endMatch((String) message.getContent());
                    }
                });
                break;
            case "play_again_request":
                Platform.runLater(() -> {
                    if (gameRoomController != null) {
                        gameRoomController.promptPlayAgain();
                    }
                });
                break;
            case "rematch_declined":
                Platform.runLater(() -> {
                    if (gameRoomController != null) {
                        gameRoomController.handleRematchDeclined((String) message.getContent());
                    }
                });
                break;
            case "leaderboard":
                List<User> leaderboard = (List<User>) message.getContent();
                Platform.runLater(() -> {
                    if (mainController != null) {
                        mainController.updateLeaderboard(leaderboard);
                    }
                });
                break;

            case "match_history":
                List<MatchDetails> history = (List<MatchDetails>) message.getContent();
                Platform.runLater(() -> {
                    if (mainController != null) {
                        mainController.updateMatchHistory(history);
                    }
                });
                break;

            case "user_matches":
                List<Match> matches = (List<Match>) message.getContent();
                Platform.runLater(() -> {
                    if (mainController != null) {
                        mainController.updateMatchesList(matches);
                    }
                });
                break;
            case "match_details":
                List<MatchDetails> details = (List<MatchDetails>) message.getContent();
                Platform.runLater(() -> {
                    if (mainController != null) {
                        mainController.showMatchDetails(details);
                    }
                });
                break;

            case "update_score":
                final Object scoresObj = message.getContent();
                int[] scores = toIntArray(scoresObj);
                System.out.println("Client user=" + (user != null ? user.getUsername() : "(not-set)")
                        + " Received update_score array: " + Arrays.toString(scores));
                Platform.runLater(() -> {
                    if (gameRoomController != null) {
                        gameRoomController.updateScore(scores);
                    }
                });
                break;
            case "return_to_main":
                Platform.runLater(() -> {
                    try {
                        showMainUI();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
                break;
            case "match_result":
                Platform.runLater(() -> {
                    if (gameRoomController != null) {
                        gameRoomController.showMatchResult((String) message.getContent());
                    }
                });
                break;

            case "your_turn":
                final Object yourTurnObj = message.getContent();
                if (yourTurnObj instanceof Integer) {
                    int duration = (Integer) yourTurnObj;
                    Platform.runLater(() -> {
                        if (gameRoomController != null) {
                            gameRoomController.promptYourTurn(duration);
                        }
                    });
                } else {
                    System.out.println("Warning: your_turn content not Integer: " + yourTurnObj);
                }
                break;
            case "goalkeeper_turn":
                final Object gkTurnObj = message.getContent();
                if (gkTurnObj instanceof Integer) {
                    int duration1 = (Integer) gkTurnObj;
                    Platform.runLater(() -> {
                        if (gameRoomController != null) {
                            gameRoomController.promptGoalkeeperTurn(duration1);
                        }
                    });
                } else {
                    System.out.println("Warning: goalkeeper_turn content not Integer: " + gkTurnObj);
                }
                break;

            case "opponent_turn":
                final Object oppTurnObj = message.getContent();
                if (oppTurnObj instanceof Integer) {
                    int duration2 = (Integer) oppTurnObj;
                    Platform.runLater(() -> {
                        if (gameRoomController != null) {
                            gameRoomController.handleOpponentTurn(duration2);
                        }
                    });
                } else {
                    System.out.println("Warning: opponent_turn content not Integer: " + oppTurnObj);
                }
                break;
                
            case "timeout":
                Platform.runLater(() -> {
                    if (gameRoomController != null) {
                        gameRoomController.handleTimeout((String) message.getContent());
                    }
                });
                break;
            case "opponent_timeout":
                Platform.runLater(() -> {
                    if (gameRoomController != null) {
                        gameRoomController.handleOpponentTimeout((String) message.getContent());
                    }
                });
                break;
            
            case "role_change":
                Platform.runLater(() -> {
                    if (gameRoomController != null) {
                        gameRoomController.handleRoleChange((String) message.getContent());
                    }
                });
                break;



            // Các loại message khác
            // ...
        }
    }

    public void sendMessage(Message message) throws IOException {
        
        out.writeObject(message);
        out.flush();
    }

    public void showMainUI() {
        try {
            // Clear game room controller để không nhận message game nữa
            gameRoomController = null;
            
            System.out.println("Loading MainUI.fxml...");
            FXMLLoader loader = new FXMLLoader(MainController.class.getResource("/resources/GUI/MainUI.fxml"));
            Parent root = loader.load();
            mainController = loader.getController();

            if (mainController == null) {
                System.err.println("Controller is null for MainUI.fxml");
                showErrorAlert("Không thể tải controller giao diện chính.");
                return;
            }

            mainController.setClient(this);
            Scene scene = new Scene(root);

            URL cssLocation = MainController.class.getResource("/resources/GUI/style.css");
            if (cssLocation != null) {
                scene.getStylesheets().add(cssLocation.toExternalForm());
                System.out.println("CSS file loaded: " + cssLocation.toExternalForm());
            } else {
                System.err.println("Cannot find CSS file: style.css");
            }

            primaryStage.setScene(scene);
            primaryStage.setTitle("Penalty Shootout - Main");
            primaryStage.setMinWidth(400);
            primaryStage.setMinHeight(300);
            primaryStage.show();
            sendMessage(new Message("get_users", null));
        } catch (IOException e) {
            e.printStackTrace();
            showErrorAlert("Không thể tải giao diện chính.");
        }
    }

    public void showLoginUI() {
        try {
            System.out.println("Loading LoginUI.fxml...");
            FXMLLoader loader = new FXMLLoader(LoginController.class.getResource("/resources/GUI/LoginUI.fxml"));
            Parent root = loader.load();
            loginController = loader.getController();

            if (loginController == null) {
                System.err.println("Controller is null for LoginUI.fxml");
                showErrorAlert("Không thể tải controller giao diện đăng nhập.");
                return;
            }

            loginController.setClient(this);
            Scene scene = new Scene(root);

            URL cssLocation = LoginController.class.getResource("/resources/GUI/style.css");
            if (cssLocation != null) {
                scene.getStylesheets().add(cssLocation.toExternalForm());
                System.out.println("CSS file loaded: " + cssLocation.toExternalForm());
            } else {
                System.err.println("Cannot find CSS file: style.css");
            }

            primaryStage.setScene(scene);
            primaryStage.setTitle("Penalty Shootout - Login");
            primaryStage.show();

            // Loại bỏ đoạn mã khởi tạo kết nối ở đây

        } catch (IOException e) {
            e.printStackTrace();
            showErrorAlert("Không thể tải giao diện đăng nhập.");
        }
    }

    public void showGameRoomUI(String startMessage) {
        try {
            System.out.println("Loading GameRoomUI.fxml...");
            FXMLLoader loader = new FXMLLoader(GameRoomController.class.getResource("/resources/GUI/GameRoomUI.fxml"));
            Parent root = loader.load();
            gameRoomController = loader.getController();

            if (gameRoomController == null) {
                System.err.println("Controller is null for GameRoomUI.fxml");
                showErrorAlert("Không thể tải controller giao diện phòng chơi.");
                return;
            }

            gameRoomController.setClient(this);
            Scene scene = new Scene(root);

            URL cssLocation = GameRoomController.class.getResource("/resources/GUI/style.css");
            if (cssLocation != null) {
                scene.getStylesheets().add(cssLocation.toExternalForm());
                System.out.println("CSS file loaded: " + cssLocation.toExternalForm());
            } else {
                System.err.println("Cannot find CSS file: style.css");
            }

            primaryStage.setScene(scene);
            primaryStage.setTitle("Penalty Shootout - Game Room");
            primaryStage.show();

            // Hiển thị thông báo vai trò
            gameRoomController.showStartMessage(startMessage);

        } catch (IOException e) {
            e.printStackTrace();
            showErrorAlert("Không thể tải giao diện phòng chơi.");
        }
    }

    public User getUser() {
        return user;
    }

    public void closeConnection() throws IOException {
        isRunning = false; // Dừng luồng lắng nghe
        if (in != null) {
            in.close();
        }
        if (out != null) {
            out.close();
        }
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
    }

    public static void main(String[] args) {
        javafx.application.Application.launch(ClientApp.class, args);
    }
}
