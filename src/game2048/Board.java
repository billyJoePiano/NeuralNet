package game2048;

import java.io.*;
import java.util.concurrent.*;

public class Board {
    private static final int[] POW2 = new int[]
            { 0, 2, 4, 8, 16, 32, 64, 128, 256, 512, 1024, 2048, 4096, 8192, 16384, 32768, 65536, 131072 };

    private final byte[][] tiles = new byte[4][4];
    private boolean active = true;

    public static void main (String args[]) {
        Board board = new Board();

        System.out.println(board);
        boolean moved = true;

        while (board.isActive()) {
            try {
                switch(System.in.read()) {
                    case 115: moved = board.up(); break;
                    case 122: moved = board.left(); break;
                    case 120: moved = board.down(); break;
                    case 99:  moved = board.right(); break;
                    default: continue;
                }

            } catch (IOException e) {
                System.err.println(e.getMessage());
            }

            if (moved) System.out.println(board);
        }
    }

    public Board() {
        addTile((byte)16);
        addTile((byte)15);
    }


    public byte[][] getTiles() {
        return this.tiles.clone();
    }

    public long getScore() {
        long score = 0;
        for (byte[] row : this.tiles) {
            for (byte tile : row) {
                score += POW2[tile];
            }
        }

        return score;
    }

    public boolean isActive() {
        return this.active;
    }

