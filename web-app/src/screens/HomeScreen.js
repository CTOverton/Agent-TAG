import React from "react";
import StyledButton from "../components/StyledButton/StyledButton";
import {makeStyles} from "@material-ui/core/styles";

const useStyles = makeStyles(theme => ({
    main: {
        height: '100vh',
        display: 'flex',
        flexDirection: 'column',
        justifyContent: 'center',
        alignItems: 'center',

        '& > *': {
            width: 220,
            margin: theme.spacing(1),
        },
    },
}))

const HomeScreen = ({history}) => {
    const classes = useStyles();

    return (
        <div className={classes.main}>
            <StyledButton onClick={() => history.push('/game')}>Login</StyledButton>
            <StyledButton>Sign up</StyledButton>
        </div>
    )
}

export default HomeScreen