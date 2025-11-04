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

        Scanner input = new Scanner(System.in);

        Game game = new Game(playerList, 20);
        game.start();

        System.out.println("=== GAME STARTED ===");
        System.out.println("Big Blind: 20, Small Blind: 10");

        while (game.street != Street.SHOWDOWN) {
            // Display game state
            displayGameState(game, playerList);

            // Get current player
            String currentPlayerId = game.playerOrder.get(game.getCurrentActorPos());
            Player currentPlayer = getPlayerById(playerList, currentPlayerId);

            // Skip if player is not active (folded)
            if (!game.getActivePlayerIds().contains(currentPlayerId)) {
                System.out.println("Player " + currentPlayerId + " has folded, skipping...");
                continue;
            }

            System.out.println("\n>>> " + currentPlayerId + "'s turn <<<");
            System.out.println("Your hand: " + currentPlayer.hand);
            System.out.println("Your stack: " + currentPlayer.stack);
            System.out.println("Current bet to match: " + game.getCurrentBet());
            System.out.println("Your current bet this round: " + game.getPlayerBet(currentPlayerId));

            // Show valid actions
            showValidActions(game, currentPlayerId, currentPlayer);

            System.out.print("Enter action (Fold/Check/Bet/Call/Raise): ");
            String action = input.nextLine().trim();

            int amount = 0;
            if (action.equalsIgnoreCase("Bet") || action.equalsIgnoreCase("Raise")) {
                System.out.print("Enter amount: ");
                try {
                    amount = Integer.parseInt(input.nextLine().trim());
                } catch (NumberFormatException e) {
                    System.out.println("Invalid amount, defaulting to 0");
                    amount = 0;
                }
            }

            game.handleAction(currentPlayerId, action, amount);

            // Check if only one player remains
            if (game.getActivePlayerIds().size() == 1) {
                System.out.println("\n=== GAME OVER ===");
                System.out.println("All other players folded!");
                break;
            }
        }

        // Final game state
        System.out.println("\n=== FINAL STACKS ===");
        for (Player player : playerList) {
            System.out.println(player.id + ": " + player.stack);
        }

        input.close();
    }

    private static void displayGameState(Game game, List<Player> playerList) {
        System.out.println("\n========================================");
        System.out.println("Street: " + game.street);
        System.out.println("Board: " + game.getBoard());
        System.out.println("Pot: " + game.getPot());
        System.out.println("Active players: " + game.getActivePlayerIds());
        System.out.println("========================================");
    }

    private static void showValidActions(Game game, String playerId, Player player) {
        int currentBet = game.getCurrentBet();
        int playerBet = game.getPlayerBet(playerId);
        int toCall = currentBet - playerBet;

        System.out.println("\nValid actions:");
        System.out.println("  - Fold");

        if (toCall == 0) {
            System.out.println("  - Check");
            System.out.println("  - Bet [amount]");
        } else {
            System.out.println("  - Call (" + toCall + " to call)");
            System.out.println("  - Raise [amount]");
        }
    }

    private static Player getPlayerById(List<Player> players, String id) {
        for (Player p : players) {
            if (p.id.equals(id)) {
                return p;
            }
        }
        return null;
    }
}