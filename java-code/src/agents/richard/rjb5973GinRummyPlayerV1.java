package agents.richard;

import ginrummy.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

/*
 * Strategy for my Gin Rummy Player:
 * 1. Minimize deadwood
 * Only draw from the discard if it will create a meld
 * draw from the deck otherwise
 * get rid of the highest card with the least probability of creating a meld (e.g get rid of the 10 if I have 2 kings and a 10)
 * randomly get rid of highest value card otherwise
 * only knock if you get gin or big gin
 * The goal of this bot is to always win by undercutting
 *
 * Maybe look into counting cards?
 */

/*
 *Strategy option 2
 * The simple Rummy player likes to discard high cards, what if I discard low cards first?
 * only draw from the discard if I complete a meld or add to it
 */


public class rjb5973GinRummyPlayerV1 implements GinRummyPlayer {

    private int playerNum;
    private int startingPlayerNum;
    private ArrayList<Card> cards = new ArrayList<Card>();
    private Random random = new Random();
    private boolean opponentKnocked = false;
    Card faceUpCard, drawnCard;
    ArrayList<Long> drawDiscardBitstrings = new ArrayList<Long>();
    int turnCount;


    @Override
    public void startGame(int playerNum, int startingPlayerNum, Card[] cards) {
        this.playerNum = playerNum;
        this.startingPlayerNum = startingPlayerNum;
        this.cards.clear();
        this.cards.addAll(Arrays.asList(cards));
        GameStateStorage.reset(this.cards.size());
        opponentKnocked = false;
        drawDiscardBitstrings.clear();
        turnCount = 0;
    }

    @Override
    public boolean willDrawFaceUpCard(Card card) {
        // Return true if card would be a part of a meld, false otherwise.
        this.faceUpCard = card;
        @SuppressWarnings("unchecked")
        ArrayList<Card> newCards = (ArrayList<Card>) cards.clone();
        newCards.add(card);
        ArrayList<ArrayList<ArrayList<Card>>> bestMeld = GinRummyUtil.cardsToBestMeldSets(newCards);
        if(bestMeld.size() > 0) {
            for (ArrayList<Card> meld : bestMeld.get(0))
                if (meld.contains(card))
                    return true;
        }
        return false;
    }

    @Override
    public void reportDraw(int playerNum, Card drawnCard) {
        // Ignore other player draws.  Add to cards if playerNum is this player.
        if (playerNum == this.playerNum) {
            cards.add(drawnCard);
            this.drawnCard = drawnCard;
            turnCount++;
        }
        else if(drawnCard != null) { //the other player picked from the discard
            GameStateStorage.addCardToEnemyHand(drawnCard);
        }


        if(drawnCard == null && playerNum != this.playerNum) { //the other player took from the brand new deck
            GameStateStorage.remainingFaceDownCards--;
        }
        else if(drawnCard != faceUpCard) { //we drew from the brand new pile not the discard Note first turn previouslyDiscarded will be null
            GameStateStorage.remainingFaceDownCards--;
        }

    }

    @SuppressWarnings("unchecked")
    @Override
    public Card getDiscard() {
        // Discard a random card (not just drawn face up) leaving minimal deadwood points.
        int minDeadwood = Integer.MAX_VALUE;
        ArrayList<Card> candidateCards = new ArrayList<Card>();
        for (Card card : cards) {
            // Cannot draw and discard face up card.
            if (card == drawnCard && drawnCard == faceUpCard)
                continue;


            ArrayList<Card> remainingCards = (ArrayList<Card>) cards.clone();
            remainingCards.remove(card);
            ArrayList<ArrayList<ArrayList<Card>>> bestMeldSets = GinRummyUtil.cardsToBestMeldSets(remainingCards);
            int deadwood = bestMeldSets.isEmpty() ? GinRummyUtil.getDeadwoodPoints(remainingCards) : GinRummyUtil.getDeadwoodPoints(bestMeldSets.get(0), remainingCards);
            if (deadwood <= minDeadwood) {
                if (deadwood < minDeadwood) {
                    minDeadwood = deadwood;
                    candidateCards.clear();
                }
                candidateCards.add(card);
            }

        }

        for(Card toDiscard : candidateCards) {
            //immediately discard if we can't match the same suit
            if(getOutOfPlayCardsByRank(toDiscard.rank).size() >= 2) {
                return toDiscard;
            }
        }
        ArrayList<Card> dedupedCards = (ArrayList<Card>) candidateCards.clone();
        //drop the candidate card that is not meldable with a single draw
        ArrayList<Card> unMeldableCards = rjb5973GinRummyHelper.unMeldableSingleDrawCards(cards);
        dedupedCards.removeIf(possibleMeld -> !unMeldableCards.contains(possibleMeld));
        if(dedupedCards.size() > 0) {
            candidateCards = dedupedCards;
        }
        Card discard = candidateCards.get(random.nextInt(candidateCards.size()));
        // Prevent future repeat of draw, discard pair.
        ArrayList<Card> drawDiscard = new ArrayList<Card>();
        drawDiscard.add(drawnCard);
        drawDiscard.add(discard);
        drawDiscardBitstrings.add(GinRummyUtil.cardsToBitstring(drawDiscard));
        return discard;
    }

