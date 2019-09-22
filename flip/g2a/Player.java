package flip.g2a;

import java.util.List;
import java.util.HashMap;
import javafx.util.Pair;
import java.util.ArrayList;

import flip.sim.Point;
import flip.sim.Board;
import java.util.Comparator;
import java.util.Map.Entry;
import java.util.PriorityQueue;
import java.util.Queue;

public class Player implements flip.sim.Player {

    private final static double WALL_HOLDING_PRIORITY = -1.0;

    private int seed = 42;
    private boolean isPlayer1;
    private double greedyProb;
    private Integer n;
    private Double pieceDiameter;
    private DiscreteBoard dBoard;
    private Queue<Destination> destinations;
    private List<Integer> wallHoldingPieces;

    public Player() {
        wallHoldingPieces = new ArrayList<>();
    }

    public HashMap<Integer, Point> flip(HashMap<Integer, Point> pieces) {
        HashMap<Integer, Point> flippedPieces = new HashMap<>();
        pieces.forEach((key, value) -> flippedPieces.put(key, new Point(-value.x, value.y)));
        return flippedPieces;
    }

    /**
     *
     * @param pieces initial location of the pieces for the player.
     * @param n number of pieces available.
     * @param t total turns available.
     * @param isPlayer1 true if this player is the first to move
     * @param pieceDiameter diameter of each piece
     */
    @Override
    public void init(HashMap<Integer, Point> pieces, int n, double t, boolean isPlayer1, double pieceDiameter) {
        this.n = n;
        this.isPlayer1 = isPlayer1;
        this.pieceDiameter = pieceDiameter;
        this.dBoard = new DiscreteBoard(120, 40, 2);

        if (isPlayer1) {
            pieces = this.flip(pieces);
        }

        setupStrategy(pieces);
    }

    /**
     *
     * @param numMoves number of movements to return.
     * @param playerPieces location of this player's pieces.
     * @param opponentPieces location of the opponent's pieces.
     * @param isPlayer1 true if this player is/was the first one to move.
     * @return list of moves to play
     */
    @Override
    public List<Pair<Integer, Point>> getMoves(
            Integer numMoves,
            HashMap<Integer, Point> playerPieces,
            HashMap<Integer, Point> opponentPieces,
            boolean isPlayer1) {

        if (isPlayer1) {
            playerPieces = this.flip(playerPieces);
            opponentPieces = this.flip(opponentPieces);
        }

        List<Pair<Integer, Point>> moves = new ArrayList<>();

        detectWall(playerPieces, opponentPieces);

        for (int i = 0; i < numMoves; i++) {
            final Destination d = destinations.peek();
            if (wallHoldingPieces.contains(d.id)) {
                destinations.poll();
                i--;
                continue;
            }
            //System.out.println(destinations);
            System.out.println("*** " + d);
            final Pair<Integer, Point> piecePair = new Pair<>(d.id, playerPieces.get(d.id));
            final double theta = this.getBestAngleToMove(piecePair, d.position, playerPieces, opponentPieces);
            final Pair<Integer, Point> move = this.getPositionToMove(piecePair, playerPieces, opponentPieces, theta);
            moves.add(move);
            playerPieces.put(move.getKey(), move.getValue());
            updateQueue(playerPieces);
        }

        if (isPlayer1) {
            final List<Pair<Integer, Point>> reflectedMoves = new ArrayList<>();
            for (Pair<Integer, Point> move : moves) {
                final Pair<Integer, Point> rMove = new Pair<>(move.getKey(), new Point(-move.getValue().x, move.getValue().y));
                reflectedMoves.add(rMove);
            }
            moves = reflectedMoves;
        }
        System.out.println("***" + moves);
        return moves;
    }

    protected void setupStrategy(HashMap<Integer, Point> pieces) {
        final Comparator<Destination> dc = (Destination d1, Destination d2) -> d1.priority.compareTo(d2.priority);
        this.destinations = new PriorityQueue(n, dc);

        // pick a runner
        // pick wall pieces (based on closeness to wall pieces position
        // set target destinations and priorities
        
        HashMap<Integer, Point> cPieces = new HashMap<>(pieces);
        Integer runner = getCloser(new Point(0, 0), cPieces);
        cPieces.remove(runner);
        destinations.add(new Destination(1.0, runner, new Point(0,0)));
        
        final double wallOffset = pieceDiameter / 2 - 1;
        for (int i = 0; i < 12; i++) {
            //final wallPoint = new Point(-19, wallOffset);
        }
        

        for (Entry<Integer, Point> p : pieces.entrySet()) {
            Integer id = p.getKey();
            Point position = p.getValue();
            destinations.add(new Destination(Math.abs(position.x), id, new Point(21, position.y)));
        }
        final Destination dRunner = destinations.poll();
        dRunner.priority = 0.0;
        
        destinations.add(dRunner);
    }
    
