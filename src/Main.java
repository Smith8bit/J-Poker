import java.util.*;

public class Main {
    public static void main(String[] args) {
        List<Player> playerList = new ArrayList<>();
        playerList.add(new Player("player_1", 1000));
        playerList.add(new Player("player_2", 1000));
        playerList.add(new Player("player_3", 1000));

        Game game = new Game(playerList, 20);
    }
}