    @Override
    public void reportDiscard(int playerNum, Card discardedCard) {
        // Ignore other player discards.  Remove from cards if playerNum is this player.
        if (playerNum == this.playerNum)
            cards.remove(discardedCard);
        else {
            GameStateStorage.removeCardFromEnemyHand(discardedCard);
        }
    }

    @Override
    public ArrayList<ArrayList<Card>> getFinalMelds() {
        // We only want to knock if we gin or it's early, otherwise we want to undercut
        ArrayList<ArrayList<ArrayList<Card>>> bestMeldSets = GinRummyUtil.cardsToBestMeldSets(cards);
        //knock only on gin or early in the game or if there aren't any cards left in the deck and I can
        if (!opponentKnocked && (bestMeldSets.isEmpty() || GinRummyUtil.getDeadwoodPoints(bestMeldSets.get(0), cards) > 0) && (turnCount > 5 || bestMeldSets.isEmpty() || GinRummyUtil.getDeadwoodPoints(bestMeldSets.get(0), cards) > GinRummyUtil.MAX_DEADWOOD) && (GameStateStorage.remainingFaceDownCards > 2 || bestMeldSets.isEmpty() || GinRummyUtil.getDeadwoodPoints(bestMeldSets.get(0), cards) > GinRummyUtil.MAX_DEADWOOD))
            return null;
        return bestMeldSets.isEmpty() ? new ArrayList<ArrayList<Card>>() : bestMeldSets.get(random.nextInt(bestMeldSets.size()));
    }

    @Override
    public void reportFinalMelds(int playerNum, ArrayList<ArrayList<Card>> melds) {
        // Melds ignored by simple player, but could affect which melds to make for complex player.
        if (playerNum != this.playerNum)
            opponentKnocked = true;
    }

    @Override
    public void reportScores(int[] scores) {
        // Ignored by simple player, but could affect strategy of more complex player.
    }

    @Override
    public void reportLayoff(int playerNum, Card layoffCard, ArrayList<Card> opponentMeld) {
        // Ignored by simple player, but could affect strategy of more complex player.

    }

    @Override
    public void reportFinalHand(int playerNum, ArrayList<Card> hand) {
        // Ignored by simple player, but could affect strategy of more complex player.
    }

    /**
     * gets all out of play cards by rank
     * @param rank the rank of the cards you want to find similar suits to
     * @return an array list containing cards of the same rank that are out of play
     */
    private ArrayList<Card> getOutOfPlayCardsByRank(int rank) {
        ArrayList<Card> sameSuitOutOfPlay = new ArrayList<>();
        //TODO optimize using bitstrings
        for(Card card : GinRummyUtil.bitstringToCards(GameStateStorage.discardedCards | GameStateStorage.otherPlayerCards)) {
            if(card.rank == rank) {
                sameSuitOutOfPlay.add(card);
            }
        }
        return sameSuitOutOfPlay;
    }

    static class GameStateStorage {
        static int remainingFaceDownCards;
        static long otherPlayerCards;
        static long discardedCards;

        static void addCardToEnemyHand(Card card) {
            addCardToEnemyHand(card.getId());
        }

        static void addCardToEnemyHand(int id) {
            otherPlayerCards = otherPlayerCards | (1L << id);
        }

        static void removeCardFromEnemyHand(Card card) {
            removeCardFromDiscard(card.getId());
        }

        static void removeCardFromEnemyHand(int id) {
            otherPlayerCards = otherPlayerCards & ~(1L << id);
        }

        static void addCardToDiscard(Card card) {
            addCardToDiscard(card.getId());
        }

