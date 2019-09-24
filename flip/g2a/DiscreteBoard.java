package flip.g2a;

import flip.sim.Point;
import javafx.util.Pair;

import java.lang.reflect.Array;
import java.util.*;

/**
 *
 * @author juand.correa
 */
public class DiscreteBoard {

    int width;
    int height;
    int gridResolution;
    Integer[][] board;

    public DiscreteBoard(int width, int height, int gridResolution) {
        this.width = width;
        this.height = height;
        this.gridResolution = gridResolution;
        board = new Integer[height / gridResolution][width / gridResolution];
    }

    public void reset() {
        for (Integer[] board1 : board) {
            for (int j = 0; j < board1.length; j++) {
                board1[j] = null;
            }
        }
    }

    public boolean isFree(Pair<Integer, Integer> target, boolean[][] visited) {
        int yCoord = target.getKey();
        int xCoord = target.getValue();
        boolean inBounds = xCoord >= 0 && xCoord < visited[0].length && yCoord >= 0 && yCoord < visited.length;

        if(! inBounds) {
            return false;
        }

        boolean notVisited = ! visited[yCoord][xCoord];
        boolean isFree = this.board[yCoord][xCoord] == null || this.board[yCoord][xCoord] > -1;
        return inBounds && notVisited && isFree;
    }

    public ArrayList<Point> findClosestPiece(Pair<Integer, Integer>target) {
        Queue<Pair> q = new LinkedList<>();
        q.add(target);
        String[][] directions = new String[height / gridResolution][width / gridResolution];
        boolean[][] visited = new boolean[height / gridResolution][width / gridResolution];
        for (boolean[] visited1 : visited) {
            for (int j = 0; j < visited1.length; j++) {
                visited1[j] = false;
            }
        }
        for (String[] directions1 : directions) {
            for (int j = 0; j < directions1.length; j++) {
                directions1[j] = "";
            }
        }
        visited[target.getKey()][target.getValue()] = true;

        ArrayList<Point>finalPath = new ArrayList<Point>();

        while(! q.isEmpty()) {
            Pair<Integer, Integer> p = q.remove();
            int yCoord = p.getKey();
            int xCoord = p.getValue();

            int boardValue = this.board[yCoord][xCoord] == null ? -1 : this.board[yCoord][xCoord];
            if(boardValue >= 0 && target != p) {
                finalPath.add(new Point(boardValue, boardValue)); // this is just the piece idx in both k, v

                int yPath = yCoord;
                int xPath = xCoord;

                while(directions[yPath][xPath].length() > 0) {
                    String nextDirection = directions[yPath][xPath];
                    if(nextDirection == "up") {
                        yPath --;
                    } else if (nextDirection == "right") {
                        xPath++;
                    } else if(nextDirection == "down") {
                        yPath ++;
                    }

                    finalPath.add(this.getRealBoardCoords(new Point(yPath, xPath)));
                }

                return finalPath;
            } else {
                Pair<Integer, Integer> up = new Pair(yCoord - 1, xCoord);
                if(this.isFree(up, visited)) {
                    visited[up.getKey()][up.getValue()] = true;
                    q.add(up);
                    directions[up.getKey()][up.getValue()] = "down";
                }

                Pair<Integer, Integer> left = new Pair(yCoord, xCoord - 1);
                if(this.isFree(left, visited)) {
                    visited[left.getKey()][left.getValue()] = true;
                    q.add(left);
                    directions[left.getKey()][left.getValue()] = "right";
                }

                Pair<Integer, Integer> down = new Pair(yCoord + 1, xCoord);
                if(this.isFree(down, visited)) {
                    visited[down.getKey()][down.getValue()] = true;
                    q.add(down);
                    directions[down.getKey()][down.getValue()] = "up";
                }
            }
        }

        return finalPath;
    }

    public Point getRealBoardCoords(Point p) {
        return new Point((double) ((p.y * gridResolution) - 20.), (double) ((p.x * gridResolution) - 60.0));
    }

    public Pair<Integer, Integer> getDiscreteBoardCoords(Point p) {
        return new Pair((int) ((p.y + 20) / gridResolution), (int) ((p.x + 60) / gridResolution));
    }

    public void recordOpponentPieces(Collection<Point> opponentPieces) {
        for (Point p : opponentPieces) {
            Pair<Integer, Integer> discreteCoords = this.getDiscreteBoardCoords(p);
            board[discreteCoords.getKey()][discreteCoords.getValue()] = -1;
        }
    }

    public void recordPlayerPieces(Collection<Point> playerPieces) {
        for (int i = 0; i < playerPieces.size(); i ++) {
            Point p = (Point) playerPieces.toArray()[i];
            Pair<Integer, Integer> discreteCoords = this.getDiscreteBoardCoords(p);
            board[discreteCoords.getKey()][discreteCoords.getValue()] = 1;
        }
    }

