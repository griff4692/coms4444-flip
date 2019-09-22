package flip.g2a;

import flip.sim.Point;
import java.util.Collection;

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

    public void recordOpponentPieces(Collection<Point> opponentPieces) {
        for (Point p : opponentPieces) {
            board[(int) ((p.y + 20) / gridResolution)][(int) ((p.x + 60) / gridResolution)] = -1;
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
            if ((double) count / board.length > 0.2) {
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

}
