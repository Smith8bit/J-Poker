import java.util.*;

public class Main {
    public static void main(String[] args) {
        List<Player> playerList = new ArrayList<>();
        Player player_1 = new Player("player_1", 1000);
        playerList.add(player_1);
        Player player_2 = new Player("player_2", 1000);
        playerList.add(player_2);
        Player player_3 = new Player("player_3", 1000);
        playerList.add(player_3);

        Game game = new Game(playerList, 20);
        game.startHand();
        System.out.println(game.playerOrder);

        System.out.println(player_1.hand);
        System.out.println(player_2.hand);
        System.out.println(player_3.hand);

        game.handleAction("player_1", "fold", 0);
        game.handleAction("player_2", "call", 20);
        game.handleAction("player_3", "call", 20);

        game.handleAction("player_1", "fold", 0);
        game.handleAction("player_2", "call", 20);
        game.handleAction("player_3", "call", 20);

//        game._progressStreet();
//        game._progressStreet();
//        game._progressStreet();
    }
}