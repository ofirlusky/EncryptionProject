package utils;

import game.PieceValue;

public class Node
{
    private PieceValue currentPieceValue;
    private int IDofNode;



    public Node(int IDofNode) {
        this.currentPieceValue = PieceValue.EMPTY;
        this.IDofNode = IDofNode;
    }

    public void setIDofNode(int IDofNode) {
        this.IDofNode = IDofNode;
    }


    public PieceValue getCurrentPieceValue() {
        return this.currentPieceValue;
    }

    public void setCurrentPieceValue(PieceValue currentPieceValue) {
        this.currentPieceValue = currentPieceValue;
    }

    public int getIDofNode() {
        return this.IDofNode;
    }


}