        static void addCardToDiscard(int id) {
            discardedCards = discardedCards | (1L << id);
        }

        static void removeCardFromDiscard(Card card) {
            removeCardFromDiscard(card.getId());
        }

        static void removeCardFromDiscard(int id) {
            discardedCards = discardedCards & ~(1L << id);
        }

        static void reset(int handSize) {
            remainingFaceDownCards = Card.allCards.length - 1 - (2 * handSize);
            otherPlayerCards = 0;
            discardedCards = 0;


        }
    }

}

class rjb5973GinRummyHelper {

    static Random random = new Random();

    public static int getBestDeadwood(ArrayList<Card> myCards) {
        if(GinRummyUtil.cardsToBestMeldSets(myCards).isEmpty()) {
            return GinRummyUtil.getDeadwoodPoints(myCards);
        }
        else {
            return GinRummyUtil.getDeadwoodPoints(GinRummyUtil.cardsToBestMeldSets(myCards).get(0), myCards);
        }
    }

    public static int bestDeadwoodPointsAfterDiscard(ArrayList<Card> myCards) {
        if(myCards.size() > 11) {return -1;}
        int minDeadwood = Integer.MAX_VALUE;
        for(Card c : myCards) {
            myCards.remove(c);
            ArrayList<ArrayList<ArrayList<Card>>> bestMeldSets = GinRummyUtil.cardsToBestMeldSets(myCards);
            minDeadwood = Math.min(minDeadwood, bestMeldSets.isEmpty() ? GinRummyUtil.getDeadwoodPoints(myCards) : GinRummyUtil.getDeadwoodPoints(bestMeldSets.get(0), myCards));
            myCards.add(c);
        }
        return minDeadwood;
    }

    public static ArrayList<Card> bestDeadwoodAfterDiscard(ArrayList<Card> myCards) {
        // Choose a random card to discard that we can't make a meld of
        int minDeadwood = Integer.MAX_VALUE;
        ArrayList<Card> candidateCards = new ArrayList<Card>();
        for (Card card : myCards) {



            ArrayList<Card> remainingCards = (ArrayList<Card>) myCards.clone();
            remainingCards.remove(card);
            ArrayList<ArrayList<ArrayList<Card>>> bestMeldSets = GinRummyUtil.cardsToBestMeldSets(remainingCards);
            int deadwood = bestMeldSets.isEmpty() ? GinRummyUtil.getDeadwoodPoints(remainingCards) : GinRummyUtil.getDeadwoodPoints(bestMeldSets.get(0), remainingCards);
            if (deadwood <= minDeadwood) {
                if (deadwood < minDeadwood) {
                    minDeadwood = deadwood;
                    candidateCards.clear();
                }
                candidateCards.add(card);
            }

        }

        ArrayList<Card> dedupedCards = (ArrayList<Card>) candidateCards.clone();
        //drop the candidate card with the least number of suits in it
        for(Card matchingSuit : candidateCards) {
            for(Card otherSuit : candidateCards) {
                if(matchingSuit.rank == otherSuit.rank) {
                    dedupedCards.remove(matchingSuit);
                    dedupedCards.remove(otherSuit);
                }
            }
        }
        if(dedupedCards.size() > 0) {
            candidateCards = dedupedCards;
        }
        ArrayList<Card> minDeadwoodHand = (ArrayList<Card>) myCards.clone();
        minDeadwoodHand.remove(candidateCards.get(random.nextInt(candidateCards.size())));
        return minDeadwoodHand;
    }

    public static int numCardsThatMakeGin(ArrayList<Card> myCards) {
        int count = 0;
        long remainingDeck = (1L << Card.allCards.length) - 1;
        remainingDeck = remainingDeck & ~(GinRummyUtil.cardsToBitstring(myCards)); //remove my hand from the available cards
        remainingDeck = remainingDeck & ~(rjb5973GinRummyPlayerV1.GameStateStorage.discardedCards);

        ArrayList<Card> restofPile = GinRummyUtil.bitstringToCards(remainingDeck);
        for(Card c : restofPile) {
            myCards.add(c);
            if(bestDeadwoodPointsAfterDiscard(myCards) == 0) {
                count++;
            }
            myCards.remove(c);
        }
        return count;
    }

