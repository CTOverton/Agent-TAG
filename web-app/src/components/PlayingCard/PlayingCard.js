import React from 'react';
import {makeStyles} from "@material-ui/core/styles";
import {rankToRender, suitToRender} from "../../utils/cardRenderer";


const useStyles = makeStyles(theme => ({
    root: {
        margin: 10,
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        width: 120,
        height: 168,
        borderRadius: 20,
        backgroundColor: '#fff'
    },
    inner: {
        display: 'flex',
        justifyContent: 'center',
        alignItems: 'center',
        flex: 1,
        margin: 10,
        borderWidth: 4,
        borderStyle: 'solid',
        borderColor: '#FFFFFF',
        borderRadius: 20,
    },
    faceDown: {
        margin: 10,
        display: 'flex',
        width: 120,
        height: 168,
        borderRadius: 20,
        backgroundColor: "#885F78",
    },
    content: {
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        width: 64,
        height: 64,
        flexGrow: 1,
        color: '#12171C',
    },
    rank: {
        textAlign: 'center',
        fontWeight: 600,
        fontSize: 48,
        fontFamily: 'Proxima Nova Medium',
        letterSpacing: 0,
        color: '#12171C',
        textTransform: 'uppercase',
    },
    suit: {
        width: 64,
        height: 64,
    }
}))

const PlayingCard = ({isFaceDown, title, rank, suit}) => {
    const style = useStyles();

    if (isFaceDown) return (
        <div className={style.faceDown}>
            <div className={style.inner}>
                {title}
            </div>
        </div>
    )

    return (
        <div className={style.root}>
            <div className={style.content}>
                <div className={style.rank}>{rankToRender(rank)}</div>
            </div>
            <div className={style.content}>
                <img src={suitToRender(suit)} className={style.suit}/>
            </div>
        </div>
    )
}

export default PlayingCard