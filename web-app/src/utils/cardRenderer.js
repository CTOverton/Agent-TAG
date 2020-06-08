import Club from "../assets/suits/Club.svg";
import Heart from "../assets/suits/Heart.svg";
import Spade from "../assets/suits/Spade.svg";
import Diamond from "../assets/suits/Diamond.svg";

export function suitToRender(suit) {
    switch (suit) {
        case 0:
            return Club;
        case 1:
            return Heart;
        case 2:
            return Spade;
        case 3:
            return Diamond;
    }
}

export function rankToRender(rank) {
    switch (rank) {
        case 0:
            return 'A';
        case 1:
            return '2';
        case 2:
            return '3';
        case 3:
            return '4';
        case 4:
            return '5';
        case 5:
            return '6';
        case 6:
            return '7';
        case 7:
            return '8';
        case 8:
            return '9';
        case 9:
            return '10';
        case 10:
            return 'J';
        case 11:
            return 'Q';
        case 12:
            return 'K';
    }
}