package agents.jacob;

import ginrummy.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

public class Jag6248GinRummyPlayerV3 implements GinRummyPlayer {
    private int playerNum;
    @SuppressWarnings("unused")
    private int startingPlayerNum;
    private int roundCounter = 0;
    private ArrayList<Card> cards = new ArrayList<Card>();
    private Random random = new Random();
    private boolean opponentKnocked = false;
    private boolean canGin;
    Card faceUpCard, drawnCard;
    ArrayList<Long> drawDiscardBitstrings = new ArrayList<Long>();
    JacobCardCounterV3 cc;

    @Override
    public void startGame(int playerNum, int startingPlayerNum, Card[] cards) {
        this.playerNum = playerNum;
        this.startingPlayerNum = startingPlayerNum;
        this.cards.clear();
        this.cards.addAll(Arrays.asList(cards));
        opponentKnocked = false;
        drawDiscardBitstrings.clear();

        cc = new JacobCardCounterV3(GinRummyUtil.cardsToBitstring(this.cards));
        canGin = true;
    }

    @Override
    public boolean willDrawFaceUpCard(Card card) {
        this.faceUpCard = card;
        int meldCounter = 0;
        @SuppressWarnings("unchecked")
        ArrayList<Card> newCards = (ArrayList<Card>) cards.clone();
        newCards.add(card);
        cc.markCard(card.getId());


        if (GinHelper.canMeld(this.cc.getOpponentHand(), card)) {
            return true;
        }
        else {
            for (ArrayList<Card> meld : GinRummyUtil.cardsToAllMelds(newCards)) {
                if (meld.contains(card))
                    meldCounter++;
            }

            newCards.remove(card);

            // Check how many cards that may be in the deck
            ArrayList<Integer> countedPotentialMelds = new ArrayList<>();
            int tempCount;
            ArrayList<Card> deck = GinRummyUtil.bitstringToCards(cc.getInvertedDeck());
            for (Card dCard : deck) {
                tempCount = 0;
                newCards.add(dCard);
                for (ArrayList<Card> meld : GinRummyUtil.cardsToAllMelds(newCards)) {
                    if (meld.contains(dCard)) {
                        tempCount++;
                    }
                }
                countedPotentialMelds.add(tempCount);
                newCards.remove(dCard);
            }
            // This will need to be adjusted
            int betterThanCurrentCount = 0;
            double oddsOfBetter = 0.0;

            for (int count : countedPotentialMelds) {
                if (count >= meldCounter) {
                    betterThanCurrentCount++;
                }
            }

            oddsOfBetter = (1.0 * betterThanCurrentCount) / cc.numberCardsInDeck();

            return oddsOfBetter < 1.0;
        }
    }

    @Override
    public void reportDraw(int playerNum, Card drawnCard) {
        roundCounter++;
        if (drawnCard == null) {
            cc.incrementUnknownDraw();
            return;
        }
        if (cc.deckContainsCard(drawnCard.getId())) {
            cc.markCard(drawnCard.getId());
        }

        if (playerNum == this.playerNum) {
            cards.add(drawnCard);
            this.drawnCard = drawnCard;
        }
        else {
            cc.markOpponentHand(drawnCard.getId());
        }
    }