    public void reset() {
        this.active = true;
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                this.tiles[i][j] = 0;
            }
        }
    }

    public String toString() {
        char[] chars = new char[88];
        char[] next = new char[2];

        int index = 1;
        chars[0] = '\n';

        for (int i = 0; i < 4; i++) {
            for (int j = 0; true; j++) {
                if (tiles[i][j] == 0) {
                next[0] = next[1] = '-';

                } else if (tiles[i][j] > 10) {
                    next[0] = '1';
                    next[1] = (char)(tiles[i][j] + 38);

                } else {
                    next[0] = ' ';
                    next[1] = (char)(tiles[i][j] + 48);
                }

                chars[index++] = next[0];
                chars[index++] = next[1];

                if (j == 3) {
                    chars[index++] = '\n';
                    if (i != 3) chars[index++] = '\n';
                    break;

                } else {
                    chars[index++] = ' ';
                    chars[index++] = ' ';
                    chars[index++] = ' ';
                    chars[index++] = ' ';
                }
             }
         }

        return String.copyValueOf(chars);
    }

    private boolean checkLost() {
        // assumes there are no open tiles
        return     tiles[0][0] != tiles[1][0]
                && tiles[0][0] != tiles[0][1]
                && tiles[0][1] != tiles[1][1]
                && tiles[0][1] != tiles[0][2]
                && tiles[0][2] != tiles[1][2]
                && tiles[0][2] != tiles[0][3]
                && tiles[0][3] != tiles[1][3]

                && tiles[1][0] != tiles[2][0]
                && tiles[1][0] != tiles[1][1]
                && tiles[1][1] != tiles[2][1]
                && tiles[1][1] != tiles[1][2]
                && tiles[1][2] != tiles[2][2]
                && tiles[1][2] != tiles[1][3]
                && tiles[1][3] != tiles[2][3]

                && tiles[2][0] != tiles[3][0]
                && tiles[2][0] != tiles[2][1]
                && tiles[2][1] != tiles[3][1]
                && tiles[2][1] != tiles[2][2]
                && tiles[2][2] != tiles[3][2]
                && tiles[2][2] != tiles[2][3]
                && tiles[2][3] != tiles[3][3]

                && tiles[3][0] != tiles[3][1]
                && tiles[3][1] != tiles[3][2]
                && tiles[3][2] != tiles[3][3];

    }

    public boolean up() {
        byte numberOpen = 0;
        boolean moved = false;

        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                if (this.tiles[i][j] == 0) {
                    boolean empty = true;

                    for (int o = i + 1; o < 4; o++) {
                        if (this.tiles[o][j] != 0) {
                            this.tiles[i][j] = this.tiles[o][j];
                            this.tiles[o][j] = 0;
                            empty = false;
                            moved = true;
                            break;
                        }
                    }

                    if (empty)  {
                        numberOpen++;
                        continue;
                    }
                }

                for (int o = i + 1; o < 4; o++) {
                    if (this.tiles[o][j] == this.tiles[i][j]) {
                         this.tiles[i][j]++;
                        this.tiles[o][j] = 0;
                        moved = true;
                        break;

                    } else if (this.tiles[o][j] != 0) {
                        break;
                    }
                }
            }
        }

        if (numberOpen == ((byte)0) && checkLost()) {
            this.active = false;

        } else if (moved) {
            addTile(numberOpen);
        }

        return moved;
    }

    public boolean down() {
        byte numberOpen = 0;
        boolean moved = false;

        for (int i = 3; i >= 0; i--) {
            for (int j = 0; j < 4; j++) {
                if (this.tiles[i][j] == 0) {
                    boolean empty = true;

                    for (int o = i - 1; o >= 0; o--) {
                        if (this.tiles[o][j] != 0) {
                            this.tiles[i][j] = this.tiles[o][j];
                            this.tiles[o][j] = 0;
                            empty = false;
                            moved = true;
                            break;
                        }
                    }

                    if (empty)  {
                        numberOpen++;
                        continue;
                    }
                }

                for (int o = i - 1; o >= 0; o--) {
                    if (this.tiles[o][j] == this.tiles[i][j]) {
                        this.tiles[i][j]++;
                        this.tiles[o][j] = 0;
                        moved = true;
                        break;

                    } else if (this.tiles[o][j] != 0) {
                        break;
                    }
                }
            }
        }

        if (numberOpen == ((byte)0) && checkLost()) {
            this.active = true;

        } else if (moved) {
            addTile(numberOpen);
        }

        return moved;
    }

    public boolean left() {
        byte numberOpen = 0;
        boolean moved = false;

        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                if (this.tiles[j][i] == 0) {
                    boolean empty = true;

                    for (int o = i + 1; o < 4; o++) {
                        if (this.tiles[j][o] != 0) {
                            this.tiles[j][i] = this.tiles[j][o];
                            this.tiles[j][o] = 0;
                            empty = false;
                            moved = true;
                            break;
                        }
                    }

                    if (empty)  {
                        numberOpen++;
                        continue;
                    }
                }

                for (int o = i + 1; o < 4; o++) {
                    if (this.tiles[j][o] == this.tiles[j][i]) {
                        this.tiles[j][i]++;
                        this.tiles[j][o] = 0;
                        moved = true;
                        break;

                    } else if (this.tiles[j][o] != 0) {
                        break;
                    }
                }
            }
        }

        if (numberOpen == ((byte)0) && checkLost()) {
            this.active = true;

        } else if(moved) {
            addTile(numberOpen);
        }

        return moved;
    }

    public boolean right() {
        byte numberOpen = 0;
        boolean moved = false;

        for (int i = 3; i >=0; i--) {
            for (int j = 0; j < 4; j++) {
                if (this.tiles[j][i] == 0) {
                    boolean empty = true;

                    for (int o = i - 1; o >= 0; o--) {
                        if (this.tiles[j][o] != 0) {
                            this.tiles[j][i] = this.tiles[j][o];
                            this.tiles[j][o] = 0;
                            empty = false;
                            moved = true;
                            break;
                        }
                    }

                    if (empty)  {
                        numberOpen++;
                        continue;
                    }
                }

                for (int o = i - 1; o >= 0; o--) {
                    if (this.tiles[j][o] == this.tiles[j][i]) {
                        this.tiles[j][i]++;
                        this.tiles[j][o] = 0;
                        moved = true;
                        break;

                    } else if (this.tiles[j][o] != 0) {
                        break;
                    }
                }
            }
        }

        if (numberOpen == ((byte)0) && checkLost()) {
            this.active = true;

        } else if (moved) {
            addTile(numberOpen);
        }

        return moved;
    }

    private void addTile(byte numberOpen) {
        ThreadLocalRandom tlr = ThreadLocalRandom.current();
        int tileNum = tlr.nextInt(numberOpen);


        for (byte[] row : this.tiles) {
            for (int i = 0; i < 4; i++) {
                if (row[i] != 0) continue;
                else if (tileNum-- == 0) {
                    row[i] = (byte)((tlr.nextInt(10) < 9) ? 1 : 2);
                    return;
                }
            }
        }

        throw new IllegalStateException();
    }

    public byte getTile(int row, int col) throws ArrayIndexOutOfBoundsException {
        return this.tiles[row][col];
    }
}
