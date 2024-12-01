import com.google.gson.Gson;
import jakarta.websocket.*;
import jakarta.websocket.server.ServerEndpoint;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@ServerEndpoint("/game")
public class GameServer {

    private static final Map<Session, String> players = new ConcurrentHashMap<>();
    private static final Map<String, Snake> snakes = new ConcurrentHashMap<>();
    private static final Gson gson = new Gson();

    @OnOpen
    public void onOpen(Session session) {
        System.out.println("New player connected: " + session.getId());
        players.put(session, null); // Name will be assigned later.
    }

    @OnMessage
    public void onMessage(Session session, String message) {
        PlayerMessage msg = gson.fromJson(message, PlayerMessage.class);

        switch (msg.getType()) {
            case "join" -> {
                String playerName = msg.getData();
                players.put(session, playerName);
                snakes.put(playerName, new Snake(playerName));
                broadcastState();
            }
            case "move" -> {
                String direction = msg.getData();
                Snake snake = snakes.get(players.get(session));
                if (snake != null) {
                    snake.move(direction);
                    broadcastState();
                }
            }
        }
    }

    @OnClose
    public void onClose(Session session) {
        String playerName = players.remove(session);
        if (playerName != null) {
            snakes.remove(playerName);
            broadcastState();
        }
        System.out.println("Player disconnected: " + session.getId());
    }

    @OnError
    public void onError(Session session, Throwable throwable) {
        System.err.println("Error for session " + session.getId() + ": " + throwable.getMessage());
    }

    private void broadcastState() {
        GameState state = new GameState(snakes.values());
        String stateJson = gson.toJson(state);

        for (Session session : players.keySet()) {
            try {
                session.getBasicRemote().sendText(stateJson);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private static class PlayerMessage {
        private String type;
        private String data;

        public String getType() {
            return type;
        }

        public String getData() {
            return data;
        }
    }

    private static class Snake {
        private final String name;
        private final List<Point> body = new ArrayList<>();
        private String direction = "UP";

        public Snake(String name) {
            this.name = name;
            body.add(new Point(10, 10));
        }

        public void move(String newDirection) {
            direction = newDirection; // Update direction
            Point head = body.get(0);

            Point newHead = switch (direction) {
                case "UP" -> new Point(head.x, head.y - 1);
                case "DOWN" -> new Point(head.x, head.y + 1);
                case "LEFT" -> new Point(head.x - 1, head.y);
                case "RIGHT" -> new Point(head.x + 1, head.y);
                default -> head;
            };

            body.add(0, newHead); // Add new head
            body.remove(body.size() - 1); // Remove tail
        }

        public String getName() {
            return name;
        }

        public List<Point> getBody() {
            return body;
        }
    }

    private static class Point {
        int x, y;

        public Point(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }

    private static class GameState {
        private final Collection<Snake> snakes;

        public GameState(Collection<Snake> snakes) {
            this.snakes = snakes;
        }

        public Collection<Snake> getSnakes() {
            return snakes;
        }
    }
}
