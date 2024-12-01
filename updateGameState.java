class GamePanel extends JPanel {
    private GameState gameState;

    public void updateGameState(GameState state) {
        this.gameState = state;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (gameState != null) {
            for (GameState.Snake snake : gameState.getSnakes()) {
                g.setColor(Color.GREEN);
                for (GameState.Point point : snake.getBody()) {
                    g.fillRect(point.x * 20, point.y * 20, 20, 20);
                }
            }
        }
    }
}
