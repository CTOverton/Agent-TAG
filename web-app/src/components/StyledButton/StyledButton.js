import React from "react";
import Button from "@material-ui/core/Button";
import {makeStyles} from "@material-ui/core/styles";

const useStyles = makeStyles(theme => ({
    root: {
        color: '#ffffff',
        borderWidth: 4,
        borderStyle: 'solid',
        borderColor: '#FFFFFF',
        borderRadius: 10,

        textAlign: 'center',
        fontSize: 24,
        fontFamily: 'Proxima Nova Light',
        letterSpacing: 4.8,
    },
}))

const StyledButton = ({children, ...other}) => {
    const style = useStyles();

    return <Button className={style.root} {...other}>{children}</Button>
}

export default StyledButton