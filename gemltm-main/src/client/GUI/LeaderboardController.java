package client.GUI;

import client.Client;
import common.Message;
import common.User;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.io.IOException;
import java.util.List;

public class LeaderboardController {

    @FXML
    private TableView<User> leaderboardTable;
    @FXML
    private TableColumn<User, Integer> rankColumn;
    @FXML
    private TableColumn<User, String> lbNameColumn;
    @FXML
    private TableColumn<User, Integer> lbPointsColumn;

    private Client client;

    public void setClient(Client client) throws IOException {
        this.client = client;
        loadLeaderboard();
    }

    @FXML
    private void initialize() {
        // Cấu hình bảng xếp hạng
        rankColumn.setCellValueFactory(cellData -> {
            int index = leaderboardTable.getItems().indexOf(cellData.getValue()) + 1;
            return new SimpleIntegerProperty(index).asObject();
        });
        lbNameColumn.setCellValueFactory(new PropertyValueFactory<>("username"));
        lbPointsColumn.setCellValueFactory(new PropertyValueFactory<>("points"));
    }

    private void loadLeaderboard() throws IOException {
        Message request = new Message("get_leaderboard", null);
        client.sendMessage(request);
    }

    public void updateLeaderboard(List<User> leaderboard) {
        ObservableList<User> leaderboardList = FXCollections.observableArrayList(leaderboard);
        leaderboardTable.setItems(leaderboardList);
    }

    @FXML
    private void handleBack() throws IOException {
        client.showMainUI();
    }

    @FXML
    private void handleLogout() throws IOException {
        if (client.getUser() != null) {
            Message logoutMessage = new Message("logout", client.getUser().getId());
            client.sendMessage(logoutMessage);
            client.showLoginUI();
        }
    }
}