    @Override
    public Card getDiscard() {
        // Discard a random card (not just drawn face up) leaving minimal deadwood points.
        int minDeadwood = Integer.MAX_VALUE;
        ArrayList<Card> candidateCards = new ArrayList<Card>();
        ArrayList<Card> candidateCardsOppCantMeld = new ArrayList<>();
        for (Card card : cards) {
            // Cannot draw and discard face up card.
            if (card == drawnCard && drawnCard == faceUpCard)
                continue;
            // Disallow repeat of draw and discard.
            ArrayList<Card> drawDiscard = new ArrayList<Card>();
            //drawDiscard.add(drawnCard);
           // drawDiscard.add(card);
            if (drawDiscardBitstrings.contains(GinRummyUtil.cardsToBitstring(drawDiscard)))
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

            if (!GinHelper.canMeld(cc.getOpponentHand(), card)){
                candidateCardsOppCantMeld.add(card);
            }
        }

        Card discard = null;
        ArrayList<Card> cloneHand = (ArrayList<Card>) this.cards.clone();
        if (candidateCardsOppCantMeld.isEmpty()) {
            ArrayList<Integer> potentialMelds = new ArrayList<>();

            for (Card card : candidateCards) {
                int potentialMeldCount = 0;
                cloneHand.add(card);

                for (Card dCard : GinRummyUtil.bitstringToCards(cc.getInvertedDeck())) {
                    if (cc.deckContainsCard(dCard.getId())) {
                        cloneHand.add(dCard);
                        for (ArrayList<Card> meld : GinRummyUtil.cardsToAllMelds(cloneHand)) {
                            if (checkMeld(meld, card, dCard)) {
                                potentialMeldCount++;
                            }
                        }
                        cloneHand.remove(dCard);
                    }
                }

                cloneHand.remove(card);
                potentialMelds.add(potentialMeldCount);
            }

            int minMeldIndex = 0;
            int leastPotentialMeldCount = Integer.MAX_VALUE;
            for (int i = 0; i < candidateCards.size(); i++) {
                if (leastPotentialMeldCount > potentialMelds.get(i)) {
                    minMeldIndex = i;
                    leastPotentialMeldCount = potentialMelds.get(i);
                }
            }
        }
        else {
            for (Card card : candidateCardsOppCantMeld) {
                if (GinHelper.cardToDiscardForBestDeadwood(cloneHand) == card.getId()) {
                    discard = card;
                }
            }
            if (discard == null) {
                discard = candidateCardsOppCantMeld.get(random.nextInt(candidateCardsOppCantMeld.size()));
            }
        }


        // Prevent future repeat of draw, discard pair.
        ArrayList<Card> drawDiscard = new ArrayList<Card>();
        drawDiscard.add(drawnCard);
        drawDiscard.add(discard);
        drawDiscardBitstrings.add(GinRummyUtil.cardsToBitstring(drawDiscard));
        return discard;
    }

    @Override
    public void reportDiscard(int playerNum, Card discardedCard) {
        if (playerNum == this.playerNum)
            cards.remove(discardedCard);
        else {
            cc.markOpponentDiscard(discardedCard.getId());
            cc.unmarkOpponentHand(discardedCard.getId());
        }
    }

    @Override
    public ArrayList<ArrayList<Card>> getFinalMelds() {
        ArrayList<ArrayList<ArrayList<Card>>> bestMeldSets = GinRummyUtil.cardsToBestMeldSets(cards);
        // Don't Knock if the opponent hasn't and you don't have anything good
        if (!opponentKnocked && (bestMeldSets.isEmpty() || GinRummyUtil.getDeadwoodPoints(bestMeldSets.get(0), cards) > GinRummyUtil.MAX_DEADWOOD))
            return null;

        // Opponent Knocked.. Show what you have
        if (opponentKnocked) {
            roundCounter = 0;
            return bestMeldSets.isEmpty() ? new ArrayList<ArrayList<Card>>() : bestMeldSets.get(random.nextInt(bestMeldSets.size()));
        }

        // ONLY KNOCK IF YOU HAVE GIN: New Strat from CFR
        ArrayList<ArrayList<Card>> melds = GinRummyUtil.cardsToAllMelds(this.cards);
        if (GinRummyUtil.getDeadwoodPoints(melds, this.cards) <= 0) {
            roundCounter = 0;
            return bestMeldSets.get(random.nextInt(bestMeldSets.size()));
        }
        else if (this.roundCounter <= 5 && GinRummyUtil.getDeadwoodPoints(melds, this.cards) < GinRummyUtil.MAX_DEADWOOD) {
            return bestMeldSets.get(random.nextInt(bestMeldSets.size()));
        }

        return null;
    }

    @Override
    public void reportFinalMelds(int playerNum, ArrayList<ArrayList<Card>> melds) {
        if (playerNum != this.playerNum)
            opponentKnocked = true;
    }

    @Override
    public void reportScores(int[] scores) {

    }

    @Override
    public void reportLayoff(int playerNum, Card layoffCard, ArrayList<Card> opponentMeld) {

    }

    @Override
    public void reportFinalHand(int playerNum, ArrayList<Card> hand) {

    }

    public boolean insideStraight(ArrayList<Card> meld, Card countedCard) {
        if (meld.get(0).getSuit() == meld.get(1).getSuit()) {
            return false;
        }
        int endIndx = meld.size() - 1;

        boolean outsideRight = (meld.get(endIndx).getRank() == countedCard.getRank()) &&
                (meld.get(endIndx).getSuit() == countedCard.getSuit());

        boolean outsideLeft = (meld.get(0).getRank() == countedCard.getRank()) &&
                (meld.get(0).getSuit() == countedCard.getSuit());

        return !(outsideLeft || outsideRight);
    }

    public boolean checkMeld(ArrayList<Card> meld, Card card) {
        for (Card mCard : meld) {
            if (mCard.getSuit() == card.getSuit() && mCard.getRank() == card.getRank()) {
                return true;
            }
        }
        return false;
    }

