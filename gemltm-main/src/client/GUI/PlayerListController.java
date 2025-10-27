package client.GUI;

import client.Client;
import common.Message;
import common.User;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PlayerListController {

    @FXML
    private TextField searchField;
    @FXML
    private TableView<User> usersTable;
    @FXML
    private TableColumn<User, String> nameColumn;
    @FXML
    private TableColumn<User, Integer> pointsColumn;
    @FXML
    private TableColumn<User, String> statusColumn;

    private Client client;
    private ObservableList<User> usersList = FXCollections.observableArrayList();

    public void setClient(Client client) throws IOException {
        this.client = client;
        loadUsers();
    }

    @FXML
    private void initialize() {
        // Cấu hình bảng người chơi
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("username"));
        pointsColumn.setCellValueFactory(new PropertyValueFactory<>("points"));
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));

        // Custom cell factory cho statusColumn
        statusColumn.setCellFactory(column -> new TableCell<User, String>() {
            private final HBox hBox = new HBox(5);
            private final Circle circle = new Circle(5);
            private final Label label = new Label();

            {
                label.setStyle("-fx-text-fill: black; -fx-font-weight: bold;");
                hBox.getChildren().addAll(circle, label);
            }

            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    Color color;
                    switch (status.trim()) {
                        case "online":
                            color = Color.GREEN;
                            label.setStyle("-fx-text-fill: green; -fx-font-weight: bold;");
                            break;
                        case "ingame":
                            color = Color.RED;
                            label.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
                            break;
                        case "offline":
                            color = Color.GRAY;
                            label.setStyle("-fx-text-fill: gray; -fx-font-weight: bold;");
                            break;
                        default:
                            color = Color.BLACK;
                            label.setStyle("-fx-text-fill: black; -fx-font-weight: bold;");
                            break;
                    }
                    circle.setFill(color);
                    label.setText(status);
                    setGraphic(hBox);
                    setText(null);
                }
            }
        });

        // Sự kiện double click để gửi yêu cầu trận đấu
        usersTable.setRowFactory(tv -> {
            TableRow<User> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && (!row.isEmpty())) {
                    User clickedUser = row.getItem();
                    if (clickedUser.getId() != client.getUser().getId()) {
                        Message matchRequest = new Message("request_match", clickedUser.getId());
                        try {
                            client.sendMessage(matchRequest);
                        } catch (IOException ex) {
                            Logger.getLogger(PlayerListController.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                }
            });
            return row;
        });
    }

    private void loadUsers() throws IOException {
        Message request = new Message("get_users", null);
        client.sendMessage(request);
    }

    public void updateUsersList(List<User> newUsers) {
        Platform.runLater(() -> {
            usersList.setAll(newUsers);
            usersTable.setItems(usersList);
            usersTable.refresh();
        });
    }

    public void updateStatus(String statusUpdate) {
        if (statusUpdate == null || statusUpdate.isEmpty()) {
            return;
        }
        String[] parts = statusUpdate.split(" ");
        if (parts.length >= 3) {
            String username = parts[0];
            String status = parts[2].replace(".", "");
            for (User user : usersList) {
                if (user.getUsername().equalsIgnoreCase(username)) {
                    user.setStatus(status);
                    usersTable.refresh();
                    break;
                }
            }
        }
    }

    public void showMatchRequest(int requesterId) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Yêu Cầu Trận Đấu");
        alert.setHeaderText("Bạn nhận được yêu cầu trận đấu từ người chơi ID: " + requesterId);
        alert.setContentText("Bạn có muốn đồng ý?");

        alert.showAndWait().ifPresent(response -> {
            boolean accepted = response == ButtonType.OK;
            Object[] data = { requesterId, accepted };
            Message responseMessage = new Message("match_response", data);
            try {
                client.sendMessage(responseMessage);
            } catch (IOException ex) {
                Logger.getLogger(PlayerListController.class.getName()).log(Level.SEVERE, null, ex);
            }
        });
    }

    public void handleMatchResponse(String response) {
        // Stop any game sounds in case an invite failed/was declined
        try {
            if (client != null) client.stopGameSounds();
        } catch (Exception ex) {
            System.err.println("Error stopping sounds on match response: " + ex.getMessage());
        }

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Trận Đấu");
        alert.setHeaderText(null);
        alert.setContentText(response);
        alert.showAndWait();
    }

    @FXML
    private void handleFilterOnline() {
        ObservableList<User> filtered = FXCollections.observableArrayList();
        for (User user : usersList) {
            if (user.getStatus().equalsIgnoreCase("online")) {
                filtered.add(user);
            }
        }
        usersTable.setItems(filtered);
    }

    @FXML
    private void handleSearch() {
        String keyword = searchField.getText().toLowerCase();
        if (keyword.isEmpty()) {
            usersTable.setItems(usersList);
            return;
        }
        ObservableList<User> filtered = FXCollections.observableArrayList();
        for (User user : usersList) {
            if (user.getUsername().toLowerCase().contains(keyword)) {
                filtered.add(user);
            }
        }
        usersTable.setItems(filtered);
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