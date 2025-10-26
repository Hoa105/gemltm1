package client.GUI;

import client.Client;
import common.Message;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import java.io.IOException;

public class MainController {

    @FXML
    private Label welcomeLabel;

    private Client client;

    public void setClient(Client client) {
        this.client = client;
        if (client.getUser() != null) {
            welcomeLabel.setText("Chào mừng, " + client.getUser().getUsername() + "!");
        }
    }

    @FXML
    private void handleLogout() throws IOException {
        if (client.getUser() != null) {
            Message logoutMessage = new Message("logout", client.getUser().getId());
            client.sendMessage(logoutMessage);
            client.showLoginUI();
        }
    }

    @FXML
    private void handleShowPlayerList() {
        try {
            client.showPlayerListUI();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleShowHistory() {
        try {
            client.showHistoryUI();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleShowLeaderboard() {
        try {
            client.showLeaderboardUI();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