    public boolean checkMeld(ArrayList<Card> meld, Card card, Card counted) {
        boolean countedFound = false;
        boolean drawFound = false;
        for (Card mCard : meld) {
            if (mCard.getSuit() == card.getSuit() && mCard.getRank() == card.getRank()) {
                drawFound = true;
            }
            if (mCard.getSuit() == counted.getSuit() && mCard.getRank() == counted.getRank()) {
                countedFound = true;
            }
        }

        return countedFound && drawFound;
    }


    public static class GinHelper {
        private static int keringhan(long hand) {
            long temp = hand;
            int count = 0;

            while (temp != 0) {
                temp &= (temp - 1);
                count++;
            }

            return count;
        }

        public static int getBestDeadwood(long hand) {
            if (keringhan(hand) < 10) {
                throw new IllegalArgumentException("Not Enough Cards In Hand! (need 10)");
            }
            else if (keringhan(hand) > 10) {
                throw new IllegalArgumentException("HEY TOO MANY CARDS! Get that card out of your shoe! No Cheating (need 10 cards)");
            }

            ArrayList<ArrayList<ArrayList<Card>>> bestMeldConfigs =
                    GinRummyUtil.cardsToBestMeldSets(GinRummyUtil.bitstringToCards(hand));

            return (bestMeldConfigs.isEmpty() ?
                    GinRummyUtil.getDeadwoodPoints(GinRummyUtil.bitstringToCards(hand)) :
                    GinRummyUtil.getDeadwoodPoints(bestMeldConfigs.get(0), GinRummyUtil.bitstringToCards(hand)));
        }

        public static int bestDeadwoodForDiscard(long hand) {
            if (keringhan(hand) < 11) {
                throw new IllegalArgumentException("You don't have enough to discard! (need 11)");
            }
            else if (keringhan(hand) > 11) {
                throw new IllegalArgumentException("HEY TOO MANY CARDS! Get that card out of your shoe! No Cheating (need 11 cards)");
            }

            long dummyHand = hand;
            int card;
            int minDeadwood = Integer.MAX_VALUE;
            ArrayList<Card> alHand = GinRummyUtil.bitstringToCards(hand);
            for (int i = 0; i < 11; i++) {
                card = alHand.get(i).getId();
                dummyHand ^= 1L << card;

                minDeadwood = Integer.min(minDeadwood,
                        GinHelper.getBestDeadwood(dummyHand));
                dummyHand |= 1L << card;
            }

            return minDeadwood;
        }

        public static int cardToDiscardForBestDeadwood(ArrayList<Card> hand) {
            int bestDeadwood = GinHelper.bestDeadwoodForDiscard(GinRummyUtil.cardsToBitstring(hand));
            long bitHand = GinRummyUtil.cardsToBitstring(hand);

            for (Card card : hand) {
                bitHand ^= 1L << card.getId();
                if (GinHelper.getBestDeadwood(bitHand) == bestDeadwood) {
                    return card.getId();
                }
                bitHand |= 1L << card.getId();
            }
            System.out.println("Something is broke");
            return 1;
        }

        //TODO: Make a less bad name
        public static int remainCardsThatCanGin(ArrayList<Card> hand, ArrayList<Card> deck) {
            long handBits = GinRummyUtil.cardsToBitstring(hand);
            int count = 0;

            for (Card deckCard : deck) {
                handBits |= 1L <<  deckCard.getId();
                if (bestDeadwoodForDiscard(handBits) == 0) {
                    count++;
                }
                handBits ^= 1L << deckCard.getId();
            }
            return count;
        }

        public static int bestDeadwoodDrop(ArrayList<Card> hand, int pickupId) {
            long handBits = GinRummyUtil.cardsToBitstring(hand);
            int currentDeadwood = Integer.MAX_VALUE;

            for (ArrayList<ArrayList<Card>> melds : GinRummyUtil.cardsToBestMeldSets(hand)) {
                currentDeadwood = Math.min(GinRummyUtil.getDeadwoodPoints(melds, hand), currentDeadwood);
            }

            handBits |= (1L << pickupId);

            int afterDiscardDeadwood = Integer.MAX_VALUE;
            for (Card card : GinRummyUtil.bitstringToCards(handBits)) {
                handBits ^= (1L << card.getId());

                for (ArrayList<ArrayList<Card>> melds : GinRummyUtil.cardsToBestMeldSets(hand)) {
                    afterDiscardDeadwood = Math.min(
                            GinRummyUtil.getDeadwoodPoints(melds, GinRummyUtil.bitstringToCards(handBits)),
                            afterDiscardDeadwood);
                }

                handBits |= (1L << card.getId());
            }

            return currentDeadwood - afterDiscardDeadwood;
        }

