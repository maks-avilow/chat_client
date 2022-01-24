package sample;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import org.example.SimpleAuthService;

import java.io.*;
import java.net.Socket;
import java.net.URL;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;

public class Controller implements Initializable {

    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;
    private String nick;

    @FXML
    public TextField regLogin;
    @FXML
    public PasswordField regPassword;
    @FXML
    public TextField regNick;
    @FXML
    public HBox regPanel;
    @FXML
    private HBox clientPanel;
    @FXML
    private HBox msgPanel;
    @FXML
    private TextField textField;
    @FXML
    private Button btnSend;
    @FXML
    private ListView<String> clientList;
    @FXML
    private TextArea textArea;
    @FXML
    private PasswordField passwordField;
    @FXML
    private TextField loginField;
    @FXML
    private HBox authPanel;

    private void connect() {
        try {
            this.socket = new Socket("localhost", 8189);
            this.in = new DataInputStream(socket.getInputStream());
            this.out = new DataOutputStream(socket.getOutputStream());
            setAuth(false);

            new Thread(() -> {
                try {
                    while (true) { // Ждем сообщения об успешной авторизации ("/authok")
                        final String msgAuth = in.readUTF();
                        System.out.println("CLIENT: Received message: " + msgAuth);
                        if (msgAuth.startsWith("/authok")) {
                            setAuth(true);
                            loadHistory();
                            nick = msgAuth.split("\\s")[1];
                            textArea.appendText("Успешная авторизация под ником " + nick + "\n");
                            break;
                        }
                        textArea.appendText(msgAuth + "\n");
                    }
                    while (true) { // После успешной авторизации можно обрабатывать все сообщения
                        String msgFromServer = in.readUTF();
                        System.out.println("CLIENT: Received message: " + msgFromServer);
                        if (msgFromServer.startsWith(nick)) {
                            msgFromServer = "[You] " + msgFromServer;
                        }
                        if ("/end".equalsIgnoreCase(msgFromServer)) {
                            break;
                        }
                        if (msgFromServer.startsWith("/clients")) {  // обновляем список клиентов. в переменной clients лежат nick
                            final List<String> clients = new ArrayList<>(Arrays.asList(msgFromServer.split("\\s")));
                            clients.remove(0);
                            clientList.getItems().clear();
                            clientList.getItems().addAll(clients);
                            continue;
                        }
                        textArea.appendText(msgFromServer + "\n");
                        SaveHistory();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    throw new RuntimeException(e);
                } finally {
                    try {
                        setAuth(false);
                        socket.close();
                        nick = "";
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                }
            }).start();

        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    private void setAuth(boolean isAuthSuccess) {
        authPanel.setVisible(!isAuthSuccess);
        authPanel.setManaged(!isAuthSuccess);

        msgPanel.setVisible(isAuthSuccess);
        msgPanel.setManaged(isAuthSuccess);

        clientPanel.setVisible(isAuthSuccess);
        clientPanel.setManaged(isAuthSuccess);
    }

    public void sendAuth(ActionEvent actionEvent) {
        if (socket == null || socket.isClosed()) {    // если разлогинились
            connect();
        }

        try {
            System.out.println("CLIENT: Send auth message");
            out.writeUTF("/auth " + loginField.getText() + " " + passwordField.getText());
            loginField.clear();
            passwordField.clear();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendMsg(ActionEvent actionEvent) {
        try {
            final String msg = textField.getText();
            System.out.println("CLIENT: Send message to server: " + msg);
            out.writeUTF(msg);
            textField.clear();
            textField.requestFocus();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        connect();
    }


    public void selectClient(MouseEvent mouseEvent) {
        if (mouseEvent.getClickCount() == 2) {
            final String msg = textField.getText();
            String nickname = clientList.getSelectionModel().getSelectedItem();
            textField.setText("/w " + nickname + " " + msg);
            textField.requestFocus();
            textField.selectEnd();
        }
    }
    private void SaveHistory() throws IOException {
        try {
            File history = new File("historycal.txt");
            if (!history.exists()) {
                System.out.println("Файла истории не существует. Сейчас он будет создан");
                history.createNewFile();
            }
            PrintWriter fileWriter = new PrintWriter(new FileWriter(history, false));

            BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
            bufferedWriter.write(textArea.getText());
            bufferedWriter.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadHistory() throws IOException {
        int posHistory = 100;
        File history = new File("historycal.txt");
        List<String> historyList = new ArrayList<>();
        FileInputStream in = new FileInputStream(history);
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(in));

        String temp;
        while ((temp = bufferedReader.readLine()) != null) {
            historyList.add(temp);
        }

        if (historyList.size() > posHistory) {
            for (int i = historyList.size() - posHistory; i <= (historyList.size() - 1); i++) {
                textArea.appendText(historyList.get(i) + "\n");
            }
        } else {
            for (int i = 0; i < posHistory; i++) {
                System.out.println(historyList.get(i));
            }
        }
    }
    public void registration() throws SQLException {
        if(socket == null || socket.isClosed()) {
            connect();
        }
        SimpleAuthService.insertNewUsers(regLogin.getText(), regPassword.getText(), regNick.getText());
        System.out.println("Поздравляем с успешной регистрацией.\nВойдите через форму авторизации. ");
        regLogin.clear();
        regPassword.clear();
        regNick.clear();
    }
}