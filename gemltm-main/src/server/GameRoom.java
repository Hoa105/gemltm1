package server;

import common.Message;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class GameRoom {

    private ClientHandler shooterHandler;
    private ClientHandler goalkeeperHandler;
    private DatabaseManager dbManager;
    private int matchId;
    private int shooterScore;
    private int goalkeeperScore;
    private int currentRound;
    private final int MAX_ROUNDS = 10;  // Tổng 10 rounds, mỗi người sút 5 lần
    private final int WIN_SCORE = 10;   // Không dùng điều kiện thắng sớm nữa
    private String shooterDirection;
    private Boolean shooterWantsRematch = null;
    private Boolean goalkeeperWantsRematch = null;
    // Thời gian chờ cho mỗi lượt (ví dụ: 15 giây)
    private final int TURN_TIMEOUT = 15;
    // Always start a new round with the shooter picking first
    private ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);

    // Biến lưu trữ Future của nhiệm vụ chờ
    private ScheduledFuture<?> shooterTimeoutTask;
    private ScheduledFuture<?> goalkeeperTimeoutTask;

    // Biến để kiểm tra xem người chơi đã thực hiện hành động chưa
    private boolean shooterActionReceived = false;
    private boolean goalkeeperActionReceived = false;

    private String goalkeeperDirection;

    public GameRoom(ClientHandler player1, ClientHandler player2, DatabaseManager dbManager) throws SQLException {
        this.dbManager = dbManager;
        this.matchId = dbManager.saveMatch(player1.getUser().getId(), player2.getUser().getId(), 0);
        this.shooterScore = 0;
        this.goalkeeperScore = 0;
        this.currentRound = 1;

        // Random chọn người sút và người bắt
        if (new Random().nextBoolean()) {
            this.shooterHandler = player1;
            this.goalkeeperHandler = player2;
        } else {
            this.shooterHandler = player2;
            this.goalkeeperHandler = player1;
        }
    }

    public void startMatch() {
        try {
            // update ingame status for both player
            shooterHandler.getUser().setStatus("ingame");
            goalkeeperHandler.getUser().setStatus("ingame");

            // to do gui message neu can
            String shooterMessage = "Trận đấu bắt đầu! Bạn là người sút.";
            String goalkeeperMessage = "Trận đấu bắt đầu! Bạn là người bắt.";
            shooterHandler.sendMessage(new Message("match_start", shooterMessage));
            goalkeeperHandler.sendMessage(new Message("match_start", goalkeeperMessage));
            requestNextMove();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void requestNextMove() {
        try {
            if (checkEndGame()) {
                endMatch();
                return;
            }
            // Always prompt the shooter first at the start of a round
            shooterActionReceived = false;
            goalkeeperActionReceived = false;
            shooterDirection = null;
            goalkeeperDirection = null;

            // Cancel any pending timeouts from the previous turn
            if (shooterTimeoutTask != null && !shooterTimeoutTask.isDone()) {
                shooterTimeoutTask.cancel(true);
            }
            if (goalkeeperTimeoutTask != null && !goalkeeperTimeoutTask.isDone()) {
                goalkeeperTimeoutTask.cancel(true);
            }

            shooterHandler.sendMessage(new Message("your_turn", TURN_TIMEOUT));
            goalkeeperHandler.sendMessage(new Message("opponent_turn", TURN_TIMEOUT));

            // Schedule shooter timeout
            shooterTimeoutTask = scheduler.schedule(() -> startShooterTimeout(), TURN_TIMEOUT, TimeUnit.SECONDS);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Xử lý hướng sút từ người sút
    public synchronized void handleShot(String shooterDirection, ClientHandler shooter)
            throws SQLException, IOException {
        this.shooterDirection = shooterDirection;
        shooterActionReceived = true; // Đánh dấu đã nhận hành động từ người sút
        
        // Hủy timeout của shooter nếu đang chạy
        if (shooterTimeoutTask != null && !shooterTimeoutTask.isDone()) {
            shooterTimeoutTask.cancel(true);
        }
        
        // Yêu cầu người bắt chọn hướng chặn (goalkeeper picks after shooter)
        goalkeeperHandler.sendMessage(new Message("goalkeeper_turn", TURN_TIMEOUT));
        shooterHandler.sendMessage(new Message("opponent_turn", TURN_TIMEOUT));

        // Bắt đầu đếm thời gian chờ cho người bắt (schedule with delay)
        goalkeeperActionReceived = false;
        if (goalkeeperTimeoutTask != null && !goalkeeperTimeoutTask.isDone()) {
            goalkeeperTimeoutTask.cancel(true);
        }
        goalkeeperTimeoutTask = scheduler.schedule(() -> startGoalkeeperTimeout(), TURN_TIMEOUT, TimeUnit.SECONDS);
    }

    // Xử lý hướng chặn từ người bắt
    public synchronized void handleGoalkeeper(String goalkeeperDirection, ClientHandler goalkeeper)
            throws SQLException, IOException {
        if (this.shooterDirection == null) {
            // Nếu shooterDirection chưa được thiết lập, không thể xử lý
            shooterHandler.sendMessage(new Message("error", "Hướng sút chưa được thiết lập."));
            goalkeeperHandler.sendMessage(new Message("error", "Hướng sút chưa được thiết lập."));
            return;
        }
        this.goalkeeperDirection = goalkeeperDirection;
        goalkeeperActionReceived = true; // Đánh dấu đã nhận hành động từ người bắt

        // Hủy nhiệm vụ chờ của người bắt nếu còn tồn tại
        if (goalkeeperTimeoutTask != null && !goalkeeperTimeoutTask.isDone()) {
            goalkeeperTimeoutTask.cancel(true);
        }

        // Xử lý kết quả và kết thúc round qua hàm dùng chung
        boolean goal = !shooterDirection.equalsIgnoreCase(goalkeeperDirection);
        resolveRound(goal, this.shooterDirection, this.goalkeeperDirection, "normal");
    }

    private void determineWinner() throws SQLException, IOException {
        int winnerId = 0;
        String endReason = "normal";

        if (shooterScore > goalkeeperScore) {
            winnerId = shooterHandler.getUser().getId();
        } else if (goalkeeperScore > shooterScore) {
            winnerId = goalkeeperHandler.getUser().getId();
        }

        if (winnerId != 0) {
            dbManager.updateUserPoints(winnerId, 3);
        }
        dbManager.updateMatchWinner(matchId, winnerId, endReason);

        // Thông báo kết quả trận đấu cho cả hai người chơi
        shooterHandler.sendMessage(new Message("match_result", (shooterScore > goalkeeperScore) ? "win" : "lose"));
        goalkeeperHandler.sendMessage(new Message("match_result", (goalkeeperScore > shooterScore) ? "win" : "lose"));

        // Tạo một ScheduledExecutorService mới để trì hoãn việc gửi tin nhắn
        ScheduledExecutorService rematchScheduler = Executors.newScheduledThreadPool(1);
        rematchScheduler.schedule(() -> {
            // Gửi tin nhắn yêu cầu chơi lại sau 3 giây
            shooterHandler.sendMessage(new Message("play_again_request", "Bạn có muốn chơi lại không?"));
            goalkeeperHandler.sendMessage(new Message("play_again_request", "Bạn có muốn chơi lại không?"));
            // Đóng scheduler sau khi hoàn tất
            rematchScheduler.shutdown();
        }, 3, TimeUnit.SECONDS);
    }

    // Xử lý yêu cầu chơi lại
    public synchronized void handlePlayAgainResponse(boolean playAgain, ClientHandler responder)
            throws SQLException, IOException {
        if (responder == shooterHandler) {
            shooterWantsRematch = playAgain;
        } else if (responder == goalkeeperHandler) {
            goalkeeperWantsRematch = playAgain;
        }

        // Kiểm tra nếu một trong hai người chơi đã thoát
        if (shooterHandler == null || goalkeeperHandler == null) {
            return;
        }

        // Kiểm tra nếu cả hai người chơi đã phản hồi
        if (shooterWantsRematch != null && goalkeeperWantsRematch != null) {
            if (shooterWantsRematch && goalkeeperWantsRematch) {
                // Cả hai người chơi đồng ý chơi lại
                resetGameState();
                startMatch();
            } else {
                // cap nhat status "ingame" -> "online"
                shooterHandler.getUser().setStatus("online");
                goalkeeperHandler.getUser().setStatus("online");

                dbManager.updateUserStatus(shooterHandler.getUser().getId(), "online");
                dbManager.updateUserStatus(goalkeeperHandler.getUser().getId(), "online");

                shooterHandler.getServer()
                        .broadcast(new Message("status_update", shooterHandler.getUser().getUsername() + " is online"));
                goalkeeperHandler.getServer().broadcast(
                        new Message("status_update", goalkeeperHandler.getUser().getUsername() + " is online"));
                // ------------------------------------------------------------//

                // Gửi thông báo kết thúc trận đấu
                shooterHandler.sendMessage(new Message("match_end", "Trận đấu kết thúc."));
                goalkeeperHandler.sendMessage(new Message("match_end", "Trận đấu kết thúc."));

                // Đặt lại biến
                shooterWantsRematch = null;
                goalkeeperWantsRematch = null;

                // Đưa cả hai người chơi về màn hình chính
                shooterHandler.clearGameRoom();
                goalkeeperHandler.clearGameRoom();
            }
        }
    }

    private void resetGameState() throws SQLException {
        // Reset game variables
        shooterScore = 0;
        goalkeeperScore = 0;
        currentRound = 1;
        shooterDirection = null;
        shooterWantsRematch = null;
        goalkeeperWantsRematch = null;

        // Swap shooter and goalkeeper roles for fairness
        ClientHandler temp = shooterHandler;
        shooterHandler = goalkeeperHandler;
        goalkeeperHandler = temp;

        // Create a new match in the database
        matchId = dbManager.saveMatch(shooterHandler.getUser().getId(), goalkeeperHandler.getUser().getId(), 0);
    }
    
    // Đổi vai trò giữa người sút và người bắt
    private void swapRoles() {
        ClientHandler temp = shooterHandler;
        shooterHandler = goalkeeperHandler;
        goalkeeperHandler = temp;
        
        // Cũng phải swap điểm số vì điểm liên quan đến vai trò
        int tempScore = shooterScore;
        shooterScore = goalkeeperScore;
        goalkeeperScore = tempScore;
    }

    // Đảm bảo rằng phương thức endMatch() tồn tại và được định nghĩa chính xác
    private void endMatch() throws SQLException, IOException {
        determineWinner();

        // Reset in-game status for both players after match
        if (shooterHandler != null) {
            shooterHandler.getUser().setStatus("online");
            // todo gui message neu can
        }
        if (goalkeeperHandler != null) {
            goalkeeperHandler.getUser().setStatus("online");
            // todo gui message neu can
        }
    }

    public void handlePlayerDisconnect(ClientHandler disconnectedPlayer) throws SQLException, IOException {
        String resultMessageToWinner = "Đối thủ đã thoát. Bạn thắng trận đấu!";
        String resultMessageToLoser = "Bạn đã thoát. Bạn thua trận đấu!";
        int winnerId;
        String endReason = "player_quit";
        ClientHandler otherPlayer;

        if (disconnectedPlayer == shooterHandler) {
            otherPlayer = goalkeeperHandler;
        } else {
            otherPlayer = shooterHandler;
        }

        shooterWantsRematch = false;
        goalkeeperWantsRematch = false;
        winnerId = otherPlayer.getUser().getId();

        dbManager.updateUserPoints(winnerId, 3);
        dbManager.updateMatchWinner(matchId, winnerId, endReason);

        // cap nhat status "ingame" -> "online"
        otherPlayer.getUser().setStatus("online");
        dbManager.updateUserStatus(otherPlayer.getUser().getId(), "online");
        otherPlayer.getServer()
                .broadcast(new Message("status_update", otherPlayer.getUser().getUsername() + " is online"));

        // cap nhat status "ingame" -> "offline"
        disconnectedPlayer.getUser().setStatus("offline");
        dbManager.updateUserStatus(disconnectedPlayer.getUser().getId(), "offline");
        disconnectedPlayer.getServer()
                .broadcast(new Message("status_update", disconnectedPlayer.getUser().getUsername() + " is offline"));
        // -------------------------------------------------------

        // Gửi thông báo kết thúc trận đấu cho cả hai người chơi
        otherPlayer.sendMessage(new Message("match_end", resultMessageToWinner));
        disconnectedPlayer.sendMessage(new Message("match_end", resultMessageToLoser));

        // Đặt lại trạng thái game room
        shooterWantsRematch = null;
        goalkeeperWantsRematch = null;
        shooterDirection = null;

        // Sử dụng phương thức clearGameRoom() để đặt gameRoom thành null
        if (shooterHandler != null) {
            shooterHandler.clearGameRoom();
        }
        if (goalkeeperHandler != null) {
            goalkeeperHandler.clearGameRoom();
        }

    }

    public void handlePlayerQuit(ClientHandler quittingPlayer) throws SQLException, IOException {
        String resultMessageToLoser = "Bạn đã thoát. Bạn thua trận đấu!";
        String resultMessageToWinner = "Đối thủ đã thoát. Bạn thắng trận đấu!";

        int winnerId;
        String endReason = "player_quit";
        ClientHandler otherPlayer;

        if (quittingPlayer == shooterHandler) {
            winnerId = goalkeeperHandler.getUser().getId();
            otherPlayer = goalkeeperHandler;
            // Cập nhật trạng thái chơi lại
            shooterWantsRematch = false;
        } else {
            winnerId = shooterHandler.getUser().getId();
            otherPlayer = shooterHandler;
            // Cập nhật trạng thái chơi lại
            goalkeeperWantsRematch = false;
        }

        dbManager.updateUserPoints(winnerId, 3);
        dbManager.updateMatchWinner(matchId, winnerId, endReason);

        // cap nhat status "ingame" -> "online"
        shooterHandler.getUser().setStatus("online");
        goalkeeperHandler.getUser().setStatus("online");

        dbManager.updateUserStatus(shooterHandler.getUser().getId(), "online");
        dbManager.updateUserStatus(goalkeeperHandler.getUser().getId(), "online");

        shooterHandler.getServer()
                .broadcast(new Message("status_update", shooterHandler.getUser().getUsername() + " is online"));
        goalkeeperHandler.getServer()
                .broadcast(new Message("status_update", goalkeeperHandler.getUser().getUsername() + " is online"));
        // ------------------------------------------------------------

        // Gửi thông báo kết thúc trận đấu cho cả hai người chơi
        quittingPlayer.sendMessage(new Message("match_end", resultMessageToLoser));
        otherPlayer.sendMessage(new Message("match_end", resultMessageToWinner));

        // Đặt lại trạng thái game room
        shooterWantsRematch = null;
        goalkeeperWantsRematch = null;
        shooterDirection = null;

        // Sử dụng phương thức clearGameRoom() để đặt gameRoom thành null
        if (shooterHandler != null) {
            shooterHandler.clearGameRoom();
        }
        if (goalkeeperHandler != null) {
            goalkeeperHandler.clearGameRoom();
        }

        // Không cần gửi thông báo "return_to_main"
    }

    public synchronized void startShooterTimeout() {
        try {
            if (checkEndGame()) {
                endMatch();
                return;
            }
            if (!shooterActionReceived) {
                // Người sút không thực hiện hành động trong thời gian quy định
                shooterActionReceived = true;
                goalkeeperActionReceived = true; // kết thúc luôn round do hết giờ

                // Thông báo timeout
                shooterHandler.sendMessage(new Message("timeout", "Hết giờ! Bạn bị mất lượt."));
                goalkeeperHandler.sendMessage(new Message("opponent_timeout",
                        "Đối thủ hết giờ! Bạn được cộng 1 điểm."));

                // Chọn hướng để hiển thị animation: đặt cả 2 cùng 'Middle' để mô phỏng thủ môn bắt bóng
                this.shooterDirection = "Middle";
                this.goalkeeperDirection = "Middle";

                // Hủy mọi timeout còn lại
                if (shooterTimeoutTask != null && !shooterTimeoutTask.isDone()) {
                    shooterTimeoutTask.cancel(true);
                }
                if (goalkeeperTimeoutTask != null && !goalkeeperTimeoutTask.isDone()) {
                    goalkeeperTimeoutTask.cancel(true);
                }

                // Kết thúc round: đối thủ (GK) được 1 điểm
                resolveRound(false, this.shooterDirection, this.goalkeeperDirection, "shooter_timeout");
            }
        } catch (SQLException | IOException e) {
            e.printStackTrace();
        }
    }

    private boolean checkEndGame() {
        // Chỉ kết thúc khi đã chơi đủ 10 rounds
        // Không kết thúc sớm dù có người dẫn trước
        return currentRound > MAX_ROUNDS;
    }

    public synchronized void startGoalkeeperTimeout() {
        try {
            if (!goalkeeperActionReceived) {
                // Người bắt không thực hiện hành động trong thời gian quy định
                if (this.shooterDirection == null) {
                    // Shooter chưa chọn hướng, không thể tự động chọn cho GK
                    return;
                }
                goalkeeperActionReceived = true;

                // Gửi thông báo timeout
                goalkeeperHandler.sendMessage(
                        new Message("timeout", "Hết giờ! Bạn bị mất lượt."));
                shooterHandler.sendMessage(new Message("opponent_timeout",
                        "Đối thủ hết giờ! Bạn được cộng 1 điểm."));

                // Chọn hướng thủ môn khác với người sút để đảm bảo người sút ghi bàn
                if ("Left".equalsIgnoreCase(this.shooterDirection)) {
                    this.goalkeeperDirection = "Right";
                } else if ("Right".equalsIgnoreCase(this.shooterDirection)) {
                    this.goalkeeperDirection = "Left";
                } else { // shooter chọn Middle
                    this.goalkeeperDirection = "Left"; // chọn khác để đảm bảo có bàn thắng
                }

                // Hủy mọi timeout còn lại
                if (goalkeeperTimeoutTask != null && !goalkeeperTimeoutTask.isDone()) {
                    goalkeeperTimeoutTask.cancel(true);
                }
                if (shooterTimeoutTask != null && !shooterTimeoutTask.isDone()) {
                    shooterTimeoutTask.cancel(true);
                }

                // Kết thúc round: người sút được 1 điểm
                resolveRound(true, this.shooterDirection, this.goalkeeperDirection, "goalkeeper_timeout");
            }
        } catch (SQLException | IOException e) {
            e.printStackTrace();
        }
    }

    // Hàm dùng chung để kết thúc một round với kết quả đã biết
    private synchronized void resolveRound(boolean goal, String shooterDir, String gkDir, String reason)
            throws SQLException, IOException {
        // Hủy mọi timeout còn lại để tránh chạy trễ
        if (shooterTimeoutTask != null && !shooterTimeoutTask.isDone()) {
            shooterTimeoutTask.cancel(true);
        }
        if (goalkeeperTimeoutTask != null && !goalkeeperTimeoutTask.isDone()) {
            goalkeeperTimeoutTask.cancel(true);
        }

        // Cập nhật điểm số
        if (goal) {
            shooterScore++;
        } else {
            goalkeeperScore++;
        }

        // Gửi kết quả riêng cho từng người theo góc nhìn của họ
        String kick_result_shooter = (goal ? "win" : "lose") + "-" + shooterDir + "-" + gkDir;
        String kick_result_goalkeeper = (goal ? "lose" : "win") + "-" + shooterDir + "-" + gkDir;
        shooterHandler.sendMessage(new Message("kick_result", kick_result_shooter));
        goalkeeperHandler.sendMessage(new Message("kick_result", kick_result_goalkeeper));

        // Lưu chi tiết trận đấu vào database (result lưu theo góc nhìn người sút)
        dbManager.saveMatchDetails(matchId, currentRound,
                shooterHandler.getUser().getId(),
                goalkeeperHandler.getUser().getId(),
                shooterDir, gkDir, goal ? "win" : "lose");

        // Gửi tỷ số cập nhật cho từng người chơi
        Message scoreMessageToShooter = new Message("update_score",
                new int[] { shooterScore, goalkeeperScore, currentRound });
        Message scoreMessageToGoalkeeper = new Message("update_score",
                new int[] { goalkeeperScore, shooterScore, currentRound });
        shooterHandler.sendMessage(scoreMessageToShooter);
        goalkeeperHandler.sendMessage(scoreMessageToGoalkeeper);

        // Tăng round
        currentRound++;

        // Kiểm tra nếu đã hết 10 rounds
        if (currentRound > MAX_ROUNDS) {
            determineWinner();
            return;
        }

        // Đổi vai trò sau mỗi round để cả 2 người luân phiên sút/bắt
        swapRoles();

        // Thông báo vai trò mới
        String shooterMessage = "Round " + currentRound + ": Bây giờ bạn là người sút.";
        String goalkeeperMessage = "Round " + currentRound + ": Bây giờ bạn là người bắt.";
        shooterHandler.sendMessage(new Message("role_change", shooterMessage));
        goalkeeperHandler.sendMessage(new Message("role_change", goalkeeperMessage));

        // Reset trạng thái và tiếp tục round mới
        shooterDirection = null;
        goalkeeperDirection = null;
        shooterActionReceived = false;
        goalkeeperActionReceived = false;
        requestNextMove();
    }
}