    protected Integer getCloser(Point point, HashMap<Integer, Point> pieces) {
        double bestDistance = Double.POSITIVE_INFINITY;
        Integer closerPoint = null;
        for (Entry<Integer, Point> p : pieces.entrySet()) {
            final double d = getDistance(p.getValue(), point);
            if (d < bestDistance) {
                closerPoint = p.getKey();
                bestDistance = d;
            }
        }
        return closerPoint;
    }

    protected void updateQueue(HashMap<Integer, Point> playerPieces) {
        final Destination d = destinations.peek();
        final Point curPos = playerPieces.get(d.id);
        final double distance = Math.hypot(d.position.y - curPos.y, d.position.x - curPos.x);
        //System.out.println("***distance " + distance);
        if (distance < pieceDiameter / 2) {
            destinations.poll();
            if (d.priority == WALL_HOLDING_PRIORITY) {
                wallHoldingPieces.add(d.id);
            }
        }
    }

    protected void detectWall(HashMap<Integer, Point> playerPieces, HashMap<Integer, Point> opponentPieces) {
        try {
            dBoard.reset();
            dBoard.recordOpponentPieces(opponentPieces.values());
            Double crowdedX = dBoard.getCrowdedColumn(-22, 22);
            if (crowdedX != null) {
                //moves.add(this.getPositionToMove(new Pair<>(0, playerPieces.get(0)), playerPieces, opponentPieces, 0));
                //System.out.println("***Crowded " + crowdedX);
                final double cY = dBoard.findAHole(crowdedX);
                //System.out.println("***" + cY);
                Entry<Integer, Point> min = null;
                for (Entry<Integer, Point> p : playerPieces.entrySet()) {
                    if (min == null || Math.abs(crowdedX - p.getValue().x) < Math.abs(crowdedX - min.getValue().x)) {
                        min = p;
                    }
                }
                if (Math.abs(crowdedX - min.getValue().x) > pieceDiameter / 2 && destinations.peek().priority != WALL_HOLDING_PRIORITY) {
                    System.out.println("***Block wall, move " + min.getKey() + " to (" + crowdedX + ", " + cY + ")");
                    destinations.add(new Destination(WALL_HOLDING_PRIORITY, min.getKey(), new Point(crowdedX, cY)));

                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected boolean checkValidity(Pair<Integer, Point> move, HashMap<Integer, Point> player_pieces, HashMap<Integer, Point> opponent_pieces) {
        boolean valid = true;

        // check if move is adjacent to previous position.
        if (!Board.almostEqual(Board.getdist(player_pieces.get(move.getKey()), move.getValue()), pieceDiameter)) {
            System.out.println("No Adjacent");
            return false;
        }
        // check for collisions
        valid = valid && !Board.check_collision(player_pieces, move);
        if (!valid) {
//            System.out.println("Collision with own pieces");
        }
        valid = valid && !Board.check_collision(opponent_pieces, move);
        if (!valid) {
//            System.out.println("Collision with opponent pieces");
        }

        // check within bounds
        valid = valid && Board.check_within_bounds(move);
        return valid;

    }

    /**
     * Return the list of pieces that should be considered when deciding which
     * one to move
     *
     * @param playerPieces
     * @return A filtered list of pieces
     */
    protected HashMap<Integer, Point> filterPiecesToMove(HashMap<Integer, Point> playerPieces) {
        // filter out pieces that are already in the opponents area
        double min, max, eps = 1E-7;
        if (this.isPlayer1) {
            min = -60.0;
            max = -20.0;
        } else {
            min = 20.0;
            max = 60.0;
        }
        HashMap<Integer, Point> piecesToMove = new HashMap<>();
        playerPieces.entrySet().stream().filter((entry)
                -> (entry.getValue().x - this.pieceDiameter / 2 + eps < min || entry.getValue().x + this.pieceDiameter / 2 - eps > max)
        ).forEachOrdered((entry) -> {
            piecesToMove.put(entry.getKey(), entry.getValue());
        });
        return piecesToMove;
    }

    protected void alliedPiecesBehind(HashMap<Integer, Point> playerPieces, ArrayList<ArrayList<Double>> playerInputs) {
        Point[] xSorted = playerPieces.values().stream().sorted((p1, p2) -> Double.compare(p1.x, p2.x)).toArray(Point[]::new);
        for (int i = 0; i < playerPieces.size(); i++) {
            Point p = playerPieces.get(i);
            boolean pieceBehind = false;
            int first = 0;
            int last = playerPieces.size();
            int mid = (first + last) / 2;
            while (first <= last) {
                if (xSorted[mid].x + pieceDiameter < p.x) {
                    first = mid + 1;
                } else if (xSorted[mid] == p || xSorted[mid].x >= p.x) {
                    last = mid - 1;
                } else {
                    // found another piece in the range, next we go left and
                    // right checking if they collide in the y-axis
                    int j = mid;
                    do {
                        if (Math.abs(xSorted[j].y - p.y) <= pieceDiameter) {
                            pieceBehind = true;
                            break;
                        }
                        j++;
                    } while (xSorted[j].x < p.x);
                    j = mid--;
                    while (xSorted[j].x > p.x - pieceDiameter) {
                        if (Math.abs(xSorted[j].y - p.y) <= pieceDiameter) {
                            pieceBehind = true;
                            break;
                        }
                        j++;
                    }
                    break;
                }
                mid = (first + last) / 2;
            }
            playerInputs.get(i).add(pieceBehind ? 1.0 : -1.0);
        }
    }

    protected double getDistanceToTarget(Point point) {
        return Math.max(20 - point.x, 0.0);
    }

    /**
     * Computes the distances from each point to the target line in the board
     *
     * @param playerPieces
     * @return A map with the distance of each point passed
     */
    protected HashMap<Integer, Double> getDistancesToTarget(HashMap<Integer, Point> playerPieces) {
        HashMap<Integer, Double> distanceMap = new HashMap<>();
        playerPieces.forEach((key, value) -> distanceMap.put(key, Math.max(20 - value.x, 0.0)));
        return distanceMap;
    }

    /**
     * Given a piece decides to what position to move it (implicitly the angle)
     *
     * @param piece
     * @param playerPieces
     * @param opponentPieces
     * @param theta
     * @return the position where the piece will be after moving
     */
    protected Pair<Integer, Point> getPositionToMove(
            Pair<Integer, Point> piece, HashMap<Integer, Point> playerPieces, HashMap<Integer, Point> opponentPieces, double theta) {
        double window = 0.0;

        while (window < Math.PI) {
            for (int i = -1; i < 2; i += 2) {
                double theta_adj = theta + i * window;
                Point newPos = getNewPosition(piece.getValue(), theta_adj);
                //System.out.println("Move " + piece.getKey() + " to " + newPos);
                Pair<Integer, Point> newMove = new Pair(piece.getKey(), newPos);
                if (this.checkValidity(newMove, playerPieces, opponentPieces)) {
                    return newMove;
                } else {
//                    System.out.println("No valid");
                }
            }
            window += 0.01;
        }

        return null;
    }

    private double getBestAngleToMove(
            Pair<Integer, Point> piece,
            Point tarPos,
            HashMap<Integer, Point> playerPieces,
            HashMap<Integer, Point> opponentPieces) {
        final Point curPos = piece.getValue();
        final double distance = getDistance(tarPos, curPos);
        final double angle = Math.atan2(tarPos.y - curPos.y, tarPos.x - curPos.x);

        if (Math.abs(pieceDiameter - distance) < 0.01 || distance >= 2 * pieceDiameter) {
            return angle;
        }

        final double oAngle = Math.acos(distance / 2 / pieceDiameter);

        final double theta1 = angle + oAngle;
        final double theta2 = angle - oAngle;

        Point newPos = getNewPosition(curPos, theta1);
        Pair<Integer, Point> move = new Pair<>(piece.getKey(), newPos);

        return checkValidity(move, playerPieces, opponentPieces) ? theta1 : theta2;
    }

    protected Point getNewPosition(Point pos, double angle) {
        final double deltaX = pieceDiameter * Math.cos(angle);
        final double deltaY = pieceDiameter * Math.sin(angle);
        return new Point(pos.x + deltaX, pos.y + deltaY);
    }
    
    protected double getDistance(Point p1, Point p2) {
        return Math.hypot(p2.y - p1.y, p2.x - p1.x);
    }
}

class Destination {

    Double priority;
    Integer id;
    Point position;

    public Destination(Double priority, Integer id, Point position) {
        this.priority = priority;
        this.id = id;
        this.position = position;
    }

    @Override
    public String toString() {
        return "Destination{" + "priority=" + priority + ", id=" + id + ", position=" + position + '}';
    }

}