    public void recordPlayerPiecesIdx(Collection<Point> playerPieces) {
        for (int i = 0; i < playerPieces.size(); i ++) {
            Point p = (Point) playerPieces.toArray()[i];
            Pair<Integer, Integer> discreteCoords = this.getDiscreteBoardCoords(p);
            board[discreteCoords.getKey()][discreteCoords.getValue()] = i;
        }
    }
    
    public Double getCrowdedColumn() {
        return getCrowdedColumn(0, width);
    }

    public Double getCrowdedColumn(double minX, double maxX) {
        final int minXR = (int)((minX + width / 2) / gridResolution);
        final int maxXR = (int)((maxX + width / 2) / gridResolution);
        for (int j = minXR; j < maxXR; j++) {
            int count = 0;
            for (int i = 0; i < board.length; i++) {
                if (board[i][j] != null) {
                    count++;
                }
            }
            if ((double) count / board.length > 0.1) {
                return (double) j * gridResolution - width / 2;
            }
        }
        return null;
    }

    public Double findAHole(double x) {
        final int col = (int) ((x + width / 2) / gridResolution);
        int d = 1;
        int blanks;
        Integer bestRow = null;
        int bestRowBlanks = 0;
        for (int i = 0; i < board.length; i++) {
            blanks = 0;
            for (int j = Math.max(i - d, 0); j < Math.min(i + d, board.length); j++) {
                for (int k = Math.max(col - d, 0); k < Math.min(col + d, board[i].length); k++) {
                    if (board[j][k] == null) {
                        blanks++;
                    }
                }
            }
            if (bestRowBlanks < blanks || Math.abs(i - board.length / 2) < Math.abs(bestRow - board.length / 2)) {
                bestRow = i;
                bestRowBlanks = blanks;
            }
        }
        return (bestRow == null) ? null : bestRow.doubleValue() * gridResolution - height / 2;
    }

    public int findClosestSumDistances(int lowY, int highY, int x) {
        boolean[][] used = new boolean[board.length][board[0].length];

        for(int i = 0; i < board.length; i++) {
            for(int j = 0; j < board[0].length; j++) {
                used[i][j] = false;
            }
        }

        int totalDistance = 0;
        for(int wallY = lowY; wallY < highY; wallY++) {
            int closestX = -1;
            int closestY = -1;
            int closestDistance = 999;
            for(int row = 0; row < board.length; row++) {
                for (int col = x + 1; col < board[0].length; col++) {
                    if(board[row][col] != null && board[row][col] == -1 && !used[row][col]) {
                        int distance = Math.abs(wallY - row) + Math.abs(x - col);
                        if(distance <= closestDistance) {
                            closestX = col;
                            closestY = row;
                            closestDistance = distance;
                        }
                    }
                }
            }
            used[closestY][closestX] = true;
            totalDistance += closestDistance;
        }

        return totalDistance;
    }

    public double findBestHole(double x, double runnerY, double runnerX) {
        int runnerYDiscrete = (int) ((runnerY + height / 2) / gridResolution);
        int runnerXDiscrete = (int) ((runnerX + width / 2) / gridResolution);
        final int wallCol = (int) ((x + width / 2) / gridResolution);

        double bestY = -1.0;
        int bestDeltaAdvantage = -9999;

        int currTop = -1;
        for (int i = 0; i < board.length; i++) {
            if (board[i][wallCol] != null) {
                if(currTop > -1) {
                    int opponentDistances = findClosestSumDistances(currTop, i, wallCol);
                    int wallMid = (int) ((i - 1 + currTop) / 2.0);
                    int myDistance = Math.abs(runnerXDiscrete - wallCol) + Math.abs(runnerYDiscrete - wallMid);
                    int delta = opponentDistances - myDistance;
                    if(delta >= bestDeltaAdvantage) {
                        bestY = (double) wallMid * gridResolution - 20.;
                        bestDeltaAdvantage = delta;
                    }
                }
                currTop = -1;
            } else {
                currTop = currTop == -1 ? i : currTop;
            }
        }

        if(currTop > -1) {
            int opponentDistances = findClosestSumDistances(currTop, board.length, wallCol);
            int wallMid = (int) ((board.length - 1 + currTop) / 2.0);
            int myDistance = Math.abs(runnerXDiscrete - wallCol) + Math.abs(runnerYDiscrete - wallMid);
            int delta = opponentDistances - myDistance;
            if(delta >= bestDeltaAdvantage) {
                bestY = (double) wallMid * gridResolution - 20.;
            }
        }

        return bestY;
    }

}