    public static boolean opponentCanMakeMeld(ArrayList<Card> hand, Card card) {
        long outOfPlayCards = GinRummyUtil.cardsToBitstring(hand) | rjb5973GinRummyPlayerV1.GameStateStorage.discardedCards;
        return !unMeldableSingleDrawCards(GinRummyUtil.bitstringToCards(outOfPlayCards), GinRummyUtil.bitstringToCards(rjb5973GinRummyPlayerV1.GameStateStorage.otherPlayerCards)).contains(card);
    }


    /**
     * Checks to see if your card makes a meld and if it does what the drop in points would be
     * @param myHand your hand
     * @param toMeld the card you want to add to a meld
     * @return the drop in deadwood points, will return a negative number if the meld increases the points, or Integer.MIN_VALUE if the card will not create a meld
     */
    public static int deadwoodDrop(ArrayList<Card> myHand, Card toMeld) {
        int currentDeadwood = GinRummyUtil.cardsToBestMeldSets(myHand).isEmpty() ? GinRummyUtil.getDeadwoodPoints(myHand) : GinRummyUtil.getDeadwoodPoints(GinRummyUtil.cardsToBestMeldSets(myHand).get(0), myHand);
        myHand.add(toMeld);
        ArrayList<ArrayList<ArrayList<Card>>> meldsWithCard = GinRummyUtil.cardsToBestMeldSets(myHand);
        myHand.remove(toMeld);

        for(ArrayList<ArrayList<Card>> meldSet : meldsWithCard) {
            for (ArrayList<Card> meld : meldSet) {
                if(meld.contains(toMeld)) {
                    return currentDeadwood - GinRummyUtil.getDeadwoodPoints(meldSet, myHand);
                }
            }
        }

        return Integer.MIN_VALUE;
    }



    public static ArrayList<Card> unMeldableSingleDrawCards(ArrayList<Card> myCards) {
        long outOfPlayCards = rjb5973GinRummyPlayerV1.GameStateStorage.discardedCards | rjb5973GinRummyPlayerV1.GameStateStorage.otherPlayerCards;

        return unMeldableSingleDrawCards(myCards, GinRummyUtil.bitstringToCards(outOfPlayCards));
    }

    public static ArrayList<Card> unMeldableSingleDrawCards(ArrayList<Card> hand, ArrayList<Card> outOfPlayCards) {
        ArrayList<Card> remainingDeck = GinRummyUtil.bitstringToCards(((1L << Card.allCards.length) - 1) & ~(GinRummyUtil.cardsToBitstring(outOfPlayCards)));
        ArrayList<Card> unMeldableCards = (ArrayList<Card>) hand.clone();
        for(Card c : hand) {
            for(ArrayList<Card> meldSet : GinRummyUtil.cardsToAllMelds(remainingDeck)) {
                    if(meldSet.contains(c)) {
                        unMeldableCards.remove(c);
                    }
            }
        }
        return unMeldableCards;
    }

    public static ArrayList<Card> unMeldableDoubleDrawCards(ArrayList<Card> myCards) {
        //we can treat the discards as the hand and the user's hand as the deck and use the same algorithm for the unMeldableSingleDrawCards
        long outOfPlayCards = rjb5973GinRummyPlayerV1.GameStateStorage.discardedCards | rjb5973GinRummyPlayerV1.GameStateStorage.otherPlayerCards;
        return unMeldableSingleDrawCards(GinRummyUtil.bitstringToCards(outOfPlayCards), myCards);
    }

    public static double averageDeadwood(ArrayList<Card> myCards) {
        long remainingDeck = (1L << Card.allCards.length) - 1;
        remainingDeck = remainingDeck & ~(GinRummyUtil.cardsToBitstring(myCards)); //remove my hand from the available cards
        remainingDeck = remainingDeck & ~(rjb5973GinRummyPlayerV1.GameStateStorage.discardedCards);
        ArrayList<ArrayList<ArrayList<Card>>> handMelds = GinRummyUtil.cardsToBestMeldSets(myCards);
        int handDeadwood = handMelds.isEmpty()? GinRummyUtil.getDeadwoodPoints(myCards) : GinRummyUtil.getDeadwoodPoints(handMelds.get(0), myCards);
        int averageDeadwood = 0;
        for (Card c : GinRummyUtil.bitstringToCards(remainingDeck)) {
            averageDeadwood += GinRummyUtil.getDeadwoodPoints(c);
        }
        averageDeadwood = averageDeadwood / GinRummyUtil.bitstringToCards(remainingDeck).size();

        return handDeadwood + averageDeadwood;
    }
}

