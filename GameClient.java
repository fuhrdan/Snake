import com.google.gson.Gson;
import jakarta.websocket.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.net.URI;

@ClientEndpoint
public class GameClient extends JFrame {

    private static final Gson gson = new Gson();
    private Session session;
    private GamePanel gamePanel;

    public GameClient(String playerName) {
        setTitle("Snake Game");
        setSize(800, 600);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        gamePanel = new GamePanel();
        add(gamePanel);

        addKeyListener(new KeyListener() {
            @Override
            public void keyPressed(KeyEvent e) {
                String direction = switch (e.getKeyCode()) {
                    case KeyEvent.VK_UP -> "UP";
                    case KeyEvent.VK_DOWN -> "DOWN";
                    case KeyEvent.VK_LEFT -> "LEFT";
                    case KeyEvent.VK_RIGHT -> "RIGHT";
                    default -> null;
                };
                if (direction != null) {
                    sendMessage("move", direction);
                }
            }

            @Override
            public void keyTyped(KeyEvent e) {}
            @Override
            public void keyReleased(KeyEvent e) {}
        });

        connectToServer(playerName);
    }

    private void connectToServer(String playerName) {
        try {
            WebSocketContainer container = ContainerProvider.getWebSocketContainer();
            session = container.connectToServer(this, URI.create("ws://localhost:8080/game"));
            sendMessage("join", playerName);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sendMessage(String type, String data) {
        try {
            PlayerMessage msg = new PlayerMessage(type, data);
            session.getBasicRemote().sendText(gson.toJson(msg));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @OnMessage
    public void onMessage(String message) {
        GameState state = gson.fromJson(message, GameState.class);
        gamePanel.updateGameState(state);
    }

    public static void main(String[] args) {
        String playerName = JOptionPane.showInputDialog("Enter your name:");
        if (playerName != null && !playerName.isEmpty()) {
            SwingUtilities.invokeLater(() -> new GameClient(playerName).setVisible(true));
        }
    }

    private static class PlayerMessage {
        private final String type;
        private final String data;

        public PlayerMessage(String type, String data) {
            this.type = type;
            this.data = data;
        }
    }
}
