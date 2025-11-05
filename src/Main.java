import java.util.*;

public class Main {
    public static void main(String[] args) {
        List<Player> playerList = new ArrayList<>();
        playerList.add(new Player("player_1", 1000));
        playerList.add(new Player("player_2", 1000));
        playerList.add(new Player("player_3", 1000));
        Game game = new Game(playerList, 20);

        game.start();
        game.call(playerList.get(2).id);
        game.call(playerList.get(0).id);
        int currentP = game.currentActorPos;

        game.check();
        game.check();
        game.check();

        game.check();
        game.check();
        game.check();

        game.check();
        game.check();
        game.check();

//        game.bet(game.activePlayerIds.get(game.currentActorPos), 10);
//        System.out.println(game.activePlayerIds.get(game.currentActorPos));
//
//        game.fold(game.activePlayerIds.get(game.currentActorPos));
//
//        System.out.println(game.activePlayerIds.get(game.currentActorPos));
//        game.call(game.activePlayerIds.get(game.currentActorPos));
    }
}