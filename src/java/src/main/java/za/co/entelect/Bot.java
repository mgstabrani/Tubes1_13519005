package za.co.entelect.challenge;

import za.co.entelect.challenge.command.*;
import za.co.entelect.challenge.entities.*;
import za.co.entelect.challenge.enums.CellType;
import za.co.entelect.challenge.enums.Direction;

import java.lang.annotation.Target;
import java.util.*;
import java.util.stream.Collectors;

public class Bot {

    private Random random;
    private GameState gameState;
    private Opponent opponent;
    private MyWorm currentWorm;

    public Bot(Random random, GameState gameState) {
        this.random = random;
        this.gameState = gameState;
        this.opponent = gameState.opponents[0];
        this.currentWorm = getCurrentWorm(gameState);
    }

    private MyWorm getCurrentWorm(GameState gameState) {
        return Arrays.stream(gameState.myPlayer.worms)
                .filter(myWorm -> myWorm.id == gameState.currentWormId)
                .findFirst()
                .get();
    }

    public Command run() {

        MyWorm myTeam = getCurrentWorm(gameState);
        List<Cell> surround = getSurroundingCells(myTeam.position.x, myTeam.position.y);
        if (gameState.map[myTeam.position.x][myTeam.position.y].type == CellType.LAVA) {
            for (Cell cell : surround) {
                if (cell.type != CellType.LAVA) {
                    if (cell.type == CellType.DIRT) {
                        return new DigCommand(cell.x, cell.y);
                    }
                    return new MoveCommand(cell.x, cell.y);
                }
            }
        }

        Worm enemy = getFirstWormInRange();
        if (enemy != null) {
            Direction dir = resolveDirection(currentWorm.position, enemy.position);
            return new ShootCommand(dir);
        }

        for (Cell num : surround) {
            if (num.powerUp != null) {
                return new MoveCommand(num.x, num.y);
            }
        }

        Direction now = toCenter(myTeam.position);
        if (now != null) {
                Cell c = surround.stream()
                        .filter(w -> (w.x == myTeam.position.x + now.x) && (w.y == myTeam.position.y + now.y))
                        .findFirst()
                        .get();
                if (c.type.equals(CellType.DIRT)) {
                    return new DigCommand(myTeam.position.x + now.x, myTeam.position.y + now.y);
                }
                return new MoveCommand(myTeam.position.x + now.x, myTeam.position.y + now.y);
        }

        return new DoNothingCommand();
    }

    private Worm getFirstWormInRange() {

        Set<String> cells = constructFireDirectionLines(currentWorm.weapon.range)
                .stream()
                .flatMap(Collection::stream)
                .map(cell -> String.format("%d_%d", cell.x, cell.y))
                .collect(Collectors.toSet());

        for (Worm enemyWorm : opponent.worms) {
            String enemyPosition = String.format("%d_%d", enemyWorm.position.x, enemyWorm.position.y);
            if (cells.contains(enemyPosition)) {
                return enemyWorm;
            }
        }

        return null;
    }

    private List<List<Cell>> constructFireDirectionLines(int range) {
        List<List<Cell>> directionLines = new ArrayList<>();
        for (Direction direction : Direction.values()) {
            List<Cell> directionLine = new ArrayList<>();
            for (int directionMultiplier = 1; directionMultiplier <= range; directionMultiplier++) {

                int coordinateX = currentWorm.position.x + (directionMultiplier * direction.x);
                int coordinateY = currentWorm.position.y + (directionMultiplier * direction.y);

                if (!isValidCoordinate(coordinateX, coordinateY)) {
                    break;
                }

                if (euclideanDistance(currentWorm.position.x, currentWorm.position.y, coordinateX, coordinateY) > range) {
                    break;
                }

                Cell cell = gameState.map[coordinateY][coordinateX];
                if (cell.type != CellType.AIR) {
                    break;
                }

                directionLine.add(cell);
            }
            directionLines.add(directionLine);
        }

        return directionLines;
    }

    private List<Cell> getSurroundingCells(int x, int y) {
        ArrayList<Cell> cells = new ArrayList<>();
        for (int i = x - 1; i <= x + 1; i++) {
            for (int j = y - 1; j <= y + 1; j++) {
                // Don't include the current position
                if (i != x && j != y && isValidCoordinate(i, j)) {
                    cells.add(gameState.map[j][i]);
                }
            }
        }

        return cells;
    }

    private int euclideanDistance(int aX, int aY, int bX, int bY) {
        return (int) (Math.sqrt(Math.pow(aX - bX, 2) + Math.pow(aY - bY, 2)));
    }

    private boolean isValidCoordinate(int x, int y) {
        return x >= 0 && x < gameState.mapSize
                && y >= 0 && y < gameState.mapSize;
    }

    private Direction resolveDirection(Position a, Position b) {
        StringBuilder builder = new StringBuilder();

        int verticalComponent = b.y - a.y;
        int horizontalComponent = b.x - a.x;

        if (verticalComponent < 0) {
            builder.append('N');
        } else if (verticalComponent > 0) {
            builder.append('S');
        }

        if (horizontalComponent < 0) {
            builder.append('W');
        } else if (horizontalComponent > 0) {
            builder.append('E');
        }

        return Direction.valueOf(builder.toString());
    }

    private Direction toCenter(Position now) {
        if (now.x < 16 && now.y < 16) {
            return Direction.SE;
        } else if (now.x < 16 && now.y == 16) {
            return Direction.E;
        } else  if (now.x < 16) {
            return  Direction.NE;
        } else  if (now.x == 16 && now.y < 16) {
            return Direction.S;
        } else  if (now.x == 16 && now.y == 16) {
            return null;
        } else  if (now.x == 16) {
            return  Direction.N;
        } else  if (now.y < 16) {
            return  Direction.SW;
        } else  if (now.y == 16) {
            return  Direction.W;
        } else {
            return  Direction.NW;
        }
    }

}