        // This is the "Determine if an opponent can meld" function ... I just use it for my player too
        public static boolean canMeld(long hand, Card card) {
            /*
                    Suit * 13 + rank.. ie

                    Queen Clubs = 11
                        (11 % 13) = 11 (Queen)
                        (11 / 13) = 0 (Clubs)
                    King Clubs = 12

                    Queen Hearts = 24
                        (24 % 13) = 11 (Queen)
                        (24 / 13) = 1 (Hearts)
                    King Hearts = 25
             */
            boolean canMakeSet;
            boolean canMakeRun;
            long cardSuit = (card.getId() / 13);
            long cardRank = (card.getId() % 13);

            if (cardRank == 0) {
                canMakeRun = (hand & (1L << card.getId() + 1)) != 0 &&
                        (hand & (1L << card.getId() + 2)) != 0;
            }
            else if (cardRank== 12) {
                canMakeRun = (hand & (1L << card.getId() - 1)) != 0 &&
                        (hand & (1L << card.getId() - 2)) != 0;
            }
            else {
                canMakeRun = (hand & (1L << card.getId() + 1)) != 0 &&
                        (hand & (1L << card.getId() - 1)) != 0;

                if ((cardRank != 11 && cardRank != 1) && !canMakeRun) {
                    canMakeRun = ((hand & (1L << card.getId() - 1)) != 0 &&
                            (hand & (1L << card.getId() - 2)) != 0) ||
                            ((hand & (1L << card.getId() + 1)) != 0 &&
                            (hand & (1L << card.getId() + 2)) != 0);

                }
            }

            int matchingRankCount = 0;
            for (int i = 0; i < 4; i++) {
                if (i != cardSuit) {
                    matchingRankCount += ((hand & (1L << (cardRank * i)) )!= 0 ? 1 : 0);
                }
            }

            canMakeSet = (matchingRankCount >= 2);

            return (canMakeRun || canMakeSet);
        }

        public static long cardsCannotMeldAfterOneDraw(long hand, long deck) {
            ArrayList<Card> deckList = GinRummyUtil.bitstringToCards(deck);
            long ableToMeldList = 0;
            for (Card card : deckList) {
                hand |= (1L << card.getId());

                for (long meld : GinRummyUtil.cardsToAllMeldBitstrings(GinRummyUtil.bitstringToCards(hand))) {
                    if ((meld & (1L << card.getId())) == 1) {
                        ableToMeldList |= (1L << card.getId());
                    }
                }

                hand ^= (1L << card.getId());
            }

            return ableToMeldList ^ hand;
        }

        public static long cardsCannotMeldAfterTwoDraw(long hand, long deck) {
            long tempHand = hand;
            ArrayList<ArrayList<Card>> allMelds = GinRummyUtil.cardsToAllMelds(GinRummyUtil.bitstringToCards(hand));

            // Mark all cards not in melds
            for (ArrayList<Card> meld : allMelds) {
                for (Card card : meld) {
                    if ((hand & (1L << card.getId())) != 0 && (tempHand & (1L << card.getId())) != 0) {
                        tempHand ^= (1L << card.getId());
                    }
                }
            }

            ArrayList<Card> remainingCards = GinRummyUtil.bitstringToCards(tempHand);
            int cardRank;
            boolean canMakeRun;
            boolean canMakeSet;
            for (Card card : remainingCards) {
                cardRank = card.getRank();
                if (cardRank == 0) {
                    canMakeRun = ((deck & (1L << card.getId() + 1)) != 0 || (hand & (1L << card.getId() + 1)) != 0) &&
                            ((deck & (1L << card.getId() + 2)) != 0 || (hand & (1L << card.getId() + 1)) != 0);
                }
                else if (cardRank== 12) {
                    canMakeRun = ((deck & (1L << card.getId() - 1)) != 0 || (hand & (1L << card.getId() - 1)) != 0) &&
                            ((deck & (1L << card.getId() - 2)) != 0 || (hand & (1L << card.getId() - 2)) != 0);
                }
                else {
                    canMakeRun = ((deck & (1L << card.getId() + 1)) != 0 || (hand & (1L << card.getId() + 1)) != 0) &&
                            ((deck & (1L << card.getId() - 1)) != 0 || (hand & (1L << card.getId() - 1)) != 0);

                    if ((cardRank != 11 && cardRank != 1) && !canMakeRun) {
                        canMakeRun = (((deck & (1L << card.getId() - 1)) != 0 || (hand & (1L << card.getId() - 1)) != 0) &&
                                ((deck & (1L << card.getId() - 2)) != 0 || (hand & (1L << card.getId() - 2)) != 0)) ||
                                (((deck & (1L << card.getId() + 1)) != 0 || (hand & (1L << card.getId() + 1)) != 0) &&
                                ((deck & (1L << card.getId() + 2)) != 0 || (hand & (1L << card.getId() + 2)) != 0));


                    }
                }

                int matchingRankCount = 0;
                int cardSuit = card.getSuit();
                for (int i = 0; i < 4; i++) {
                    if (i != cardSuit) {
                        if ((hand & (1L << (cardRank * i))) != 0 || (deck & (1L << (cardRank * i))) != 0) {
                            matchingRankCount++;
                        }
                    }
                }

                canMakeSet = (matchingRankCount >= 2);

                if (canMakeRun || canMakeSet) {
                    tempHand ^= (1L << card.getId());
                }
            }

            return tempHand;
        }

