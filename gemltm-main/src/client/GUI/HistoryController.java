package client.GUI;

import client.Client;
import common.Match;
import common.MatchDetails;
import common.Message;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.List;

public class HistoryController {

    @FXML
    private TableView<Match> matchesTable;
    @FXML
    private TableColumn<Match, Integer> matchIdColumn;
    @FXML
    private TableColumn<Match, String> opponentColumn;
    @FXML
    private TableColumn<Match, String> matchResultColumn;
    @FXML
    private TableColumn<Match, String> matchTimeColumn;

    @FXML
    private TableView<MatchDetails> historyTable;
    @FXML
    private TableColumn<MatchDetails, Integer> roundColumn;
    @FXML
    private TableColumn<MatchDetails, String> timeColumn;
    @FXML
    private TableColumn<MatchDetails, String> roleColumn;
    @FXML
    private TableColumn<MatchDetails, String> directionColumn;
    @FXML
    private TableColumn<MatchDetails, String> historyResultColumn;

    private Client client;
    private int demRole = 0;

    public void setClient(Client client) throws IOException {
        this.client = client;
        loadUserMatches();
    }

    @FXML
    private void initialize() {
        // Cấu hình bảng danh sách trận đấu
        matchIdColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        opponentColumn.setCellValueFactory(cellData -> {
            Match match = cellData.getValue();
            String opponentName = match.getOpponentName(client.getUser().getId());
            return new SimpleStringProperty(opponentName);
        });
        matchResultColumn.setCellValueFactory(cellData -> {
            Match match = cellData.getValue();
            String result = match.getResult(client.getUser().getId());
            return new SimpleStringProperty(result);
        });
        matchTimeColumn.setCellValueFactory(cellData -> {
            Timestamp time = cellData.getValue().getTime();
            return new SimpleStringProperty(time != null ? time.toString() : "");
        });

        // Cấu hình bảng chi tiết trận đấu
        roundColumn.setCellValueFactory(new PropertyValueFactory<>("round"));
        timeColumn.setCellValueFactory(cellData -> {
            Timestamp time = cellData.getValue().getTime();
            return new SimpleStringProperty(time != null ? time.toString() : "");
        });
        roleColumn.setCellValueFactory(cellData -> {
            MatchDetails md = cellData.getValue();
            String role;
            if (demRole % 2 == 0)
                role = (md.getShooterId() == client.getUser().getId()) ? "Sút" : "Bắt";
            else
                role = (md.getShooterId() == client.getUser().getId()) ? "Bắt" : "Sút";
            demRole += 1;
            return new SimpleStringProperty(role);
        });
        directionColumn.setCellValueFactory(cellData -> {
            MatchDetails md = cellData.getValue();
            String direction = (md.getShooterId() == client.getUser().getId()) 
                ? md.getShooterDirection() : md.getGoalkeeperDirection();
            return new SimpleStringProperty(direction);
        });
        historyResultColumn.setCellValueFactory(cellData -> {
            MatchDetails md = cellData.getValue();
            String result = "";
            if (md.getShooterId() == client.getUser().getId()) {
                if (md.getShooterDirection() != null && md.getGoalkeeperDirection() != null
                        && md.getShooterDirection().equalsIgnoreCase(md.getGoalkeeperDirection())) {
                    result = (md.getRound() % 2 == 1) ? "lose" : "win";
                } else {
                    result = (md.getRound() % 2 == 1) ? "win" : "lose";
                }
            } else {
                if (md.getShooterDirection() != null && md.getGoalkeeperDirection() != null
                        && md.getShooterDirection().equalsIgnoreCase(md.getGoalkeeperDirection())) {
                    result = (md.getRound() % 2 == 1) ? "win" : "lose";
                } else {
                    result = (md.getRound() % 2 == 1) ? "lose" : "win";
                }
            }
            return new SimpleStringProperty(result);
        });

        // Sự kiện click để hiển thị chi tiết trận đấu
        matchesTable.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                Match clickedMatch = newValue;
                try {
                    Message request = new Message("get_match_details", clickedMatch.getId());
                    client.sendMessage(request);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void loadUserMatches() throws IOException {
        Message request = new Message("get_user_matches", null);
        client.sendMessage(request);
    }

    public void updateMatchesList(List<Match> matches) {
        ObservableList<Match> matchesList = FXCollections.observableArrayList(matches);
        matchesTable.setItems(matchesList);
    }

    public void showMatchDetails(List<MatchDetails> details) {
        ObservableList<MatchDetails> detailsList = FXCollections.observableArrayList(details);
        historyTable.setItems(detailsList);
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