        public static double averageDeadwoodAfterDraw(long hand, long deck) {
            double runningTot = 0;
            ArrayList<Card> alDeck = GinRummyUtil.bitstringToCards(deck);
            ArrayList<Card> alHand = GinRummyUtil.bitstringToCards(hand);
            int currentDeadwood = Integer.MAX_VALUE;

            for (ArrayList<ArrayList<Card>> melds : GinRummyUtil.cardsToBestMeldSets(alHand)) {
                currentDeadwood = Math.min(GinRummyUtil.getDeadwoodPoints(melds, alHand), currentDeadwood);
            }

            for (Card card : alDeck) {
                hand |= (1L << card.getId());
                if (!GinRummyUtil.cardsToBestMeldSets(GinRummyUtil.bitstringToCards(hand)).isEmpty()) {
                    runningTot += (GinRummyUtil.getDeadwoodPoints(GinRummyUtil.cardsToBestMeldSets(GinRummyUtil.bitstringToCards(hand)).get(0),
                            GinRummyUtil.bitstringToCards(hand)));
                }
                hand ^= (1L << card.getId());
            }

            return  currentDeadwood - (runningTot / keringhan(deck));
        }
    }
}
class JacobCardCounterV3 {
    // in the bit string, if a card is marked w/ a 1, it has been seen, otherwise it may be in deck
    private long deck;
    private long opponentHand;
    private long opponentDiscarded;
    private int unknownDrawCount;

    // This shouldn't be called but its here
    public JacobCardCounterV3() {
        this.deck = 0;
        this.opponentHand = 0;
        this.unknownDrawCount = 10;
    }

    public JacobCardCounterV3(long hand) {
        this.deck |= hand;
        this.opponentHand = 0;
        this.unknownDrawCount = 10;
    }

    public void incrementUnknownDraw() {
        this.unknownDrawCount++;
    }

    public void markCard(int id) {
        this.deck |= 1L << id;
    }

    public void markOpponentHand(int id) {
        this.opponentHand |= (1L << id);
    }
    public void unmarkOpponentHand(int id) {
        if ((this.opponentHand & (1L << id)) != 0) {
            this.opponentHand ^= (1L << id);
        }
    }

    public void markOpponentDiscard(int id) {
        this.opponentDiscarded |= (1L << id);

        if ((this.opponentHand & (1L << id)) == 1) {
            this.opponentHand ^= (1L << id);
        }
    }

    // Returns deck where 1 means seen, 0 means unseen
    public long getDeck() {
        return this.deck;
    }

    // Returns deck where 1 means unseen, 0 means seen
    public long getInvertedDeck() {
        long dummyDeck = this.deck;

        for (int i = 0; i < 52; i++) {
            dummyDeck ^= (1L << i);
        }

        return dummyDeck;
    }

    public long getOpponentHand() {
        return this.opponentHand;
    }

    public long getOpponentDiscarded() {
        return this.opponentDiscarded;
    }

    public boolean deckContainsCard(int id) {
        return (this.deck & (1L << id)) == 0;
    }

    public int numberCardsInDeck() {
        long invertedDeck = this.getInvertedDeck();
        int count = 0;

        while (invertedDeck != 0) {
            invertedDeck &= (invertedDeck - 1);
            count++;
        }

        return count;
    }

    public int numberOfCardsUntilGameEnd() {
        return this.numberCardsInDeck() - 2;
    }
}
